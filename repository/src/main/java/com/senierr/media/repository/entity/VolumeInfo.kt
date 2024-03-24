package com.senierr.media.repository.entity

/**
 * 挂载盘信息
 *
 * @author senierr_zhou
 * @date 2023/07/30
 */
data class VolumeInfo(
    val uuid: String?,
    val path: String?,
    val description: String?,
    val mState: String,
    val isRemovable: Boolean
)
