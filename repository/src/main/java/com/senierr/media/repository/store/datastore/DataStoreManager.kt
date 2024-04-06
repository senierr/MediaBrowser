package com.senierr.media.repository.store.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.senierr.media.repository.store.datastore.DataStoreManager.DS_NAME

/**
 * SharedPreferences管理器
 *
 * @author zhouchunjie
 * @date 2019/11/28
 */
object DataStoreManager {

    const val DS_NAME = "repository_ds"
}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DS_NAME)