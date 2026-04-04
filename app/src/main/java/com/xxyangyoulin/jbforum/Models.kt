package com.xxyangyoulin.jbforum

data class Board(
    val title: String,
    val description: String,
    val url: String,
    val latestThreadTitle: String = "",
    val latestThreadUrl: String = ""
)

data class ThreadSummary(
    val id: String,
    val title: String,
    val author: String,
    val authorUid: String = "",
    val authorAvatarUrl: String? = null,
    val publishedAt: String = "",
    val titleIconUrls: List<String> = emptyList(),
    val totalPages: Int? = null,
    val url: String,
    val thumbnailUrls: List<String> = emptyList(),
    val viewsText: String = "",
    val repliesText: String = "",
    val lastReplyAuthor: String = "",
    val lastReplyTime: String = "",
    val metaText: String = ""
)

data class ThreadListPage(
    val threads: List<ThreadSummary>,
    val nextPageUrl: String?
)

data class PostItem(
    val pid: String,
    val author: String,
    val authorUid: String = "",
    val authorAvatarUrl: String? = null,
    val time: String,
    val floor: String,
    val content: String,
    val contentBlocks: List<PostContentBlock> = emptyList(),
    val imageUrls: List<String>,
    val remarks: List<PostRemark> = emptyList()
)

data class PostRemark(
    val author: String,
    val authorUid: String = "",
    val authorAvatarUrl: String? = null,
    val time: String,
    val content: String
)

data class PostContentBlock(
    val text: String? = null,
    val imageUrl: String? = null,
    val imageIndex: Int? = null
)

data class ThreadDetail(
    val title: String,
    val author: String,
    val authorUid: String = "",
    val publishedAt: String,
    val repliesText: String,
    val viewsText: String,
    val posts: List<PostItem>,
    val url: String,
    val nextPageUrl: String?,
    val currentPage: Int,
    val totalPages: Int,
    val favoriteCount: String,
    val favoriteActionUrl: String?,
    val replyAction: String?,
    val replyFields: Map<String, String>
)

data class RemarkForm(
    val pid: String,
    val targetLabel: String,
    val actionUrl: String,
    val hiddenFields: Map<String, String>
)

data class CaptchaChallenge(
    val imageBytes: ByteArray,
    val formhash: String,
    val loginAction: String,
    val loginhash: String,
    val referer: String,
    val seccodeHash: String,
    val hiddenFields: Map<String, String>
)

data class UserSession(
    val username: String,
    val uid: String = ""
)

data class ComposeForm(
    val actionUrl: String,
    val hiddenFields: Map<String, String>,
    val typeOptions: List<Pair<String, String>>
)

data class UserThreadItem(
    val title: String,
    val url: String,
    val board: String = "",
    val time: String = "",
    val summary: String = "",
    val deleteActionUrl: String? = null
)

data class UserThreadListPage(
    val items: List<UserThreadItem>,
    val nextPageUrl: String?
)

data class UserProfile(
    val username: String,
    val uid: String,
    val avatarUrl: String?,
    val stats: List<Pair<String, String>>,
    val basics: List<Pair<String, String>>,
    val activity: List<Pair<String, String>>,
    val credits: List<Pair<String, String>>
)

data class LocalFavoriteImage(
    val id: String,
    val filePath: String,
    val thumbnailPath: String,
    val originalUrl: String,
    val savedAt: Long,
    val sourceThreadTitle: String = "",
    val sourceThreadUrl: String = ""
)

data class DetectedLink(
    val value: String,
    val type: String
)

data class LocalFavoriteLink(
    val id: String,
    val value: String,
    val type: String,
    val savedAt: Long,
    val sourceThreadTitle: String = "",
    val sourceThreadUrl: String = ""
)
