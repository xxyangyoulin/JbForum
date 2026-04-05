package com.xxyangyoulin.jbforum

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * 论坛 HTML 解析器
 * 负责解析论坛返回的 HTML 页面
 */
object ForumHtmlParser {

    fun parseMessageStatus(document: Document): ForumMessageStatus? {
        val uid = Regex("""discuz_uid\s*=\s*'(\d+)'""")
            .find(document.html())
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()
        val loggedIn = uid.isNotBlank() && uid != "0"
        if (!loggedIn) {
            return ForumMessageStatus()
        }
        val noticeUrl = document.selectFirst("#nte_menu, #myprompt_menu a[href*=do=notice]")
            ?.attr("href")
            ?.let { absoluteUrl(it, document) }
            .orEmpty()
        val messageUrl = document.selectFirst("#msg_menu, #pm_ntc")
            ?.attr("href")
            ?.let { absoluteUrl(it, document) }
            .orEmpty()
        val unreadCount = document.selectFirst("#nte_menu i")
            ?.text()
            ?.trim()
            ?.toIntOrNull()
            ?: document.select("#myprompt_menu span.rq")
                .sumOf { it.text().trim().toIntOrNull() ?: 0 }
        val hasUnread = unreadCount > 0 || document.selectFirst("#pm_ntc.new, #myprompt.new") != null
        return ForumMessageStatus(
            loggedIn = true,
            unreadCount = unreadCount,
            hasUnread = hasUnread,
            noticeUrl = noticeUrl,
            messageUrl = messageUrl
        )
    }

    fun parseNoticeItems(document: Document): List<ForumNoticeItem> {
        return document.select(".nts dl[notice]").mapNotNull { item ->
            val body = item.selectFirst(".ntc_body") ?: return@mapNotNull null
            val authorLink = body.selectFirst("a[href*=home.php?mod=space][href*=uid=]")
            val titleLink = body.select("a[href]").firstOrNull { link ->
                val href = link.attr("href")
                href.isNotBlank() &&
                    !href.contains("home.php?mod=space") &&
                    link.text().trim().isNotBlank() &&
                    link.text().trim() != "查看"
            } ?: return@mapNotNull null
            val targetLink = body.select("a[href]").lastOrNull { link ->
                val href = link.attr("href")
                href.isNotBlank() &&
                    !href.contains("home.php?mod=space") &&
                    link.text().trim().isNotBlank()
            } ?: return@mapNotNull null
            ForumNoticeItem(
                id = item.attr("notice").trim(),
                author = authorLink?.text()?.trim().orEmpty(),
                authorUid = extractUid(authorLink?.attr("href").orEmpty()),
                authorAvatarUrl = item.selectFirst(".avt img")?.let(::imageUrl),
                time = item.selectFirst("dt .xg1")?.text()?.replace('\u00A0', ' ')?.trim().orEmpty(),
                content = body.text().replace("查看", "").trim(),
                threadTitle = titleLink.text().trim(),
                targetUrl = absoluteUrl(targetLink.attr("href"), document)
            )
        }
    }

    /**
     * 解析板块列表
     */
    fun parseBoards(document: Document): List<Board> {
        return document.select("table.fl_tb tr").mapNotNull { row ->
            val link = row.selectFirst("td h2 a") ?: return@mapNotNull null
            Board(
                title = link.text().trim(),
                description = row.selectFirst("td p.xg2")?.text()?.trim().orEmpty(),
                url = absoluteUrl(link.attr("href"), document),
                latestThreadTitle = row.selectFirst("td.fl_by .forumlist a")?.text()?.trim().orEmpty(),
                latestThreadUrl = absoluteUrl(
                    row.selectFirst("td.fl_by .forumlist a")?.attr("href").orEmpty(),
                    document
                )
            )
        }
    }

