package com.senierr.plugin.autodimens

/**
 * 屏幕尺寸
 */
class ScreenSize {

    String name
    int width       // 宽
    int height      // 高
    double scale    // 缩放比

    ScreenSize(String name) {
        this.name = name
    }
}