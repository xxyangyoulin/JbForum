package com.xxyangyoulin.jbforum

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object MessageStatusStore {
    private val _status = MutableStateFlow(ForumMessageStatus())
    val status: StateFlow<ForumMessageStatus> = _status

    fun init() {
        _status.value = MessageStatusPersistence.load()
    }

    fun value(): ForumMessageStatus = _status.value

    fun update(status: ForumMessageStatus) {
        _status.value = status
        MessageStatusPersistence.save(status)
    }

    fun clear() {
        _status.value = ForumMessageStatus()
        MessageStatusPersistence.clear()
    }
}
