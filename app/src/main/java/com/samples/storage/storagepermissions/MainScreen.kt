package com.samples.storage.storagepermissions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.samples.storage.storagepermissions.MainViewModel.Companion.ANDROID_VERSION
import com.samples.storage.storagepermissions.MainViewModel.Companion.API_VERSION

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val state = viewModel.uiState
    val hasReadExternalStorageBeenGranted: Boolean? by viewModel.hasReadExternalStorageBeenGranted.collectAsState(initial = false)

    val requestPermissions = rememberLauncherForActivityResult(
        RequestMultiplePermissions(),
        viewModel::onPermissionRequest
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Permissions", fontFamily = FontFamily.Serif) },
            )
        },
        floatingActionButton = {
            if (state.hasStorageAccess) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::refreshLibrary,
                    icon = {
                        if (state.queryStatus == MainViewModel.QueryStatus.LOADING) {
                            CircularProgressIndicator()
                        } else {
                            Icon(Icons.Filled.Refresh, null)
                        }
                    },
                    text = { Text(text = "Refresh Library") },
                )
            } else {
                ExtendedFloatingActionButton(
                    onClick = { requestPermissions.launch(state.permissions) },
                    icon = { Icon(Icons.Filled.Lock, null) },
                    text = { Text(text = "Request permissions") },
                )
            }
        }
    ) { paddingValues ->

        Column(Modifier.padding(paddingValues)) {
            ListItem(
                headlineText = { Text("Android Version") },
                leadingContent = { Icon(Icons.Filled.Android, contentDescription = null) },
                trailingContent = { Text(ANDROID_VERSION) }
            )
            Divider()
            ListItem(
                headlineText = { Text("Device SDK API Version") },
                leadingContent = { Icon(Icons.Filled.Settings, contentDescription = null) },
                trailingContent = { Text(API_VERSION.toString()) }
            )
            Divider()
            ListItem(
                headlineText = { Text("Target SDK API Version") },
                leadingContent = { Icon(Icons.Filled.Settings, contentDescription = null) },
                trailingContent = { Text(state.targetSdk.toString()) }
            )
            Divider()
            ListItem(
                headlineText = { Text("Has READ_EXTERNAL_STORAGE been granted before?") },
                leadingContent = { Icon(Icons.Filled.History, contentDescription = null) },
                trailingContent = { Text(hasReadExternalStorageBeenGranted?.toString() ?: "No record") }
            )
            Divider()
            ListItem(
                headlineText = { Text("Required Storage Permissions") },
                leadingContent = { Icon(Icons.Filled.Lock, contentDescription = null) },
                trailingContent = { Text(if (state.hasStorageAccess) "Granted" else "Not granted") }
            )
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = state.permissions.joinToString("\n"),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Divider()
            MediaGallery(state.items)
        }
    }
}

@Composable
fun MediaGallery(items: List<MainViewModel.MediaFile>) {
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        items(items) {
            AsyncImage(
                model = it.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.aspectRatio(1f)
            )
        }
    }
}