    /**
     * 解析帖子摘要列表
     */
    fun parseThreadSummaries(document: Document): List<ThreadSummary> {
        val forumThreads = document.select("tbody[id^=normalthread_]").mapNotNull { row ->
            val titleLink = row.selectFirst("div.post_infolist_tit a.s[href*=viewthread], div.post_infolist_tit a[href*=viewthread]") ?: return@mapNotNull null
            val authorLink = row.selectFirst("div.post_infolist_other a.xi2, div.post_infolist_other a[href*=space]")
            val author = authorLink?.text()?.trim().orEmpty()
            val authorAvatarUrl = threadAuthorAvatarUrl(row)
            val publishedAt = row.selectFirst("div.post_infolist_other .dateline span[title], div.post_infolist_other .dateline span, div.post_infolist_other .dateline")
                ?.text()
                ?.trim()
                .orEmpty()
            val titleIconUrls = threadTitleIconUrls(row)
            val totalPages = row.select("div.post_infolist_tit .tps a").lastOrNull()?.text()?.trim()?.toIntOrNull()
            val thumbnailUrls = threadPreviewImages(row)
            val viewsText = row.selectFirst("div.post_infolist_other .nums .views")?.text()?.trim().orEmpty()
            val repliesText = row.selectFirst("div.post_infolist_other .nums .reply")?.text()?.trim().orEmpty()
            val lastReplyAuthor = row.selectFirst("div.post_infolist_other span.time.y a[href*=space]")?.text()?.trim().orEmpty()
            val lastReplyTime = row.selectFirst("div.post_infolist_other span.time.y span[title], div.post_infolist_other span.time.y > span:last-child span, div.post_infolist_other span.time.y > span:last-child")
                ?.text()
                ?.trim()
                .orEmpty()
            ThreadSummary(
                id = row.id().removePrefix("normalthread_"),
                title = titleLink.text().trim(),
                author = author,
                authorUid = extractUid(authorLink?.attr("href").orEmpty()),
                authorAvatarUrl = authorAvatarUrl,
                publishedAt = publishedAt,
                titleIconUrls = titleIconUrls,
                totalPages = totalPages,
                url = absoluteUrl(titleLink.attr("href")),
                thumbnailUrls = thumbnailUrls,
                viewsText = viewsText,
                repliesText = repliesText,
                lastReplyAuthor = lastReplyAuthor,
                lastReplyTime = lastReplyTime,
                metaText = ""
            )
        }
        if (forumThreads.isNotEmpty()) return forumThreads
        return document.select("li.pbw").mapNotNull { item ->
            val titleLink = item.selectFirst("h3 a[href*=viewthread][href*=tid=]") ?: return@mapNotNull null
            val authorLink = item.selectFirst("a[href*=home.php?mod=space][href*=uid=]")
            val boardLink = item.selectFirst("a[href*=forumdisplay]")
            val metaLine = item.select("p").getOrNull(2)
            ThreadSummary(
                id = item.id().ifBlank { titleLink.attr("href").substringAfter("tid=").substringBefore('&') },
                title = titleLink.text().trim(),
                author = authorLink?.text()?.trim().orEmpty(),
                authorUid = extractUid(authorLink?.attr("href").orEmpty()),
                url = absoluteUrl(titleLink.attr("href")),
                repliesText = item.selectFirst("p.xg1")?.text()?.trim().orEmpty(),
                metaText = listOf(
                    metaLine?.selectFirst("span")?.text()?.trim(),
                    boardLink?.text()?.trim()
                ).filter { !it.isNullOrBlank() }.joinToString(" · ")
            )
        }
    }

