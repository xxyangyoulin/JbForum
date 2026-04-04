package com.xxyangyoulin.jbforum

object LinkCategory {
    const val ALL = "全部"
    const val MAGNET = "磁力"
    const val CLOUD = "网盘"
    const val CODE = "番号"
    const val TEXT = "文本"
    const val OTHER = "其他"

    val tabs = listOf(ALL, MAGNET, CLOUD, CODE, TEXT, OTHER)

    fun matches(tab: String, type: String): Boolean {
        return when (tab) {
            ALL -> true
            MAGNET -> type == MAGNET || type == "种子ID"
            CLOUD -> type == CLOUD
            CODE -> type == CODE
            TEXT -> type == TEXT
            else -> type != MAGNET && type != CLOUD && type != "种子ID" && type != CODE && type != TEXT
        }
    }
}
