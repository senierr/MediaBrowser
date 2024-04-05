package com.senierr.media.repository.service.api

import com.senierr.media.repository.entity.PlaySession

/**
 * 播控服务
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
interface IPlayControlService {

    /**
     * 保存播放会话
     */
    suspend fun savePlaySession(playSession: PlaySession, mediaType: Int)

    /**
     * 获取播放会话
     */
    suspend fun fetchPlaySession(mediaType: Int): PlaySession?
}