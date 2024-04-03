package com.senierr.media.domain.audio.widget.viewmodel

import android.content.ComponentName

/**
 *
 * @author senierr_zhou
 * @date 2024/04/03
 */
class MiniBarControlViewModel : BaseControlViewModel() {

    override fun getComponentName(): ComponentName {
        return ComponentName("com.senierr.media.Browser", "com.senierr.media.domain.audio.service.AudioMediaBrowserService")
    }
}