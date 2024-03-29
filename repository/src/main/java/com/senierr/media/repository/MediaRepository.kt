package com.senierr.media.repository

import android.app.Application
import com.senierr.media.repository.service.api.IMediaService
import com.senierr.media.repository.store.db.DatabaseManager
import com.senierr.media.repository.service.impl.MediaService

/**
 * U盘视频数据入口
 *
 * @author chunjiezhou
 */
object MediaRepository {

    private lateinit var application: Application

    /**
     * 初始化
     *
     * @param application 应用上下文
     * @param ignoreAudio 是否忽略音频
     * @param ignoreImage 是否忽略图片
     * @param ignoreVideo 是否忽略视频
     */
    fun initialize(application: Application) {
        this.application = application
        DatabaseManager.initialize(application)
    }

    /**
     * 获取应用实例
     */
    fun getApplication(): Application = application

    /**
     * 获取数据服务
     */
    inline fun <reified T> getService(): T = when (T::class.java) {
        IMediaService::class.java -> MediaService(getApplication()) as T
        else -> throw IllegalArgumentException("Can not find ${T::class.java.simpleName}!")
    }
}