    /**
     * 解析帖子详情
     */
    fun parseThreadDetail(threadUrl: String, document: Document): ThreadDetail {
        val title = document.selectFirst("#thread_subject")?.text()?.trim().orEmpty()
        val header = document.selectFirst(".nthread_other .authi")
        val threadAuthorLink = header?.selectFirst("a.au")
        val threadAuthor = threadAuthorLink?.text()?.trim().orEmpty()
        val threadAuthorUid = extractUid(threadAuthorLink?.attr("href").orEmpty())
        val threadAuthorAvatarUrl = document.selectFirst(".viewthread_authorinfo .avatar .avtm img, .viewthread_authorinfo .avatar img")
            ?.let(::imageUrl)
            ?: document.selectFirst("div[id^=post_]")?.let(::threadDetailAuthorAvatarUrl)
            ?: avatarUrl(document.selectFirst(".nthread_postbox, .nthread_firstpost, .nthread_other") ?: document)

        val posts = document.select("div[id^=post_]").mapIndexedNotNull { index, postBox ->
            val contentNode = postBox.selectFirst("td.t_f[id^=postmessage_], td.t_f") ?: return@mapIndexedNotNull null
            val author = authorForPost(postBox).ifBlank {
                if (index == 0) threadAuthor else ""
            }
            val authorUid = authorUidForPost(postBox).ifBlank {
                if (index == 0) threadAuthorUid else ""
            }
            val contentBlocks = contentBlocks(contentNode)
            PostItem(
                pid = postBox.id().removePrefix("post_"),
                author = author,
                authorUid = authorUid,
                authorAvatarUrl = if (index == 0) {
                    threadAuthorAvatarUrl?.takeIf { it.isNotBlank() }
                } else {
                    avatarUrl(postBox)
                }
                    ?.takeIf { it.isNotBlank() }
                    ?: if (index == 0) threadAuthorAvatarUrl?.takeIf { it.isNotBlank() } else null,
                time = postBox.selectFirst(".authi em[id], .authi span.xg1, .pi .authi em, .pi .authi span.xg1")?.text()?.trim().orEmpty(),
                floor = postBox.selectFirst(".pi strong a, .pi strong")?.text()?.trim().orEmpty(),
                content = contentText(contentNode),
                contentBlocks = contentBlocks,
                imageUrls = contentBlocks.mapNotNull { it.imageUrl },
                remarks = postRemarks(postBox)
            )
        }

        val replyForm = document.selectFirst("#fastpostform")
        val nextPageUrl = document.select(".pgs .pg a.nxt, .pgt .pg a.nxt, .pg a.nxt, .pgt a.nxt")
            .firstOrNull { link ->
                val href = link.attr("href")
                href.isNotBlank() && !href.startsWith("javascript:", ignoreCase = true)
            }
            ?.attr("href")
            ?.takeIf { it.isNotBlank() }
            ?.let { absoluteUrl(it, document) }
            ?.takeIf { it.isNotBlank() }
        val currentPage = document.selectFirst(".pg strong, .pgt strong")?.text()?.trim()?.toIntOrNull()
            ?: threadUrl.substringAfter("page=", "1").substringBefore('&').toIntOrNull()
            ?: 1
        val totalPages = document.select(".pg a, .pgt a")
            .mapNotNull { link ->
                link.attr("href")
                    .substringAfter("page=", "")
                    .substringBefore('&')
                    .toIntOrNull()
            }
            .maxOrNull()
            ?: Regex("共\\s*(\\d+)\\s*頁")
                .find(document.text())
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
            ?: currentPage

        return ThreadDetail(
            title = title,
            author = threadAuthor,
            authorUid = threadAuthorUid,
            publishedAt = header?.select("span.mr10")?.firstOrNull()?.text()?.trim().orEmpty(),
            repliesText = header?.selectFirst("span.y")?.text()?.substringAfter("回復:")?.trim().orEmpty(),
            viewsText = header?.select("span.mr10.y")?.firstOrNull()?.text()?.substringAfter("瀏覽:")?.trim().orEmpty(),
            posts = posts,
            url = threadUrl,
            nextPageUrl = nextPageUrl,
            currentPage = currentPage,
            totalPages = totalPages,
            favoriteCount = document.selectFirst("#favoritenumber")?.text()?.trim().orEmpty(),
            favoriteActionUrl = document.selectFirst("#k_favorite")?.attr("href")?.takeIf { it.isNotBlank() }?.let { absoluteUrl(it, document) },
            replyAction = replyForm?.attr("action")?.let { absoluteUrl(it, document) },
            replyFields = replyForm?.let(::extractFields).orEmpty()
        )
    }

