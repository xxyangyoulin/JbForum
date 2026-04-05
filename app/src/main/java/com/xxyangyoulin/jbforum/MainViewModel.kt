package com.xxyangyoulin.jbforum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppState(
    val boards: List<Board> = emptyList(),
    val threads: List<ThreadSummary> = emptyList(),
    val threadsNextPageUrl: String? = null,
    val threadDetail: ThreadDetail? = null,
    val detectedLinks: List<DetectedLink> = emptyList(),
    val userThreads: List<UserThreadItem> = emptyList(),
    val userThreadsNextPageUrl: String? = null,
    val userReplies: List<UserThreadItem> = emptyList(),
    val userRepliesNextPageUrl: String? = null,
    val userFavorites: List<UserThreadItem> = emptyList(),
    val userFavoritesNextPageUrl: String? = null,
    val userProfile: UserProfile? = null,
    val localFavoriteImages: List<LocalFavoriteImage> = emptyList(),
    val userCenterUid: String = "",
    val userCenterVisible: Boolean = false,
    val threadOpenedFromUserCenter: Boolean = false,
    val selectedBoard: Board? = null,
    val selectedThread: ThreadSummary? = null,
    val threadRefreshing: Boolean = false,
    val threadListFirstVisibleItemIndex: Int = 0,
    val threadListFirstVisibleItemScrollOffset: Int = 0,
    val composeForm: ComposeForm? = null,
    val remarkForm: RemarkForm? = null,
    val challenge: CaptchaChallenge? = null,
    val session: UserSession? = null,
    val loading: Boolean = false,
    val message: String? = null
)

