package com.xxyangyoulin.jbforum

import android.content.Context

object MessageStatusPersistence {
    private const val keyLoggedIn = "logged_in"
    private const val keyUnreadCount = "unread_count"
    private const val keyHasUnread = "has_unread"
    private const val keyAvatarUrl = "avatar_url"
    private const val keyNoticeUrl = "notice_url"
    private const val keyMessageUrl = "message_url"

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun load(): ForumMessageStatus {
        val context = appContext ?: return ForumMessageStatus()
        val prefs = context.getSharedPreferences(AppConstants.PREFS_MESSAGE_STATUS, Context.MODE_PRIVATE)
        return ForumMessageStatus(
            loggedIn = prefs.getBoolean(keyLoggedIn, false),
            unreadCount = prefs.getInt(keyUnreadCount, 0),
            hasUnread = prefs.getBoolean(keyHasUnread, false),
            avatarUrl = prefs.getString(keyAvatarUrl, null),
            noticeUrl = prefs.getString(keyNoticeUrl, "").orEmpty(),
            messageUrl = prefs.getString(keyMessageUrl, "").orEmpty()
        )
    }

    fun save(status: ForumMessageStatus) {
        val context = appContext ?: return
        context.getSharedPreferences(AppConstants.PREFS_MESSAGE_STATUS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(keyLoggedIn, status.loggedIn)
            .putInt(keyUnreadCount, status.unreadCount)
            .putBoolean(keyHasUnread, status.hasUnread)
            .putString(keyAvatarUrl, status.avatarUrl)
            .putString(keyNoticeUrl, status.noticeUrl)
            .putString(keyMessageUrl, status.messageUrl)
            .apply()
    }

    fun clear() {
        val context = appContext ?: return
        context.getSharedPreferences(AppConstants.PREFS_MESSAGE_STATUS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