    /**
     * 解析用户帖子列表页
     */
    fun parseUserThreadListPage(document: Document): UserThreadListPage {
        val items = document.select(".tl table tr")
            .mapNotNull { row ->
                val titleLink = row.selectFirst(
                    "th a[href*=goto=findpost][href*=ptid=], th a[href*=forum.php?mod=viewthread][href*=tid=]:not(.xi2)"
                ) ?: return@mapNotNull null
                val title = titleLink.text().trim()
                if (title.isBlank()) return@mapNotNull null
                val summary = buildList {
                    var nextRow = row.nextElementSibling()
                    while (nextRow != null && nextRow.selectFirst("th a[href]") == null) {
                        nextRow.select("td[colspan] a").mapTo(this) { it.text().trim() }
                        nextRow = nextRow.nextElementSibling()
                    }
                }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString("\n")
                UserThreadItem(
                    title = title,
                    url = absoluteUrl(titleLink.attr("href")),
                    board = row.selectFirst("td a[href*=forumdisplay]")?.text()?.trim().orEmpty(),
                    time = row.selectFirst("td.by em span[title], td.by em")?.text()?.trim().orEmpty(),
                    summary = summary
                )
            }
            .distinctBy { it.url }
        val nextPageUrl = document.selectFirst(".pg a.nxt, .pgs .pg a.nxt")
            ?.attr("href")
            ?.takeIf { it.isNotBlank() }
            ?.let { absoluteUrl(it, document) }
        return UserThreadListPage(
            items = items,
            nextPageUrl = nextPageUrl
        )
    }

    /**
     * 解析用户收藏列表页
     */
    fun parseUserFavoriteListPage(document: Document): UserThreadListPage {
        val items = document.select("#favorite_ul li[id^=fav_]").mapNotNull { item ->
            val titleLink = item.selectFirst("a[href*=forum.php?mod=viewthread][href*=tid=]") ?: return@mapNotNull null
            val deleteActionUrl = extractFavoriteDeleteActionUrl(item, document)
            UserThreadItem(
                title = titleLink.text().trim(),
                url = absoluteUrl(titleLink.attr("href")),
                time = item.selectFirst(".xg1 span[title], .xg1")?.text()?.trim().orEmpty(),
                deleteActionUrl = deleteActionUrl
            )
        }
        val nextPageUrl = document.selectFirst(".pg a.nxt, .pgs .pg a.nxt")
            ?.attr("href")
            ?.takeIf { it.isNotBlank() }
            ?.let { absoluteUrl(it, document) }
        return UserThreadListPage(
            items = items,
            nextPageUrl = nextPageUrl
        )
    }

    private fun extractFavoriteDeleteActionUrl(item: Element, document: Document): String? {
        item.selectFirst("a[href*=op=delete]")?.attr("href")
            ?.takeIf { it.isNotBlank() }
            ?.let { return absoluteUrl(it.replace("&amp;", "&"), document) }
        val onclick = item.select("a[onclick]").mapNotNull { it.attr("onclick").takeIf { text -> text.contains("op=delete") } }.firstOrNull()
            ?: return null
        val quotedUrl = Regex("""['"]([^'"]*op=delete[^'"]*)['"]""")
            .find(onclick)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace("&amp;", "&")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return absoluteUrl(quotedUrl, document)
    }

