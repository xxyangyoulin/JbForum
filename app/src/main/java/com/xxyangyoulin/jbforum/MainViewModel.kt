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
    val userThreadsLoaded: Boolean = false,
    val userRepliesLoaded: Boolean = false,
    val userFavoritesLoaded: Boolean = false,
    val userProfileLoaded: Boolean = false,
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
    val forumMessageStatus: ForumMessageStatus = ForumMessageStatus(),
    val loading: Boolean = false,
    val message: String? = null
)

class MainViewModel(
    private val repository: ForumRepository = ForumRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(AppStateSnapshot.restore() ?: AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        _state.update { it.copy(forumMessageStatus = MessageStatusStore.value()) }
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
        viewModelScope.launch {
            MessageStatusStore.status.collect { status ->
                _state.update {
                    it.copy(
                        forumMessageStatus = status,
                        session = if (!status.loggedIn) null else it.session
                    )
                }
            }
        }
        val cachedBoards = repository.loadCachedBoards()
        if (cachedBoards.isNotEmpty()) {
            _state.update { it.copy(boards = cachedBoards) }
        }
        launchTask {
            _state.update { it.copy(loading = true, message = null) }
            val boards = repository.loadBoards()
            _state.update {
                it.copy(
                    boards = boards,
                    session = repository.latestSession(),
                    localFavoriteImages = LocalImageFavorites.load(),
                    forumMessageStatus = repository.latestMessageStatus(),
                    loading = false
                )
            }
        }
    }

    fun refreshBoards() = launchTask {
        _state.update { it.copy(loading = true, message = null, selectedBoard = null, selectedThread = null, threadDetail = null, detectedLinks = emptyList(), userCenterVisible = false) }
        val boards = repository.loadBoards()
        _state.update { it.copy(boards = boards, forumMessageStatus = repository.latestMessageStatus(), loading = false) }
    }

    fun openBoard(board: Board, forceRefresh: Boolean = false) = launchTask {
        val memoryCached = boardCache[board.url]
        val diskCached = if (memoryCached == null) ThreadListDiskCache.load(board.url) else null
        val cached = memoryCached ?: diskCached?.let { BoardCache(it.threads, it.nextPageUrl) }
        _state.update {
            it.copy(
                loading = true,
                threads = cached?.threads ?: emptyList(),
                threadsNextPageUrl = cached?.nextPageUrl,
                selectedBoard = board,
                selectedThread = null,
                threadDetail = null,
                detectedLinks = emptyList(),
                message = null
            )
        }
        val page = repository.loadThreads(board.url)
        boardCache[board.url] = BoardCache(page.threads, page.nextPageUrl)
        ThreadListDiskCache.save(board.url, page)
        _state.update {
            it.copy(
                threads = page.threads,
                threadsNextPageUrl = page.nextPageUrl,
                forumMessageStatus = repository.latestMessageStatus(),
                loading = false
            )
        }
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
        _state.update {
            it.copy(
                threads = page.threads,
                threadsNextPageUrl = page.nextPageUrl,
                forumMessageStatus = repository.latestMessageStatus(),
                loading = false
            )
        }
    }

    fun openThread(thread: ThreadSummary) = launchTask {
        val openedFromUserCenter = state.value.userCenterVisible
        val memoryCached = threadDetailCache[thread.url]
        val diskCachedDetail = if (memoryCached == null) ThreadDetailDiskCache.load(thread.url) else null
        val cached = memoryCached ?: diskCachedDetail?.let { ThreadDetailCache(it, ThreadLinkRecognizer.extract(it)) }
        _state.update {
            it.copy(
                loading = cached == null,
                threadRefreshing = cached != null,
                selectedThread = thread,
                userCenterVisible = false,
                threadOpenedFromUserCenter = openedFromUserCenter,
                remarkForm = null,
                message = null,
                threadDetail = cached?.detail,
                detectedLinks = cached?.detectedLinks ?: emptyList()
            )
        }
        val detail = repository.loadThread(thread.url)
        val links = ThreadLinkRecognizer.extract(detail)
        cacheThreadDetail(detail, links)
        _state.update {
            it.copy(
                threadDetail = detail,
                detectedLinks = links,
                forumMessageStatus = repository.latestMessageStatus(),
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
        CookiePersistence.clear()
        LoginPersistence.clear()
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
        _state.update {
            it.copy(
                session = session,
                challenge = null,
                forumMessageStatus = repository.latestMessageStatus(),
                loading = false,
                message = "已登录为 ${session.username}"
            )
        }
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
        val links = ThreadLinkRecognizer.extract(refreshed)
        cacheThreadDetail(refreshed, links)
        _state.update {
            it.copy(
                remarkForm = null,
                threadDetail = refreshed,
                detectedLinks = links,
                forumMessageStatus = repository.latestMessageStatus(),
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
        val links = ThreadLinkRecognizer.extract(mergedDetail)
        cacheThreadDetail(mergedDetail, links)
        _state.update {
            it.copy(
                loading = false,
                threadDetail = mergedDetail,
                detectedLinks = links,
                forumMessageStatus = repository.latestMessageStatus()
            )
        }
    }

    fun favoriteThread() = launchTask {
        val detail = state.value.threadDetail ?: error("请先打开帖子")
        _state.update { it.copy(loading = true, message = null) }
        repository.favoriteThread(detail)
        val refreshed = repository.loadThread(detail.url)
        val links = ThreadLinkRecognizer.extract(refreshed)
        cacheThreadDetail(refreshed, links)
        _state.update {
            it.copy(
                loading = false,
                threadDetail = refreshed,
                detectedLinks = links,
                forumMessageStatus = repository.latestMessageStatus(),
                message = "收藏成功"
            )
        }
    }

    fun loadMoreThreads() = launchTask {
        val nextPageUrl = state.value.threadsNextPageUrl ?: return@launchTask
        _state.update { it.copy(loading = true, message = null) }
        val page = repository.loadThreadsPage(nextPageUrl)
        val mergedThreads = state.value.threads + page.threads
        val mergedNextPageUrl = page.nextPageUrl
        val boardUrl = state.value.selectedBoard?.url
        if (!boardUrl.isNullOrBlank()) {
            val mergedPage = ThreadListPage(threads = mergedThreads, nextPageUrl = mergedNextPageUrl)
            boardCache[boardUrl] = BoardCache(mergedThreads, mergedNextPageUrl)
            ThreadListDiskCache.save(boardUrl, mergedPage)
        }
        _state.update {
            it.copy(
                loading = false,
                threads = mergedThreads,
                threadsNextPageUrl = mergedNextPageUrl,
                forumMessageStatus = repository.latestMessageStatus()
            )
        }
    }

    fun openUserCenter(uid: String? = null, forceRefresh: Boolean = false) = launchTask {
        val session = state.value.session ?: error("请先登录")
        val targetUid = uid ?: session.uid.ifBlank { error("无法识别当前用户 UID") }
        val defaultTab = if (targetUid == session.uid) "favorite" else "thread"
        val cached = userCenterCache[targetUid]
        _state.update {
            it.copy(
                loading = forceRefresh || cached == null || !cached.profileLoaded || !cached.isTabLoaded(defaultTab),
                message = null,
                userCenterUid = targetUid,
                userCenterVisible = true,
                threadOpenedFromUserCenter = false,
                userThreads = cached?.threads ?: emptyList(),
                userThreadsNextPageUrl = cached?.threadsNextPageUrl,
                userReplies = cached?.replies ?: emptyList(),
                userRepliesNextPageUrl = cached?.repliesNextPageUrl,
                userFavorites = cached?.favorites ?: emptyList(),
                userFavoritesNextPageUrl = cached?.favoritesNextPageUrl,
                userProfile = cached?.profile,
                userThreadsLoaded = cached?.threadsLoaded == true,
                userRepliesLoaded = cached?.repliesLoaded == true,
                userFavoritesLoaded = cached?.favoritesLoaded == true,
                userProfileLoaded = cached?.profileLoaded == true
            )
        }
        if (cached != null && !forceRefresh && cached.profileLoaded && cached.isTabLoaded(defaultTab)) return@launchTask
        val profile = ForumRepository().loadUserProfile(targetUid)
        val updatedCache = when (defaultTab) {
            "favorite" -> {
                val favorites = ForumRepository().loadUserFavorites()
                (cached ?: UserCenterCache()).copy(
                    favorites = favorites.items,
                    favoritesNextPageUrl = favorites.nextPageUrl,
                    favoritesLoaded = true,
                    profile = profile,
                    profileLoaded = true
                )
            }
            else -> {
                val threads = ForumRepository().loadUserThreads(targetUid, "thread")
                (cached ?: UserCenterCache()).copy(
                    threads = threads.items,
                    threadsNextPageUrl = threads.nextPageUrl,
                    threadsLoaded = true,
                    profile = profile,
                    profileLoaded = true
                )
            }
        }
        userCenterCache[targetUid] = updatedCache
        _state.update {
            it.copy(
                loading = false,
                userThreads = updatedCache.threads,
                userThreadsNextPageUrl = updatedCache.threadsNextPageUrl,
                userReplies = updatedCache.replies,
                userRepliesNextPageUrl = updatedCache.repliesNextPageUrl,
                userFavorites = updatedCache.favorites,
                userFavoritesNextPageUrl = updatedCache.favoritesNextPageUrl,
                userProfile = updatedCache.profile,
                userThreadsLoaded = updatedCache.threadsLoaded,
                userRepliesLoaded = updatedCache.repliesLoaded,
                userFavoritesLoaded = updatedCache.favoritesLoaded,
                userProfileLoaded = updatedCache.profileLoaded,
                forumMessageStatus = repository.latestMessageStatus(),
                userCenterUid = targetUid,
                userCenterVisible = true
            )
        }
    }

    fun ensureUserCenterTabLoaded(tab: String) = launchTask {
        val session = state.value.session ?: error("请先登录")
        val uid = state.value.userCenterUid.ifBlank { session.uid.ifBlank { error("无法识别当前用户 UID") } }
        val current = userCenterCache[uid] ?: UserCenterCache()
        if (current.isTabLoaded(tab)) return@launchTask
        _state.update { it.copy(loading = true, message = null) }
        val updated = when (tab) {
            "favorite" -> {
                if (uid == session.uid) {
                    val favorites = repository.loadUserFavorites()
                    current.copy(
                        favorites = favorites.items,
                        favoritesNextPageUrl = favorites.nextPageUrl,
                        favoritesLoaded = true
                    )
                } else {
                    current.copy(favoritesLoaded = true)
                }
            }
            "reply" -> {
                val replies = repository.loadUserThreads(uid, "reply")
                current.copy(
                    replies = replies.items,
                    repliesNextPageUrl = replies.nextPageUrl,
                    repliesLoaded = true
                )
            }
            else -> {
                val threads = repository.loadUserThreads(uid, "thread")
                current.copy(
                    threads = threads.items,
                    threadsNextPageUrl = threads.nextPageUrl,
                    threadsLoaded = true
                )
            }
        }
        userCenterCache[uid] = updated
        _state.update {
            it.copy(
                loading = false,
                userThreads = updated.threads,
                userThreadsNextPageUrl = updated.threadsNextPageUrl,
                userReplies = updated.replies,
                userRepliesNextPageUrl = updated.repliesNextPageUrl,
                userFavorites = updated.favorites,
                userFavoritesNextPageUrl = updated.favoritesNextPageUrl,
                userThreadsLoaded = updated.threadsLoaded,
                userRepliesLoaded = updated.repliesLoaded,
                userFavoritesLoaded = updated.favoritesLoaded,
                forumMessageStatus = repository.latestMessageStatus()
            )
        }
    }

    fun refreshUserCenterTab(tab: String, uid: String? = null) = launchTask {
        val session = state.value.session ?: error("请先登录")
        val targetUid = uid ?: state.value.userCenterUid.ifBlank { session.uid.ifBlank { error("无法识别当前用户 UID") } }
        val current = userCenterCache[targetUid] ?: UserCenterCache()
        _state.update { it.copy(loading = true, message = null) }
        val profile = repository.loadUserProfile(targetUid)
        val updated = when (tab) {
            "favorite" -> {
                if (targetUid == session.uid) {
                    val favorites = repository.loadUserFavorites()
                    current.copy(
                        favorites = favorites.items,
                        favoritesNextPageUrl = favorites.nextPageUrl,
                        favoritesLoaded = true,
                        profile = profile,
                        profileLoaded = true
                    )
                } else {
                    current.copy(profile = profile, profileLoaded = true)
                }
            }
            "reply" -> {
                val replies = repository.loadUserThreads(targetUid, "reply")
                current.copy(
                    replies = replies.items,
                    repliesNextPageUrl = replies.nextPageUrl,
                    repliesLoaded = true,
                    profile = profile,
                    profileLoaded = true
                )
            }
            "profile" -> current.copy(profile = profile, profileLoaded = true)
            else -> {
                val threads = repository.loadUserThreads(targetUid, "thread")
                current.copy(
                    threads = threads.items,
                    threadsNextPageUrl = threads.nextPageUrl,
                    threadsLoaded = true,
                    profile = profile,
                    profileLoaded = true
                )
            }
        }
        userCenterCache[targetUid] = updated
        _state.update {
            it.copy(
                loading = false,
                userCenterUid = targetUid,
                userThreads = updated.threads,
                userThreadsNextPageUrl = updated.threadsNextPageUrl,
                userReplies = updated.replies,
                userRepliesNextPageUrl = updated.repliesNextPageUrl,
                userFavorites = updated.favorites,
                userFavoritesNextPageUrl = updated.favoritesNextPageUrl,
                userProfile = updated.profile,
                userThreadsLoaded = updated.threadsLoaded,
                userRepliesLoaded = updated.repliesLoaded,
                userFavoritesLoaded = updated.favoritesLoaded,
                userProfileLoaded = updated.profileLoaded,
                forumMessageStatus = repository.latestMessageStatus()
            )
        }
    }

    fun loadMoreUserThreads() = launchTask {
        val nextPageUrl = state.value.userThreadsNextPageUrl ?: return@launchTask
        _state.update { it.copy(loading = true, message = null) }
        val page = repository.loadUserThreadsPage(nextPageUrl)
        val merged = state.value.userThreads + page.items
        val uid = state.value.userCenterUid
        if (uid.isNotBlank()) {
            val current = userCenterCache[uid]
            if (current != null) {
                userCenterCache[uid] = current.copy(
                    threads = merged,
                    threadsNextPageUrl = page.nextPageUrl
                )
            }
        }
        _state.update {
            it.copy(
                loading = false,
                userThreads = merged,
                userThreadsNextPageUrl = page.nextPageUrl,
                forumMessageStatus = repository.latestMessageStatus()
            )
        }
    }

    fun loadMoreUserReplies() = launchTask {
        val nextPageUrl = state.value.userRepliesNextPageUrl ?: return@launchTask
        _state.update { it.copy(loading = true, message = null) }
        val page = repository.loadUserThreadsPage(nextPageUrl)
        val merged = state.value.userReplies + page.items
        val uid = state.value.userCenterUid
        if (uid.isNotBlank()) {
            val current = userCenterCache[uid]
            if (current != null) {
                userCenterCache[uid] = current.copy(
                    replies = merged,
                    repliesNextPageUrl = page.nextPageUrl
                )
            }
        }
        _state.update {
            it.copy(
                loading = false,
                userReplies = merged,
                userRepliesNextPageUrl = page.nextPageUrl,
                forumMessageStatus = repository.latestMessageStatus()
            )
        }
    }

    fun loadMoreUserFavorites() = launchTask {
        val nextPageUrl = state.value.userFavoritesNextPageUrl ?: return@launchTask
        _state.update { it.copy(loading = true, message = null) }
        val page = repository.loadUserFavoritesPage(nextPageUrl)
        val merged = state.value.userFavorites + page.items
        val uid = state.value.userCenterUid
        if (uid.isNotBlank()) {
            val current = userCenterCache[uid]
            if (current != null) {
                userCenterCache[uid] = current.copy(
                    favorites = merged,
                    favoritesNextPageUrl = page.nextPageUrl
                )
            }
        }
        _state.update {
            it.copy(
                loading = false,
                userFavorites = merged,
                userFavoritesNextPageUrl = page.nextPageUrl,
                forumMessageStatus = repository.latestMessageStatus()
            )
        }
    }

    fun deleteUserFavorite(item: UserThreadItem) = launchTask {
        val actionUrl = item.deleteActionUrl ?: error("当前收藏不支持删除")
        _state.update { it.copy(loading = true, message = null) }
        repository.deleteUserFavorite(actionUrl)
        val refreshed = repository.loadUserFavorites()
        val uid = state.value.userCenterUid
        if (uid.isNotBlank()) {
            val current = userCenterCache[uid]
            if (current != null) {
                userCenterCache[uid] = current.copy(
                    favorites = refreshed.items,
                    favoritesNextPageUrl = refreshed.nextPageUrl
                )
            }
        }
        _state.update {
            it.copy(
                loading = false,
                userFavorites = refreshed.items,
                userFavoritesNextPageUrl = refreshed.nextPageUrl,
                forumMessageStatus = repository.latestMessageStatus(),
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
                userThreadsLoaded = false,
                userRepliesLoaded = false,
                userFavoritesLoaded = false,
                userProfileLoaded = false,
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

    fun clearThreadListScroll() {
        _state.update {
            it.copy(
                threadListFirstVisibleItemIndex = 0,
                threadListFirstVisibleItemScrollOffset = 0
            )
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
        _state.update { it.copy(session = session, forumMessageStatus = repository.latestMessageStatus()) }
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

    private fun cacheThreadDetail(detail: ThreadDetail, links: List<DetectedLink>) {
        threadDetailCache[detail.url] = ThreadDetailCache(detail, links)
        ThreadDetailDiskCache.save(detail)
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

private data class BoardCache(
    val threads: List<ThreadSummary>,
    val nextPageUrl: String?
)

private data class ThreadDetailCache(
    val detail: ThreadDetail,
    val detectedLinks: List<DetectedLink>
)

private data class UserCenterCache(
    val threads: List<UserThreadItem> = emptyList(),
    val threadsNextPageUrl: String? = null,
    val replies: List<UserThreadItem> = emptyList(),
    val repliesNextPageUrl: String? = null,
    val favorites: List<UserThreadItem> = emptyList(),
    val favoritesNextPageUrl: String? = null,
    val profile: UserProfile? = null,
    val threadsLoaded: Boolean = false,
    val repliesLoaded: Boolean = false,
    val favoritesLoaded: Boolean = false,
    val profileLoaded: Boolean = false
) {
    fun isTabLoaded(tab: String): Boolean {
        return when (tab) {
            "favorite" -> favoritesLoaded
            "reply" -> repliesLoaded
            "profile" -> profileLoaded
            else -> threadsLoaded
        }
    }
}

private const val THREAD_DETAIL_CACHE_LIMIT = 20

private val boardCache = object : LinkedHashMap<String, BoardCache>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, BoardCache>?): Boolean {
        return size > 20
    }
}
private val threadDetailCache = object : LinkedHashMap<String, ThreadDetailCache>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ThreadDetailCache>?): Boolean {
        return size > THREAD_DETAIL_CACHE_LIMIT
    }
}

private val userCenterCache = object : LinkedHashMap<String, UserCenterCache>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, UserCenterCache>?): Boolean {
        return size > 20
    }
}
