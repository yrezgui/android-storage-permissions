package com.samples.storage.storagepermissions

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object Settings {
    val HAS_READ_EXTERNAL_STORAGE_BEEN_GRANTED = booleanPreferencesKey("has_read_external_storage_been_granted")
}