    /**
     * 解析用户资料
     */
    fun parseUserProfile(uid: String, document: Document): UserProfile {
        val username = document.selectFirst("#uhd .mt, .u_profile .mbn")?.ownText()?.trim()
            ?.ifBlank { null }
            ?: document.selectFirst("#uhd .mt")?.text()?.trim().orEmpty()
        return UserProfile(
            username = username,
            uid = document.selectFirst(".xw0")?.text()?.substringAfter("UID:")?.substringBefore(')')?.trim().orEmpty().ifBlank { uid },
            avatarUrl = document.selectFirst("#uhd .avt img, .u_profile .avatar img")?.absUrl("src")?.ifBlank { null },
            stats = document.select("ul.cl.bbda.pb m li, ul.cl.bbda.pbm.mbm li, ul.cl.bbda li a").mapNotNull { element ->
                val text = element.text().trim()
                text.takeIf { it.isNotBlank() }?.let { "统计" to it }
            },
            basics = document.select("ul.pf_l.cl li").mapNotNull { element ->
                val key = element.selectFirst("em")?.text()?.trim().orEmpty()
                val value = element.ownText().trim().ifBlank { element.text().replace(key, "").trim() }
                if (key.isBlank() || value.isBlank()) null else key to value
            },
            activity = document.select("#pbbs li").mapNotNull { element ->
                val key = element.selectFirst("em")?.text()?.trim().orEmpty()
                val value = element.ownText().trim().ifBlank { element.text().replace(key, "").trim() }
                if (key.isBlank() || value.isBlank()) null else key to value
            },
            credits = document.select("#psts li").mapNotNull { element ->
                val key = element.selectFirst("em")?.text()?.trim().orEmpty()
                val value = element.ownText().trim().ifBlank { element.text().replace(key, "").trim() }
                if (key.isBlank() || value.isBlank()) null else key to value
            }
        )
    }

    /**
     * 查找登录表单
     */
    fun findLoginForm(document: Document): Element? {
        return document.selectFirst("form[id^=loginform_]")
            ?: document.selectFirst("form:has(input[name=username]):has(input[name=password])")
            ?: document.select("form").firstOrNull { form ->
                form.selectFirst("input[name=username]") != null &&
                    form.selectFirst("input[name=password]") != null
            }
    }

    /**
     * 解析发帖表单
     */
    fun parseComposeForm(document: Document, url: String): ComposeForm {
        val form = document.selectFirst("#postform") ?: error("找不到发帖表单")
        val fields = extractFields(form).filterKeys { it !in setOf("subject", "message", "typeid") }
        val typeOptions = form.select("#typeid option").map {
            it.attr("value") to it.text().trim()
        }.filter { it.first.isNotBlank() && it.first != "0" }
        return ComposeForm(
            actionUrl = absoluteUrl(form.attr("action").replace("&amp;", "&"), document),
            hiddenFields = fields,
            typeOptions = typeOptions
        )
    }

    fun parseRemarkForm(document: Document, pid: String, targetLabel: String): RemarkForm {
        document.selectFirst("#messagetext p")
            ?.text()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { error(it) }
        val form = document.selectFirst("form[action*='action=comment']")
            ?: document.selectFirst("form:has(textarea[name=message])")
            ?: error("找不到点评表单")
        val action = absoluteUrl(form.attr("action").replace("&amp;", "&"), document)
        val fields = extractFields(form).filterKeys { it != "message" }
        return RemarkForm(
            pid = pid,
            targetLabel = targetLabel,
            actionUrl = action,
            hiddenFields = fields
        )
    }

    /**
     * 提取表单字段
     */
    fun extractFields(form: Element): Map<String, String> {
        val fields = linkedMapOf<String, String>()
        form.select("input[name]").forEach { input ->
            val name = input.attr("name")
            if (name.isBlank()) return@forEach
            if (input.attr("type") == "checkbox" && !input.hasAttr("checked")) return@forEach
            fields[name] = input.attr("value")
        }
        form.select("select[name]").forEach { select ->
            val selected = select.selectFirst("option[selected]") ?: select.selectFirst("option")
            fields[select.attr("name")] = selected?.attr("value").orEmpty()
        }
        form.select("textarea[name]").forEach { textarea ->
            fields.putIfAbsent(textarea.attr("name"), textarea.text())
        }
        return fields
    }

    /**
     * 提取错误消息
     */
    fun extractMessage(document: Document): String {
        return document.selectFirst("#returnmessage, [id^=returnmessage_], #messagetext p")?.text()?.trim().orEmpty()
    }

    /**
     * 提取 UID
     */
    fun extractUid(href: String): String {
        return href.substringAfter("uid=", "").substringBefore('&').trim()
    }

    /**
     * 将相对 URL 转换为绝对 URL
     */
    fun absoluteUrl(pathOrUrl: String, document: Document? = null): String {
        if (pathOrUrl.isBlank()) return ""
        return if (pathOrUrl.startsWith("http")) {
            pathOrUrl
        } else {
            ForumDomainConfig.requireBaseUrl().toHttpUrlOrNull()?.resolve(pathOrUrl)?.toString().orEmpty()
        }
    }

