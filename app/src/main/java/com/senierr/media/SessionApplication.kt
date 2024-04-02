package com.senierr.media

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.imageLoader
import coil.memory.MemoryCache
import com.senierr.base.util.LogUtil
import com.senierr.media.domain.audio.service.AudioMediaBrowserService
import com.senierr.media.repository.MediaRepository
import com.senierr.media.utils.LocalAudioFetcher

/**
 *
 * @author senierr_zhou
 * @date 2023/07/28
 */
class SessionApplication : Application(), ImageLoaderFactory, ViewModelStoreOwner {

    companion object {
        private const val TAG = "SessionApplication"

        private lateinit var application: SessionApplication

        fun getInstance(): SessionApplication = application
    }

    private val isDebug = true
    private val _viewModelStore = ViewModelStore()

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")
        application = this

        LogUtil.isDebug = isDebug
        MediaRepository.initialize(application)

        startForegroundService(Intent(this, AudioMediaBrowserService::class.java))
    }

    override val viewModelStore: ViewModelStore
        get() = _viewModelStore

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        imageLoader.memoryCache?.trimMemory(level)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        imageLoader.memoryCache?.clear()
    }

    // Coil图片加载框架配置项
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizeBytes(10 * 1024 * 1024)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024)
                    .build()
            }
            .allowRgb565(true)
            .crossfade(true)
            .components {
                // 视频解码器
                add(VideoFrameDecoder.Factory())
                // GIF解码器
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(LocalAudioFetcher.Factory())
            }
            .build()
    }
}