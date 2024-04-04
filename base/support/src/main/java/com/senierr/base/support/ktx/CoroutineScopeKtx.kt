package com.senierr.base.support.ktx

import kotlinx.coroutines.CancellationException

/**
 *
 * @author chunjiezhou
 * @date 2022/04/15
 */

inline fun <T> runCatchSilent(
    success: () -> T?,
    error: (e: Exception) -> T?,
): T? {
    return try {
        success.invoke()
    } catch (e: Exception) {
        if (e is CancellationException) {
            // ignore
            null
        } else {
            return error.invoke(e)
        }
    }
}