    // ========== 帖子内容解析 ==========

    private fun authorForPost(postBox: Element): String {
        return postBox.selectFirst(
            ".pls .avatar-name a," +
                ".pls .authi a.xw1," +
                ".pls .pi .authi a.xw1," +
                ".authi a.xw1," +
                "a.au"
        )?.text()?.trim().orEmpty()
    }

    private fun authorUidForPost(postBox: Element): String {
        return extractUid(
            postBox.selectFirst(
                ".pls .avatar-name a," +
                    ".pls .authi a.xw1," +
                    ".pls .pi .authi a.xw1," +
                    ".authi a.xw1," +
                    "a.au"
            )?.attr("href").orEmpty()
        )
    }

    private fun avatarUrl(node: Element): String? {
        return node.selectFirst(
            ".avatar img," +
                ".avatar a img," +
                ".post_avatar img," +
                ".psta img," +
                ".postauthor img," +
                ".authi img," +
                ".post_infolist_other img[src*='avatar']," +
                "img[src*='uc_server/avatar.php']," +
                "img[src*='avatar.php']"
        )?.let { image ->
            image.absUrl("src")
                .ifBlank { image.absUrl("data-original") }
                .ifBlank { image.absUrl("data-src") }
                .ifBlank { image.absUrl("file") }
                .ifBlank { image.absUrl("zoomfile") }
                .ifBlank { image.attr("src") }
                .ifBlank { image.attr("data-original") }
                .ifBlank { image.attr("data-src") }
                .ifBlank { image.attr("file") }
                .ifBlank { image.attr("zoomfile") }
                .takeIf { it.isNotBlank() }
        }
    }

    private fun threadAuthorAvatarUrl(row: Element): String? {
        return row.selectFirst(".post_avatar img")?.let { image ->
            image.absUrl("src")
                .ifBlank { image.absUrl("data-original") }
                .ifBlank { image.absUrl("data-src") }
                .ifBlank { image.attr("src") }
                .ifBlank { image.attr("data-original") }
                .ifBlank { image.attr("data-src") }
                .takeIf { it.isNotBlank() }
        }
    }

    private fun threadDetailAuthorAvatarUrl(postBox: Element): String? {
        return postBox.selectFirst("td.pls .avatar .avtm img, td.pls .avatar img, .pls.favatar .avatar .avtm img")?.let(::imageUrl)
    }

    private fun imageUrl(image: Element): String? {
        return image.absUrl("src")
            .ifBlank { image.absUrl("data-original") }
            .ifBlank { image.absUrl("data-src") }
            .ifBlank { image.absUrl("file") }
            .ifBlank { image.absUrl("zoomfile") }
            .ifBlank { image.attr("src") }
            .ifBlank { image.attr("data-original") }
            .ifBlank { image.attr("data-src") }
            .ifBlank { image.attr("file") }
            .ifBlank { image.attr("zoomfile") }
            .takeIf { it.isNotBlank() }
    }

    private fun contentBlocks(node: Element): List<PostContentBlock> {
        val blocks = mutableListOf<PostContentBlock>()
        val textBuffer = StringBuilder()
        var imageIndex = 0

        fun flushText() {
            val text = textBuffer.toString()
                .replace('\u00a0', ' ')
                .replace(Regex("[ \\t]+"), " ")
                .replace(Regex("\\n{3,}"), "\n\n")
                .trim()
            if (text.isNotBlank()) {
                blocks += PostContentBlock(text = text)
            }
            textBuffer.clear()
        }

        fun walk(current: Node) {
            when (current) {
                is TextNode -> textBuffer.append(current.text())
                is Element -> {
                    when (current.tagName()) {
                        "br" -> textBuffer.append('\n')
                        "img" -> {
                            val src = imageUrl(current).orEmpty()
                            if (!src.isBlank() && !isInlineEmotionImage(current, src)) {
                                flushText()
                                blocks += PostContentBlock(imageUrl = src, imageIndex = imageIndex++)
                            }
                        }
                        "style", "script" -> Unit
                        else -> {
                            current.childNodes().forEach(::walk)
                            if (current.tagName() in setOf("p", "div", "table", "tr")) {
                                textBuffer.append('\n')
                            }
                        }
                    }
                }
            }
        }

        node.childNodes().forEach(::walk)
        flushText()
        return blocks
    }

