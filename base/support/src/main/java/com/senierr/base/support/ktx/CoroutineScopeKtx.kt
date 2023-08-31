package com.senierr.base.support.ktx

import kotlinx.coroutines.CancellationException

/**
 *
 * @author chunjiezhou
 * @date 2022/04/15
 */

inline fun runCatchSilent(
    success: () -> Unit,
    error: (e: Exception) -> Unit,
) {
    try {
        success.invoke()
    } catch (e: Exception) {
        if (e is CancellationException) {
            // ignore
        } else {
            error.invoke(e)
        }
    }
}