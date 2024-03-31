package com.senierr.media.utils

import java.text.Collator
import java.util.Locale

/**
 * 排序工具类
 *
 * @author senierr_zhou
 * @date 2023/07/30
 */
object SortUtil {

    /**
     * 排序， 符号 > 数字 > 英文 > 中文
     */
    fun <T> List<T>.sort(getNameAction: (t: T) -> String): List<T> {
        // 将list集合分成只包含以汉字开头元素的集合和不包含以汉字开头元素的集合
        val chinese: MutableList<T> = ArrayList()
        val notChinese: MutableList<T> = ArrayList()
        for (item in this) {
            val title = getNameAction.invoke(item).trim().lowercase(Locale.ROOT)
            if (title.firstOrNull().toString().matches(Regex("[\u4e00-\u9fa5]"))) {
                // 如果开头为汉字，则加入汉字列表中
                chinese.add(item)
            } else {
                notChinese.add(item)
            }
        }
        val chinaCollator: Comparator<Any> = Collator.getInstance(Locale.CHINA)
        // 对两组数据进行排序
        notChinese.sortWith { o1: T, o2: T ->
            val title1 = getNameAction.invoke(o1).trim().lowercase(Locale.ROOT)
            val title2 = getNameAction.invoke(o2).trim().lowercase(Locale.ROOT)
            chinaCollator.compare(title1, title2)
        }
        chinese.sortWith { o1: T, o2: T ->
            val title1 = getNameAction.invoke(o1).trim().lowercase(Locale.ROOT)
            val title2 = getNameAction.invoke(o2).trim().lowercase(Locale.ROOT)
            chinaCollator.compare(title1, title2)
        }
        //合并数据
        notChinese.addAll(chinese)
        return notChinese
    }
}