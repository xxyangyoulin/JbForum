package com.xxyangyoulin.jbforum

import kotlinx.coroutines.sync.withLock
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * 论坛数据仓库
 * 负责论坛业务逻辑，组合 ApiClient 和 HtmlParser
 */
class ForumRepository {
    private val apiClient = ForumApiClient()

    /**
     * 获取用于图片加载的 OkHttpClient
     */
    fun imageClient(): okhttp3.OkHttpClient = apiClient.imageClient()

    /**
     * 加载板块列表
     */
    suspend fun loadBoards(): List<Board> {
        return apiClient.withRequestLock {
            apiClient.ensureForumSession()
            var document = apiClient.getDocument("forum.php")
            var boards = ForumHtmlParser.parseBoards(document)
            if (boards.isEmpty()) {
                document = apiClient.getDocument("./")
                boards = ForumHtmlParser.parseBoards(document)
            }
            if (boards.isEmpty()) {
                val message = ForumHtmlParser.extractMessage(document)
                error(message.ifBlank { "板块页解析失败：页面结构可能已变化，请下拉刷新重试" })
            }
            BoardDiskCache.save(boards)
            boards
        }
    }

    fun loadCachedBoards(): List<Board> = BoardDiskCache.load()

    /**
     * 加载帖子列表
     */
    suspend fun loadThreads(boardUrl: String): ThreadListPage {
        return apiClient.withRequestLock {
            apiClient.ensureForumSession()
            val document = apiClient.getDocument(boardUrl)
            val threads = ForumHtmlParser.parseThreadSummaries(document)
            if (threads.isEmpty()) {
                val hasNoResultHint = document.text().contains("沒有相關帖子") ||
                    document.text().contains("没有相关帖子") ||
                    document.text().contains("暫時沒有") ||
                    document.text().contains("暂无")
                if (!hasNoResultHint) {
                    val message = ForumHtmlParser.extractMessage(document)
                    error(message.ifBlank { "帖子列表解析失败：页面结构可能已变化，请下拉刷新重试" })
                }
            }
            val nextPageUrl = document.selectFirst(".pg a.nxt, .pgt a.nxt")
                ?.attr("href")
                ?.takeIf { it.isNotBlank() }
                ?.let { apiClient.absoluteUrl(it) }
                ?.takeIf { it.isNotBlank() }
            ThreadListPage(
                threads = threads,
                nextPageUrl = nextPageUrl
            )
        }
    }

    /**
     * 加载帖子列表页
     */
    suspend fun loadThreadsPage(boardPageUrl: String): ThreadListPage {
        return loadThreads(boardPageUrl)
    }

    /**
     * 搜索帖子
     */
    suspend fun searchThreads(keyword: String): ThreadListPage {
        return apiClient.withRequestLock {
            apiClient.ensureForumSession()
            val searchDocument = apiClient.getDocument("search.php?mod=forum")
            val formhash = searchDocument.selectFirst("input[name=formhash]")?.attr("value").orEmpty()
            val body = FormBody.Builder()
                .add("formhash", formhash)
                .add("srchtxt", keyword)
                .add("searchsubmit", "yes")
                .build()
            val html = apiClient.postHtml(
                url = apiClient.absoluteUrl("search.php?mod=forum"),
                body = body,
                referer = apiClient.absoluteUrl("search.php?mod=forum")
            )
            val document = Jsoup.parse(html, ForumDomainConfig.requireBaseUrl())
            val threads = ForumHtmlParser.parseThreadSummaries(document)
            val nextPageUrl = document.selectFirst(".pg a.nxt, .pgs .pg a.nxt")
                ?.attr("href")
                ?.takeIf { it.isNotBlank() }
                ?.let { apiClient.absoluteUrl(it) }
            ThreadListPage(
                threads = threads,
                nextPageUrl = nextPageUrl
            )
        }
    }

    /**
     * 加载帖子详情
     */
    suspend fun loadThread(threadUrl: String): ThreadDetail {
        return apiClient.withRequestLock {
            apiClient.ensureForumSession()
            val document = apiClient.getDocument(threadUrl)
            val detail = ForumHtmlParser.parseThreadDetail(threadUrl, document)
            if (detail.posts.isEmpty()) {
                val message = ForumHtmlParser.extractMessage(document)
                error(message.ifBlank { "帖子详情解析失败：页面结构可能已变化，请下拉刷新重试" })
            }
            detail
        }
    }

    /**
     * 加载帖子详情页
     */
    suspend fun loadThreadPage(threadPageUrl: String): ThreadDetail {
        return loadThread(threadPageUrl)
    }

