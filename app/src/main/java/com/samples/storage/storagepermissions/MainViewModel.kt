package com.samples.storage.storagepermissions

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns.DATA
import android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME
import android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE
import android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
import android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
import android.provider.MediaStore.Files.FileColumns.MIME_TYPE
import android.provider.MediaStore.Files.FileColumns.SIZE
import android.provider.MediaStore.Files.FileColumns._ID
import android.provider.MediaStore.MediaColumns.DATE_ADDED
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samples.storage.storagepermissions.Settings.HAS_READ_EXTERNAL_STORAGE_BEEN_GRANTED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        val ANDROID_VERSION = Build.VERSION.RELEASE
        val API_VERSION = Build.VERSION.SDK_INT
    }

    private val context: Context
        get() = getApplication()

    enum class QueryStatus {
        NOT_STARTED, LOADING, DONE
    }

    data class MediaFile(
        val uri: Uri,
        val type: Type,
        val filename: String,
        val size: Long,
        val mimeType: String,
        val path: String,
    ) {
        enum class Type {
            Image, Video
        }
    }

    val hasReadExternalStorageBeenGranted: Flow<Boolean?> = context.dataStore.data.map { settings ->
        settings[HAS_READ_EXTERNAL_STORAGE_BEEN_GRANTED]
    }

    data class UiState(
        val targetSdk: Int,
        val permissions: Array<String>,
        val hasStorageAccess: Boolean,
        val queryStatus: QueryStatus,
        val items: List<MediaFile>
    )

    var uiState by mutableStateOf(init())
        private set

    private fun init(): UiState {
        return UiState(
            targetSdk = context.applicationInfo.targetSdkVersion,
            permissions = getStoragePermissions(),
            hasStorageAccess = hasStorageAccess(),
            queryStatus = QueryStatus.NOT_STARTED,
            items = emptyList()
        )
    }

    private fun getStoragePermissions(): Array<String> {
        val permissions = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.TIRAMISU
        ) {
            arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(READ_EXTERNAL_STORAGE)
        } else {
            arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)
        }

        return permissions
    }

    private fun hasStorageAccess(): Boolean {
        return getStoragePermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun onPermissionRequest(grants: Map<String, Boolean>) {
        viewModelScope.launch {
            // Store READ_EXTERNAL_STORAGE has even been granted
            if (grants.containsKey(READ_EXTERNAL_STORAGE)) {
                context.dataStore.edit { settings ->
                    settings[HAS_READ_EXTERNAL_STORAGE_BEEN_GRANTED] =
                        grants[READ_EXTERNAL_STORAGE]!!
                }
            }

            uiState = uiState.copy(hasStorageAccess = hasStorageAccess())
        }
    }

    fun refreshLibrary() {
        uiState = uiState.copy(queryStatus = QueryStatus.LOADING, items = emptyList())

        viewModelScope.launch {
            uiState = try {
                uiState.copy(queryStatus = QueryStatus.DONE, items = queryLibrary())
            } catch (error: Exception) {
                Log.e("refreshLibrary", error.toString())
                uiState.copy(queryStatus = QueryStatus.DONE, items = emptyList())
            }
        }
    }

    private suspend fun queryLibrary(): List<MediaFile> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaFile>()
        val externalContentUri = MediaStore.Files.getContentUri("external")
            ?: throw Exception("External Storage not available")

        val projection = arrayOf(
            _ID,
            DISPLAY_NAME,
            SIZE,
            MEDIA_TYPE,
            MIME_TYPE,
            DATA,
        )

        val cursor = context.contentResolver.query(
            externalContentUri,
            projection,
            "$MEDIA_TYPE = ? OR $MEDIA_TYPE = ?",
            arrayOf(
                MEDIA_TYPE_IMAGE.toString(),
                MEDIA_TYPE_VIDEO.toString()
            ),
            "$DATE_ADDED DESC" // Hard coded limit of 500 items
        ) ?: throw Exception("Query could not be executed")

        cursor.use {
            val idColumn = cursor.getColumnIndexOrThrow(_ID)
            val displayNameColumn = cursor.getColumnIndexOrThrow(DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(SIZE)
            val mediaTypeColumn = cursor.getColumnIndexOrThrow(MEDIA_TYPE)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MIME_TYPE)
            val dataColumn = cursor.getColumnIndexOrThrow(DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getInt(idColumn)
                val contentUri = ContentUris.withAppendedId(externalContentUri, id.toLong())
                val type = if (cursor.getInt(mediaTypeColumn) == MEDIA_TYPE_IMAGE) {
                    MediaFile.Type.Image
                } else {
                    MediaFile.Type.Video
                }

                items += MediaFile(
                    uri = contentUri,
                    filename = cursor.getString(displayNameColumn),
                    size = cursor.getLong(sizeColumn),
                    type = type,
                    mimeType = cursor.getString(mimeTypeColumn),
                    path = cursor.getString(dataColumn),
                )
            }
        }

        return@withContext items
    }
}