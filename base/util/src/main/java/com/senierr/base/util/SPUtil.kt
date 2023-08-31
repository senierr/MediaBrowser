package com.senierr.base.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Base64

/**
 * SharedPreferences工具类
 *
 * @author zhouchunjie
 * @date 2019/6/19 14:13
 */
@SuppressLint("ApplySharedPref")
class SPUtil private constructor(private val sp: SharedPreferences) {

    companion object {

        private val SP_UTILS_MAP = HashMap<String, SPUtil>()

        fun getInstance(context: Context, spName: String = "SPUtil"): SPUtil {
            val spUtil = SP_UTILS_MAP[spName] ?: SPUtil(context.getSharedPreferences(spName, Context.MODE_PRIVATE))
            SP_UTILS_MAP[spName] = spUtil
            return spUtil
        }
    }

    fun putString(key: String, value: String, isCommit: Boolean = false) {
        val enKey = Base64.encodeToString(key.toByteArray(), Base64.NO_WRAP)
        val enValue = Base64.encodeToString(value.toByteArray(), Base64.NO_WRAP)
        if (isCommit) {
            sp.edit().putString(enKey, enValue).commit()
        } else {
            sp.edit().putString(enKey, enValue).apply()
        }
    }

    fun getString(key: String, defaultValue: String = ""): String {
        val enKey = Base64.encodeToString(key.toByteArray(), Base64.NO_WRAP)
        var value = sp.getString(enKey, defaultValue)
        if (value != null) {
            value = String(Base64.decode(value, Base64.NO_WRAP))
        }
        return value ?: defaultValue
    }

    fun putInt(key: String, value: Int, isCommit: Boolean = false) {
        val enKey = Base64.encodeToString(key.toByteArray(), Base64.NO_WRAP)
        if (isCommit) {
            sp.edit().putInt(enKey, value).commit()
        } else {
            sp.edit().putInt(enKey, value).apply()
        }
    }

    fun getInt(key: String, defaultValue: Int = -1): Int {
        val enKey = Base64.encodeToString(key.toByteArray(), Base64.NO_WRAP)
        return sp.getInt(enKey, defaultValue)
    }

    fun putLong(key: String, value: Long, isCommit: Boolean = false) {
        if (isCommit) {
            sp.edit().putLong(key, value).commit()
        } else {
            sp.edit().putLong(key, value).apply()
        }
    }

    fun getLong(key: String, defaultValue: Long = -1L): Long {
        return sp.getLong(key, defaultValue)
    }

    fun putFloat(key: String, value: Float, isCommit: Boolean = false) {
        if (isCommit) {
            sp.edit().putFloat(key, value).commit()
        } else {
            sp.edit().putFloat(key, value).apply()
        }
    }

    fun getFloat(key: String, defaultValue: Float = -1F): Float {
        return sp.getFloat(key, defaultValue)
    }

    fun putBoolean(key: String, value: Boolean, isCommit: Boolean = false) {
        if (isCommit) {
            sp.edit().putBoolean(key, value).commit()
        } else {
            sp.edit().putBoolean(key, value).apply()
        }
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sp.getBoolean(key, defaultValue)
    }

    fun putStringSet(key: String, value: MutableSet<String>, isCommit: Boolean = false) {
        if (isCommit) {
            sp.edit().putStringSet(key, value).commit()
        } else {
            sp.edit().putStringSet(key, value).apply()
        }
    }

    fun getStringSet(key: String, defaultValue: MutableSet<String> = mutableSetOf()): MutableSet<String> {
        return sp.getStringSet(key, defaultValue) ?: mutableSetOf()
    }

    /**
     * 获取所有键值对
     */
    fun getAll(): Map<String, *> {
        return sp.all
    }

    /**
     * 是否含某键值对
     */
    fun contains(key: String): Boolean {
        return sp.contains(key)
    }

    /**
     * 移除某键值对
     */
    fun remove(key: String, isCommit: Boolean = false) {
        if (isCommit) {
            sp.edit().remove(key).commit()
        } else {
            sp.edit().remove(key).apply()
        }
    }

    /**
     * 清除所有数据
     */
    fun clear(isCommit: Boolean = false) {
        if (isCommit) {
            sp.edit().clear().commit()
        } else {
            sp.edit().clear().apply()
        }
    }
}