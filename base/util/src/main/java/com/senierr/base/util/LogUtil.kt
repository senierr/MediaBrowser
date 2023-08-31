package com.senierr.base.util

import android.util.Log

/**
 * 日志工具类
 *
 * @author zhouchunjie
 * @date 2017/10/26
 */
object LogUtil {

    var isDebug = false
    var defaultTag: String = LogUtil::class.java.simpleName

    fun logV(msg: String?) {
        logV(defaultTag, msg)
    }

    fun logD(msg: String?) {
        logD(defaultTag, msg)
    }

    fun logI(msg: String?) {
        logI(defaultTag, msg)
    }

    fun logW(msg: String?) {
        logW(defaultTag, msg)
    }

    fun logE(msg: String?) {
        logE(defaultTag, msg)
    }

    fun logV(tag: String, msg: String?) {
        if (isDebug && msg != null) {
            Log.v(tag, msg)
        }
    }

    fun logD(tag: String, msg: String?) {
        if (isDebug && msg != null) {
            Log.d(tag, msg)
        }
    }

    fun logI(tag: String, msg: String?) {
        if (isDebug && msg != null) {
            Log.i(tag, msg)
        }
    }

    fun logW(tag: String, msg: String?) {
        if (isDebug && msg != null) {
            Log.w(tag, msg)
        }
    }

    fun logE(tag: String, msg: String?) {
        if (isDebug && msg != null) {
            Log.e(tag, msg)
        }
    }
}

fun String.logV(tag: String = LogUtil.defaultTag) {
    LogUtil.logV(tag, this)
}

fun String.logD(tag: String = LogUtil.defaultTag) {
    LogUtil.logD(tag, this)
}

fun String.logI(tag: String = LogUtil.defaultTag) {
    LogUtil.logI(tag, this)
}

fun String.logW(tag: String = LogUtil.defaultTag) {
    LogUtil.logW(tag, this)
}

fun String.logE(tag: String = LogUtil.defaultTag) {
    LogUtil.logE(tag, this)
}
