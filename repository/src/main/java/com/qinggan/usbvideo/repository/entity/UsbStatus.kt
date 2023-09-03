package com.qinggan.usbvideo.repository.entity

/**
 * U盘状态
 *
 * @author chunjiezhou
 * @date 2022/04/14
 */
data class UsbStatus(
    val action: String,
    val path: String
) {
    companion object {
        const val ACTION_EJECT = "ACTION_EJECT"                         // 未挂载
        const val ACTION_MOUNTED = "ACTION_MOUNTED"                     // 已挂载
        const val ACTION_SCANNER_STARTED = "ACTION_SCANNER_STARTED"     // 开始扫描
        const val ACTION_SCANNER_FINISHED = "ACTION_SCANNER_FINISHED"   // 扫描结束
    }
}