    private fun contentText(node: Element): String {
        val clone = node.clone().apply {
            select("img, script, style").remove()
            select("br").append("\\n")
        }
        return clone.text()
            .replace("\\n", "\n")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private fun postRemarks(postBox: Element): List<PostRemark> {
        return postBox.select("div[id^=comment_].cm .pstl").mapNotNull { item ->
            val contentNode = item.selectFirst(".psti") ?: return@mapNotNull null
            val content = contentNode.clone().apply {
                select(".xg1").remove()
            }.text()
                .replace('\u00a0', ' ')
                .trim()
            PostRemark(
                author = item.select(".psta a")
                    .firstOrNull { it.text().trim().isNotBlank() }
                    ?.text()
                    ?.trim()
                    .orEmpty(),
                authorUid = extractUid(
                    item.select(".psta a")
                        .firstOrNull { it.text().trim().isNotBlank() }
                        ?.attr("href")
                        .orEmpty()
                ),
                authorAvatarUrl = avatarUrl(item.selectFirst(".psta") ?: item),
                time = contentNode.selectFirst(".xg1")?.text()?.replace("發表於", "")?.trim().orEmpty(),
                content = content
            )
        }
    }

    private fun isInlineEmotionImage(image: Element, src: String): Boolean {
        val normalized = src.lowercase()
        if (
            image.hasClass("smilie") ||
            normalized.contains("smiley") ||
            normalized.contains("smilies") ||
            normalized.contains("/smiley/") ||
            normalized.contains("/smilies/") ||
            normalized.contains("emotion") ||
            normalized.contains("emot")
        ) {
            return true
        }
        val width = image.attr("width").toIntOrNull() ?: 0
        val height = image.attr("height").toIntOrNull() ?: 0
        return width in 1..AppConstants.PARSER_EMOTION_IMAGE_MAX_SIZE || height in 1..AppConstants.PARSER_EMOTION_IMAGE_MAX_SIZE
    }

    private fun threadPreviewImages(row: Element): List<String> {
        return row.select("div.post_infolist_tit a[href*=viewthread] img[src]")
            .mapNotNull { image ->
                val src = image.absUrl("src").ifBlank { image.attr("src") }
                if (src.isBlank()) return@mapNotNull null
                if (isStatusIcon(image, src)) return@mapNotNull null
                src
            }
            .distinct()
            .take(AppConstants.PARSER_THREAD_PREVIEW_MAX_IMAGES)
    }

    private fun threadTitleIconUrls(row: Element): List<String> {
        return row.select("div.post_infolist_tit img[src]")
            .mapNotNull { image ->
                val src = image.absUrl("src").ifBlank { image.attr("src") }
                if (src.isBlank()) return@mapNotNull null
                val normalized = src.lowercase()
                if (!normalized.endsWith("/recommend.png") &&
                    !normalized.endsWith("/hot.jpg")
                ) {
                    return@mapNotNull null
                }
                src
            }
            .distinct()
    }

    private fun isStatusIcon(image: Element, src: String): Boolean {
        val normalized = src.lowercase()
        if (
            normalized.contains("/common/") ||
            normalized.contains("icon") ||
            normalized.contains("hot") ||
            normalized.contains("digest") ||
            normalized.contains("rank_") ||
            normalized.contains("folder")
        ) {
            return true
        }
        val width = image.attr("width").toIntOrNull() ?: 0
        val height = image.attr("height").toIntOrNull() ?: 0
        return width in 1..AppConstants.PARSER_STATUS_ICON_MAX_SIZE || height in 1..AppConstants.PARSER_STATUS_ICON_MAX_SIZE
    }
}
