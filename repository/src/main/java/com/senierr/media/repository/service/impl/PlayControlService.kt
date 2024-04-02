package com.senierr.media.repository.service.impl

import com.senierr.media.repository.entity.PlaySession
import com.senierr.media.repository.service.api.IPlayControlService

/**
 * 播控服务
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
class PlayControlService : IPlayControlService {

    override suspend fun savePlaySession(playSession: PlaySession) {

    }

    override suspend fun fetchPlaySession(): PlaySession? {
        return null
    }
}