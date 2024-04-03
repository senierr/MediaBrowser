package com.senierr.media.repository.service.impl

import com.senierr.media.repository.entity.PlaySession
import com.senierr.media.repository.service.api.IPlayControlService
import com.senierr.media.repository.store.db.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 播控服务
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
class PlayControlService : IPlayControlService {

    private val playSessionDao by lazy { DatabaseManager.getDatabase().getPlaySessionDao() }

    override suspend fun savePlaySession(playSession: PlaySession) {
        return withContext(Dispatchers.IO) {
            return@withContext playSessionDao.insertOrReplace(playSession)
        }
    }

    override suspend fun fetchPlaySession(): PlaySession? {
        return withContext(Dispatchers.IO) {
            return@withContext playSessionDao.getAll().firstOrNull()
        }
    }
}