    /**
     * 获取登录验证码
     */
    suspend fun fetchLoginCaptcha(): CaptchaChallenge {
        return apiClient.withRequestLock {
            apiClient.clearSession()
            apiClient.ensureForumSession()
            val document = loadLoginDocumentWithRetry()
            val form = ForumHtmlParser.findLoginForm(document) ?: error("找不到登录表单")
            val loginAction = apiClient.absoluteUrl(form.attr("action").replace("&amp;", "&"))
            val loginhash = loginAction.substringAfter("loginhash=", "")
            val formhash = form.selectFirst("input[name=formhash]")?.attr("value").orEmpty()
            val referer = form.selectFirst("input[name=referer]")?.attr("value").orEmpty()
            val hiddenFields = form.select("input[type=hidden][name]").associate { input ->
                input.attr("name") to input.attr("value")
            }
            val seccodeHash = Regex("updateseccode\\('([^']+)'")
                .find(document.html())
                ?.groupValues
                ?.get(1)
                .orEmpty()
            require(seccodeHash.isNotBlank()) { "找不到验证码配置" }

            val imageRequest = Request.Builder()
                .url(apiClient.absoluteUrl("misc.php?mod=seccode&update=${System.currentTimeMillis()}&idhash=$seccodeHash"))
                .header("Referer", apiClient.absoluteUrl("member.php?mod=logging&action=login"))
                .build()
            val bytes = apiClient.executeForBytes(imageRequest)
            if (bytes.isEmpty()) error("验证码内容为空")
            CaptchaChallenge(
                imageBytes = bytes,
                formhash = formhash,
                loginAction = loginAction,
                loginhash = loginhash,
                referer = referer,
                seccodeHash = seccodeHash,
                hiddenFields = hiddenFields
            )
        }
    }

    private suspend fun loadLoginDocumentWithRetry(): Document {
        val path = "member.php?mod=logging&action=login"
        var document = apiClient.getDocument(path)
        var form = ForumHtmlParser.findLoginForm(document)
        if (form == null) {
            document = apiClient.getDocument(path)
            form = ForumHtmlParser.findLoginForm(document)
        }
        return document
    }

    /**
     * 登录
     */
    suspend fun login(username: String, password: String, captcha: String, challenge: CaptchaChallenge): UserSession {
        return apiClient.withRequestLock {
            val bodyBuilder = FormBody.Builder()
            challenge.hiddenFields.forEach { (key, value) ->
                bodyBuilder.add(key, value)
            }
            val body = bodyBuilder
                .add("formhash", challenge.formhash)
                .add("referer", challenge.referer)
                .add("loginfield", "username")
                .add("username", username)
                .add("password", password)
                .add("questionid", "0")
                .add("answer", "")
                .add("seccodehash", challenge.seccodeHash)
                .add("seccodemodid", "member::logging")
                .add("seccodeverify", captcha)
                .add("cookietime", AppConstants.LOGIN_COOKIE_LIFETIME_SECONDS.toString())
                .add("loginsubmit", "true")
                .build()
            val responseHtml = apiClient.postHtml(challenge.loginAction, body, apiClient.absoluteUrl("member.php?mod=logging&action=login"))
            val document = Jsoup.parse(responseHtml, ForumDomainConfig.requireBaseUrl())
            val sessionDoc = apiClient.getDocument("forum.php")
            val name = sessionDoc.selectFirst(".member-name a")?.text()?.trim().orEmpty()
            val uid = ForumHtmlParser.extractUid(sessionDoc.selectFirst(".member-name a")?.attr("href").orEmpty())
            if (name.isNotBlank() && name != "帳號") {
                return@withRequestLock UserSession(name.ifBlank { username }, uid)
            }

            val successText = document.text()
            if (successText.contains("歡迎您回來") || successText.contains("欢迎您回来") || apiClient.cookieJar.hasCookie("4fJN_2132_auth")) {
                val fallbackName = sessionDoc.selectFirst(".member-name a")?.text()?.trim().orEmpty()
                val fallbackUid = ForumHtmlParser.extractUid(sessionDoc.selectFirst(".member-name a")?.attr("href").orEmpty())
                return@withRequestLock UserSession(fallbackName.ifBlank { username }, fallbackUid)
            }

            val message = ForumHtmlParser.extractMessage(document).ifBlank { "登录失败，请检查验证码或账号密码" }
            error(message)
        }
    }

