package com.senierr.media.utils

import android.media.MediaMetadataRetriever
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import com.senierr.media.repository.entity.LocalAudio
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream

/**
 *
 * @author senierr
 * @date 2024/3/31
 */
class LocalAudioFetcher(
    private val data: LocalAudio,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val retriever = MediaMetadataRetriever()
        val inputStream = try {
            retriever.setDataSource(data.path)
            val artwork = retriever.embeddedPicture
            ByteArrayInputStream(artwork)
        } finally {
            retriever.release()
        }
        return SourceResult(
            source = ImageSource(
                source = inputStream.source().buffer(),
                context = options.context,
            ),
            mimeType = data.mimeType,
            dataSource = DataSource.DISK
        )
    }

    class Factory : Fetcher.Factory<LocalAudio> {

        override fun create(data: LocalAudio, options: Options, imageLoader: ImageLoader): Fetcher? {
            return LocalAudioFetcher(data, options)
        }
    }
}