class MainViewModel(
    private val repository: ForumRepository = ForumRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(AppStateSnapshot.restore() ?: AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.collect { current ->
                AppStateSnapshot.save(
                    current.copy(
                        loading = false,
                        threadRefreshing = false,
                        message = null,
                        challenge = null,
                        composeForm = null,
                        remarkForm = null
                    )
                )
            }
        }
        launchTask {
            _state.update { it.copy(loading = true, message = null) }
            val boards = repository.loadBoards()
            val session = repository.currentSession()
            _state.update { it.copy(boards = boards, session = session, localFavoriteImages = LocalImageFavorites.load(), loading = false) }
        }
    }

    fun refreshBoards() = launchTask {
        _state.update { it.copy(loading = true, message = null, selectedBoard = null, selectedThread = null, threadDetail = null, detectedLinks = emptyList(), userCenterVisible = false) }
        val boards = repository.loadBoards()
        _state.update { it.copy(boards = boards, loading = false) }
    }

    fun openBoard(board: Board) = launchTask {
        _state.update { it.copy(loading = true, selectedBoard = board, selectedThread = null, threadDetail = null, detectedLinks = emptyList(), message = null) }
        val page = repository.loadThreads(board.url)
        _state.update { it.copy(threads = page.threads, threadsNextPageUrl = page.nextPageUrl, loading = false) }
    }

    fun searchThreads(keyword: String) = launchTask {
        val query = keyword.trim()
        if (query.isBlank()) error("请输入搜索关键词")
        _state.update {
            it.copy(
                loading = true,
                message = null,
                selectedBoard = Board(
                    title = "搜索：$query",
                    description = "搜索结果",
                    url = "search.php?mod=forum&searchsubmit=yes&kw=${URLEncoder.encode(query, Charsets.UTF_8.name())}"
                ),
                selectedThread = null,
                threadDetail = null,
                detectedLinks = emptyList()
            )
        }
        val page = repository.searchThreads(query)
        _state.update { it.copy(threads = page.threads, threadsNextPageUrl = page.nextPageUrl, loading = false) }
    }

    fun openThread(thread: ThreadSummary) = launchTask {
        val openedFromUserCenter = state.value.userCenterVisible
        val isRefresh = state.value.threadDetail != null && state.value.selectedThread?.url == thread.url
        _state.update {
            it.copy(
                loading = !isRefresh,
                threadRefreshing = isRefresh,
                selectedThread = thread,
                userCenterVisible = false,
                threadOpenedFromUserCenter = openedFromUserCenter,
                remarkForm = null,
                message = null
            )
        }
        val detail = repository.loadThread(thread.url)
        _state.update {
            it.copy(
                threadDetail = detail,
                detectedLinks = ThreadLinkRecognizer.extract(detail),
                loading = false,
                threadRefreshing = false
            )
        }
    }

    fun prepareLogin() = launchTask {
        _state.update { it.copy(loading = true, message = null) }
        val challenge = repository.fetchLoginCaptcha()
        _state.update { it.copy(challenge = challenge, loading = false) }
    }

    fun logoutAndPrepareLogin() = launchTask {
        _state.update { it.copy(loading = true, message = null, challenge = null) }
        repository.logout()
        val challenge = repository.fetchLoginCaptcha()
        _state.update {
            it.copy(
                session = null,
                challenge = challenge,
                loading = false,
                message = "已注销"
            )
        }
    }

    fun logout() = launchTask {
        _state.update { it.copy(loading = true, message = null, challenge = null) }
        repository.logout()
        _state.update {
            it.copy(
                session = null,
                loading = false,
                message = "已注销"
            )
        }
    }

    fun login(username: String, password: String, captcha: String) = launchTask {
        val challenge = state.value.challenge ?: error("请先获取验证码")
        _state.update { it.copy(loading = true, message = null) }
        val session = repository.login(username, password, captcha, challenge)
        _state.update { it.copy(session = session, challenge = null, loading = false, message = "已登录为 ${session.username}") }
        val current = state.value
        when {
            current.selectedThread != null -> openThread(current.selectedThread)
            current.userCenterVisible -> openUserCenter(current.userCenterUid.takeIf { it.isNotBlank() })
            current.selectedBoard != null -> openBoard(current.selectedBoard)
            else -> refreshBoards()
        }
    }

    fun prepareNewThread() = launchTask {
        val board = state.value.selectedBoard ?: error("请先进入版块")
        _state.update { it.copy(loading = true, message = null) }
        val form = repository.loadNewThreadForm(board.url)
        _state.update { it.copy(composeForm = form, loading = false) }
    }

    fun submitNewThread(typeId: String, subject: String, message: String) = launchTask {
        val form = state.value.composeForm ?: error("发帖表单未加载")
        _state.update { it.copy(loading = true, message = null) }
        repository.submitNewThread(form, typeId, subject, message)
        _state.update { it.copy(composeForm = null, loading = false, message = "帖子已提交，刷新后查看结果" ) }
        state.value.selectedBoard?.let { openBoard(it) }
    }

    fun submitReply(message: String) = launchTask {
        val detail = state.value.threadDetail ?: error("请先打开帖子")
        _state.update { it.copy(loading = true, message = null) }
        repository.submitReply(detail, message)
        val selectedThread = state.value.selectedThread
        _state.update { it.copy(loading = false, message = "回复已提交", threadDetail = null, detectedLinks = emptyList()) }
        if (selectedThread != null) {
            openThread(selectedThread)
        }
    }

    fun prepareRemark(post: PostItem) = launchTask {
        _state.update { it.copy(loading = true, message = null) }
        val form = repository.loadRemarkForm(post)
        _state.update { it.copy(remarkForm = form, loading = false) }
    }

    fun submitRemark(message: String) = launchTask {
        val form = state.value.remarkForm ?: error("点评表单未加载")
        val detail = state.value.threadDetail ?: error("请先打开帖子")
        _state.update { it.copy(loading = true, message = null) }
        repository.submitRemark(form, message)
        val refreshed = repository.loadThread(detail.url)
        _state.update {
            it.copy(
                remarkForm = null,
                threadDetail = refreshed,
                detectedLinks = ThreadLinkRecognizer.extract(refreshed),
                loading = false,
                message = "点评已提交"
            )
        }
    }

    fun loadMoreReplies() = launchTask {
        val detail = state.value.threadDetail ?: error("请先打开帖子")
        val nextPageUrl = detail.nextPageUrl ?: return@launchTask
        _state.update { it.copy(loading = true, message = null) }
        val nextPage = repository.loadThreadPage(nextPageUrl)
        val mergedDetail = detail.copy(
            posts = detail.posts + nextPage.posts,
            nextPageUrl = nextPage.nextPageUrl,
            currentPage = nextPage.currentPage,
            totalPages = nextPage.totalPages
        )
        _state.update {
            it.copy(
                loading = false,
                threadDetail = mergedDetail,
                detectedLinks = ThreadLinkRecognizer.extract(mergedDetail)
            )
        }
    }

    fun favoriteThread() = launchTask {
        val detail = state.value.threadDetail ?: error("请先打开帖子")
        _state.update { it.copy(loading = true, message = null) }
        repository.favoriteThread(detail)
        val refreshed = repository.loadThread(detail.url)
        _state.update {
            it.copy(
                loading = false,
                threadDetail = refreshed,
                detectedLinks = ThreadLinkRecognizer.extract(refreshed),
                message = "收藏成功"
            )
        }
    }

    fun loadMoreThreads() = launchTask {
        val nextPageUrl = state.value.threadsNextPageUrl ?: return@launchTask
        _state.update { it.copy(loading = true, message = null) }
        val page = repository.loadThreadsPage(nextPageUrl)
        _state.update {
            it.copy(
                loading = false,
                threads = it.threads + page.threads,
                threadsNextPageUrl = page.nextPageUrl
            )
        }
    }

    fun openUserCenter(uid: String? = null) = launchTask {
        val session = state.value.session ?: error("请先登录")
        val targetUid = uid ?: session.uid.ifBlank { error("无法识别当前用户 UID") }
        _state.update {
            it.copy(
                loading = true,
                message = null,
                userCenterUid = targetUid,
                userCenterVisible = true,
                threadOpenedFromUserCenter = false
            )
        }
        val threads = repository.loadUserThreads(targetUid, "thread")
        val replies = repository.loadUserThreads(targetUid, "reply")
        val favorites = if (targetUid == session.uid) repository.loadUserFavorites() else UserThreadListPage(emptyList(), null)
        val profile = repository.loadUserProfile(targetUid)
        _state.update {
            it.copy(
                loading = false,
                userThreads = threads.items,
                userThreadsNextPageUrl = threads.nextPageUrl,
                userReplies = replies.items,
                userRepliesNextPageUrl = replies.nextPageUrl,
                userFavorites = favorites.items,
                userFavoritesNextPageUrl = favorites.nextPageUrl,
                userProfile = profile,
                userCenterUid = targetUid,
                userCenterVisible = true
            )
        }
    }

    fun loadMoreUserThreads() = launchTask {
        val nextPageUrl = state.value.userThreadsNextPageUrl ?: return@launchTask
        _state.update { it.copy(loading = true, message = null) }
        val page = repository.loadUserThreadsPage(nextPageUrl)
        _state.update {
            it.copy(
                loading = false,
                userThreads = it.userThreads + page.items,
                userThreadsNextPageUrl = page.nextPageUrl
            )
        }
    }

    fun loadMoreUserReplies() = launchTask {
        val nextPageUrl = state.value.userRepliesNextPageUrl ?: return@launchTask
        _state.update { it.copy(loading = true, message = null) }
        val page = repository.loadUserThreadsPage(nextPageUrl)
        _state.update {
            it.copy(
                loading = false,
                userReplies = it.userReplies + page.items,
                userRepliesNextPageUrl = page.nextPageUrl
            )
        }
    }

    fun loadMoreUserFavorites() = launchTask {
        val nextPageUrl = state.value.userFavoritesNextPageUrl ?: return@launchTask
        _state.update { it.copy(loading = true, message = null) }
        val page = repository.loadUserFavoritesPage(nextPageUrl)
        _state.update {
            it.copy(
                loading = false,
                userFavorites = it.userFavorites + page.items,
                userFavoritesNextPageUrl = page.nextPageUrl
            )
        }
    }

    fun deleteUserFavorite(item: UserThreadItem) = launchTask {
        val actionUrl = item.deleteActionUrl ?: error("当前收藏不支持删除")
        _state.update { it.copy(loading = true, message = null) }
        repository.deleteUserFavorite(actionUrl)
        val refreshed = repository.loadUserFavorites()
        _state.update {
            it.copy(
                loading = false,
                userFavorites = refreshed.items,
                userFavoritesNextPageUrl = refreshed.nextPageUrl,
                message = "已删除收藏"
            )
        }
    }

    fun closeUserCenter() {
        _state.update {
            it.copy(
                userCenterVisible = false,
                userThreads = emptyList(),
                userThreadsNextPageUrl = null,
                userReplies = emptyList(),
                userRepliesNextPageUrl = null,
                userFavorites = emptyList(),
                userFavoritesNextPageUrl = null,
                userProfile = null,
                userCenterUid = ""
            )
        }
    }

    fun clearCompose() {
        _state.update { it.copy(composeForm = null) }
    }

    fun clearRemark() {
        _state.update { it.copy(remarkForm = null) }
    }

    fun closeThread() {
        _state.update {
            it.copy(
                selectedThread = null,
                threadDetail = null,
                userCenterVisible = if (it.threadOpenedFromUserCenter) true else it.userCenterVisible,
                threadOpenedFromUserCenter = false,
                detectedLinks = emptyList()
            )
        }
    }

    fun leaveBoard() {
        _state.update {
            it.copy(
                selectedBoard = null,
                selectedThread = null,
                threadDetail = null,
                detectedLinks = emptyList(),
                composeForm = null,
                threadsNextPageUrl = null,
                userCenterVisible = false,
                threadOpenedFromUserCenter = false
            )
        }
    }

    fun updateThreadListScroll(index: Int, offset: Int) {
        _state.update {
            if (
                it.threadListFirstVisibleItemIndex == index &&
                it.threadListFirstVisibleItemScrollOffset == offset
            ) {
                it
            } else {
                it.copy(
                    threadListFirstVisibleItemIndex = index,
                    threadListFirstVisibleItemScrollOffset = offset
                )
            }
        }
    }

    fun clearChallenge() {
        _state.update { it.copy(challenge = null) }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun refreshSession() = launchTask {
        val session = repository.currentSession()
        _state.update { it.copy(session = session) }
    }

    fun refreshLocalFavorites() {
        _state.update { it.copy(localFavoriteImages = LocalImageFavorites.load()) }
    }

    fun deleteLocalFavorites(ids: Set<String>) = launchTask {
        LocalImageFavorites.remove(ids)
        _state.update {
            it.copy(
                localFavoriteImages = LocalImageFavorites.load(),
                message = "已取消收藏"
            )
        }
    }

    private fun launchTask(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { block() }
            } catch (t: Throwable) {
                _state.update { it.copy(loading = false, message = t.message ?: "操作失败") }
            }
        }
    }
}

private object AppStateSnapshot {
    @Volatile
    private var cached: AppState? = null

    fun restore(): AppState? = cached

    fun save(state: AppState) {
        cached = state
    }
}