    /**
     * 加载发帖表单
     */
    suspend fun loadNewThreadForm(boardUrl: String): ComposeForm {
        return apiClient.withRequestLock {
            apiClient.ensureForumSession()
            val resolved = boardUrl.toHttpUrlOrNull() ?: error("无效的 URL")
            val fid = resolved.queryParameter("fid") ?: error("缺少 fid")
            val typeId = resolved.queryParameter("typeid")
            val url = buildString {
                append("forum.php?mod=post&action=newthread&fid=")
                append(fid)
                append("&extra=page%3D1")
                if (!typeId.isNullOrBlank()) {
                    append("&filter=typeid&typeid=")
                    append(typeId)
                }
            }
            val document = apiClient.getDocument(url)
            ForumHtmlParser.parseComposeForm(document, url)
        }
    }

    /**
     * 发表新帖
     */
    suspend fun submitNewThread(form: ComposeForm, typeId: String, subject: String, message: String) {
        val builder = FormBody.Builder()
        form.hiddenFields.forEach { (key, value) -> builder.add(key, value) }
        builder.add("typeid", typeId)
        builder.add("subject", subject)
        builder.add("message", message)
        builder.add("topicsubmit", "yes")
        val html = apiClient.postHtml(form.actionUrl, builder.build(), form.actionUrl)
        val text = Jsoup.parse(html, ForumDomainConfig.requireBaseUrl()).text()
        if (text.contains("發表帖子") || text.contains("请先") || text.contains("錯誤") || text.contains("错误")) {
            if (!text.contains("歡迎您回來")) {
                error(ForumHtmlParser.extractMessage(Jsoup.parse(html, ForumDomainConfig.requireBaseUrl())).ifBlank { "发帖失败" })
            }
        }
    }

    /**
     * 回复帖子
     */
    suspend fun submitReply(detail: ThreadDetail, message: String) {
        val action = detail.replyAction ?: error("当前主题不支持回复")
        val builder = FormBody.Builder()
        detail.replyFields.forEach { (key, value) -> builder.add(key, value) }
        builder.add("message", message)
        builder.add("replysubmit", "yes")
        val html = apiClient.postHtml(action, builder.build(), action)
        val text = Jsoup.parse(html, ForumDomainConfig.requireBaseUrl()).text()
        if (text.contains("參與/回復主題") || text.contains("错误") || text.contains("錯誤")) {
            if (!text.contains("發表回復成功")) {
                error(ForumHtmlParser.extractMessage(Jsoup.parse(html, ForumDomainConfig.requireBaseUrl())).ifBlank { "回复失败" })
            }
        }
    }

    suspend fun loadRemarkForm(post: PostItem): RemarkForm {
        return apiClient.withRequestLock {
            apiClient.ensureForumSession()
            val targetLabel = listOf(post.floor, post.author.ifBlank { "匿名" }).filter { it.isNotBlank() }.joinToString(" · ")
            val document = apiClient.getDocument("forum.php?mod=misc&action=comment&pid=${post.pid}&infloat=yes&handlekey=cpost_${post.pid}")
            ForumHtmlParser.parseRemarkForm(document, post.pid, targetLabel)
        }
    }

    suspend fun submitRemark(form: RemarkForm, message: String) {
        val builder = FormBody.Builder()
        form.hiddenFields.forEach { (key, value) -> builder.add(key, value) }
        builder.add("message", message)
        builder.add("commentsubmit", "true")
        val html = apiClient.postHtml(form.actionUrl, builder.build(), form.actionUrl)
        val document = Jsoup.parse(html, ForumDomainConfig.requireBaseUrl())
        val text = document.text()
        if (
            text.contains("點評成功") ||
            text.contains("点评成功") ||
            text.contains("操作成功")
        ) {
            return
        }
        val messageText = ForumHtmlParser.extractMessage(document).ifBlank { "点评失败" }
        if (messageText.isNotBlank()) {
            error(messageText)
        }
    }

    /**
     * 收藏帖子
     */
    suspend fun favoriteThread(detail: ThreadDetail) {
        val action = detail.favoriteActionUrl ?: error("当前主题不支持收藏")
        apiClient.withRequestLock {
            apiClient.ensureForumSession()
            val document = apiClient.getDocument(action, detail.url)
            val text = document.text()
            if (
                text.contains("收藏成功") ||
                text.contains("已收藏") ||
                text.contains("您已收藏")
            ) {
                return@withRequestLock
            }
            error(ForumHtmlParser.extractMessage(document).ifBlank { "收藏失败" })
        }
    }

    /**
     * 获取当前会话
     */
    suspend fun currentSession(): UserSession? {
        return apiClient.withRequestLock {
            apiClient.ensureForumSession()
            val document = apiClient.getDocument("forum.php")
            val name = document.selectFirst(".member-name a")?.text()?.trim().orEmpty()
            name.takeIf { it.isNotBlank() && it != "帳號" }?.let {
                UserSession(
                    username = it,
                    uid = ForumHtmlParser.extractUid(document.selectFirst(".member-name a")?.attr("href").orEmpty())
                )
            }
        }
    }

