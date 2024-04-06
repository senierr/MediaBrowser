package com.senierr.media.repository.store.datastore

import androidx.datastore.preferences.core.intPreferencesKey

/**
 * DataStore Key值管理
 *
 * @author zhouchunjie
 * @date 2019/11/28
 */
object DataStoreKey {
    val EXAMPLE_COUNTER = intPreferencesKey("example_counter")
}