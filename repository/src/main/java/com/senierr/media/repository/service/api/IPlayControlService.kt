package com.senierr.media.local.repository.service.api

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
    suspend fun savePlaySession()

    /**
     * 获取播放会话
     */
    suspend fun fetchPlaySession()
}