package com.xxyangyoulin.jbforum

data class ForumMessageStatus(
    val loggedIn: Boolean = false,
    val unreadCount: Int = 0,
    val hasUnread: Boolean = false,
    val noticeUrl: String = "",
    val messageUrl: String = ""
)

data class ForumNoticeItem(
    val id: String,
    val author: String,
    val authorUid: String,
    val authorAvatarUrl: String?,
    val time: String,
    val content: String,
    val threadTitle: String,
    val targetUrl: String
)