    /**
     * 登出
     */
    suspend fun logout() {
        apiClient.clearSession()
    }

    /**
     * 加载用户帖子/回复
     */
    suspend fun loadUserThreads(uid: String, type: String = "thread"): UserThreadListPage {
        return apiClient.withRequestLock {
            apiClient.ensureForumSession()
            val path = buildString {
                append("home.php?mod=space&uid=")
                append(uid)
                append("&do=thread&view=me&from=space")
                if (type == "reply") append("&type=reply")
            }
            val document = apiClient.getDocument(path)
            if (document.selectFirst("#messagetext p")?.text()?.contains("請先登錄") == true) {
                error("请先登录后查看用户中心")
            }
            ForumHtmlParser.parseUserThreadListPage(document)
        }
    }

    /**
     * 加载用户帖子/回复分页
     */
    suspend fun loadUserThreadsPage(pageUrl: String): UserThreadListPage {
        return apiClient.withRequestLock {
            apiClient.ensureForumSession()
            ForumHtmlParser.parseUserThreadListPage(apiClient.getDocument(pageUrl))
        }
    }

    /**
     * 加载用户收藏
     */
    suspend fun loadUserFavorites(): UserThreadListPage {
        return apiClient.withRequestLock {
            apiClient.ensureForumSession()
            ForumHtmlParser.parseUserFavoriteListPage(apiClient.getDocument("home.php?mod=space&do=favorite&view=me"))
        }
    }

    /**
     * 加载用户收藏分页
     */
    suspend fun loadUserFavoritesPage(pageUrl: String): UserThreadListPage {
        return apiClient.withRequestLock {
            apiClient.ensureForumSession()
            ForumHtmlParser.parseUserFavoriteListPage(apiClient.getDocument(pageUrl))
        }
    }

    suspend fun deleteUserFavorite(actionUrl: String) {
        apiClient.withRequestLock {
            apiClient.ensureForumSession()
            val document = apiClient.getDocument(actionUrl, apiClient.absoluteUrl("home.php?mod=space&do=favorite&view=me"))
            val initialText = document.text()
            if (isFavoriteDeleteSuccess(initialText)) {
                return@withRequestLock
            }
            val confirmForm = document.selectFirst("form[action*=favorite][action*=delete], form:has(input[name=deletesubmit])")
            if (confirmForm != null) {
                val confirmAction = apiClient.absoluteUrl(confirmForm.attr("action").replace("&amp;", "&"))
                val fields = ForumHtmlParser.extractFields(confirmForm).toMutableMap()
                fields.putIfAbsent("deletesubmit", "true")
                val body = FormBody.Builder().apply {
                    fields.forEach { (key, value) -> add(key, value) }
                }.build()
                val html = apiClient.postHtml(confirmAction, body, apiClient.absoluteUrl(actionUrl))
                val resultDoc = Jsoup.parse(html, ForumDomainConfig.requireBaseUrl())
                val resultText = resultDoc.text()
                if (isFavoriteDeleteSuccess(resultText) || !isFavoriteDeleteFailure(resultText)) {
                    return@withRequestLock
                }
                error(ForumHtmlParser.extractMessage(resultDoc).ifBlank { "删除收藏失败" })
            }
            if (!isFavoriteDeleteFailure(initialText)) {
                return@withRequestLock
            }
            error(ForumHtmlParser.extractMessage(document).ifBlank { "删除收藏失败" })
        }
    }

    private fun isFavoriteDeleteSuccess(text: String): Boolean {
        return text.contains("刪除成功") ||
            text.contains("删除成功") ||
            text.contains("已移除") ||
            text.contains("操作成功") ||
            text.contains("沒有要刪除的收藏") ||
            text.contains("没有要删除的收藏")
    }

    private fun isFavoriteDeleteFailure(text: String): Boolean {
        return text.contains("您需要先登錄") ||
            text.contains("您需要先登录") ||
            text.contains("請先登錄") ||
            text.contains("请先登录") ||
            text.contains("操作失败") ||
            text.contains("錯誤") ||
            text.contains("错误")
    }

    /**
     * 加载用户资料
     */
    suspend fun loadUserProfile(uid: String): UserProfile {
        return apiClient.withRequestLock {
            apiClient.ensureForumSession()
            val document = apiClient.getDocument("home.php?mod=space&uid=$uid&do=profile&from=space")
            if (document.selectFirst("#messagetext p")?.text()?.contains("請先登錄") == true) {
                error("请先登录后查看用户中心")
            }
            ForumHtmlParser.parseUserProfile(uid, document)
        }
    }
}
