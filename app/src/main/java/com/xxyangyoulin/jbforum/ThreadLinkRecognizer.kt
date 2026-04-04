package com.xxyangyoulin.jbforum

object ThreadLinkRecognizer {
    private val magnetRegex = Regex("""(?i)magnet:\?[^\s"'<>]+""")
    private val ed2kRegex = Regex("""(?i)ed2k://\|file\|[^\s"'<>]+""")
    private val urlRegex = Regex("""(?i)\bhttps?://[^\s"'<>]+""")
    private val cloudLinkRegex = Regex(
        """(?i)\b(?:pan\.baidu\.com/s/[^\s"'<>]+|pan\.quark\.cn/s/[^\s"'<>]+|(?:www\.)?aliyundrive\.com/s/[^\s"'<>]+|(?:www\.)?alipan\.com/s/[^\s"'<>]+|drive\.uc\.cn/s/[^\s"'<>]+|115\.com/s/[^\s"'<>]+|pan\.xunlei\.com/s/[^\s"'<>]+|(?:www\.)?123pan\.com/s/[^\s"'<>]+|share\.weiyun\.com/[^\s"'<>]+|(?:www\.)?lanzou[a-z0-9]*\.com/[^\s"'<>]+)"""
    )
    private val btihRegex = Regex("""(?i)\b[0-9a-f]{40}\b""")
    private val magnetBtihRegex = Regex("""(?i)btih:([0-9a-f]{40})""")
    private val javCodeRegexes = listOf(
        Regex("""(?i)\b[a-z]{2,10}[-_]\d{2,6}(?:[-_](?:c|cd\d+))?\b"""),
        Regex("""(?i)\b\d{3}[a-z]{2,6}[-_]\d{2,5}(?:[-_](?:c|cd\d+))?\b"""),
        Regex("""(?i)\b(?:heyzo|kin8|mywife|getchu|gcolle|pcolle)[-_]\d{3,8}(?:[-_](?:c|cd\d+))?\b"""),
        Regex("""(?i)\b(?:fc2[-_]?ppv[-_]?|fc2[-_])\d{5,8}(?:[-_](?:c|cd\d+))?\b"""),
        Regex("""(?i)\b\d{6}[-_]\d{2,3}\b"""),
        Regex("""(?i)(?<![a-z0-9])n\d{4,6}(?![a-z0-9])"""),
        Regex("""(?i)\b[a-z]{2,10}-av-\d{3,6}\b"""),
        Regex("""(?i)\bheydouga-\d{4}-\d{3}\b"""),
        Regex("""(?i)\bc\d{4}-[a-z]{2}\d{5,8}\b"""),
        Regex("""(?i)\bh\d{4}-[a-z]{2,4}\d{5,8}\b""")
    )
    private val trailingPunctuation = Regex("""[),.;，。；！？!?\]]+$""")

    fun extractDisplayMatches(text: String): List<IntRange> {
        if (text.isBlank()) return emptyList()
        val ranges = mutableListOf<IntRange>()
        collectRanges(urlRegex, text, ranges)
        collectRanges(magnetRegex, text, ranges)
        collectRanges(ed2kRegex, text, ranges)
        collectRanges(cloudLinkRegex, text, ranges)
        collectRanges(btihRegex, text, ranges)
        javCodeRegexes.forEach { regex -> collectRanges(regex, text, ranges) }
        if (ranges.isEmpty()) return emptyList()
        val sorted = ranges.sortedBy { it.first }
        val merged = mutableListOf<IntRange>()
        sorted.forEach { range ->
            val last = merged.lastOrNull()
            if (last == null || range.first > last.last + 1) {
                merged += range
            } else {
                merged[merged.lastIndex] = last.first..maxOf(last.last, range.last)
            }
        }
        return merged
    }

    fun extract(detail: ThreadDetail?): List<DetectedLink> {
        if (detail == null) return emptyList()
        val ordered = linkedMapOf<String, DetectedLink>()
        val magnetBtihSet = linkedSetOf<String>()
        val texts = mutableListOf<String>()
        detail.posts.forEach { post ->
            texts += post.content
            post.contentBlocks.mapNotNullTo(texts) { it.text }
            post.remarks.mapTo(texts) { it.content }
        }
        texts.forEach { text ->
            collect(urlRegex, text, "URL", ordered)
            collect(magnetRegex, text, "磁力", ordered)
            collect(ed2kRegex, text, "ED2K", ordered)
            collect(cloudLinkRegex, text, "网盘", ordered) { raw ->
                if (raw.startsWith("http", ignoreCase = true)) raw else "https://$raw"
            }
            collectJavCodes(text, ordered)
            magnetBtihRegex.findAll(text).forEach { match ->
                magnetBtihSet += match.groupValues[1].lowercase()
            }
            collect(btihRegex, text, "种子ID", ordered) { raw ->
                if (magnetBtihSet.contains(raw.lowercase())) "" else raw
            }
        }
        return ordered.values.toList()
    }

    fun detectTypeForSelection(rawText: String): String? {
        val text = rawText.trim().replace(trailingPunctuation, "")
        if (text.isBlank()) return null
        return when {
            magnetRegex.matches(text) -> "磁力"
            ed2kRegex.matches(text) -> "ED2K"
            cloudLinkRegex.containsMatchIn(text) -> "网盘"
            btihRegex.matches(text) -> "磁力"
            detectJavCode(text)?.let { normalizeJavCode(text) == it } == true -> "番号"
            urlRegex.matches(text) -> "URL"
            else -> null
        }
    }

    private fun collectJavCodes(text: String, out: LinkedHashMap<String, DetectedLink>) {
        javCodeRegexes.forEach { regex ->
            regex.findAll(text).forEach { match ->
                val normalized = normalizeJavCode(match.value) ?: return@forEach
                val key = normalized.lowercase()
                if (!out.containsKey(key)) {
                    out[key] = DetectedLink(value = normalized, type = "番号")
                }
            }
        }
    }

    private fun detectJavCode(rawText: String): String? {
        val text = rawText.trim().replace(trailingPunctuation, "")
        javCodeRegexes.forEach { regex ->
            val match = regex.find(text) ?: return@forEach
            return normalizeJavCode(match.value)
        }
        return null
    }

    private fun normalizeJavCode(raw: String): String? {
        val cleaned = raw.trim().replace(trailingPunctuation, "")
        if (cleaned.isBlank()) return null
        val noExt = cleaned.replace(Regex("""\.[a-z0-9]{2,5}$""", RegexOption.IGNORE_CASE), "")
        return noExt
            .uppercase()
            .replace('_', '-')
            .replace(' ', '-')
            .replace(Regex("""-CD\d+$"""), "")
            .replace(Regex("""-C$"""), "")
            .replace(Regex("-+"), "-")
    }

    private fun collect(
        regex: Regex,
        text: String,
        type: String,
        out: LinkedHashMap<String, DetectedLink>,
        transform: (String) -> String = { it }
    ) {
        regex.findAll(text).forEach { match ->
            val normalized = transform(match.value.trim().replace(trailingPunctuation, ""))
            if (normalized.isNotBlank()) {
                val key = normalized.lowercase()
                if (!out.containsKey(key)) {
                    out[key] = DetectedLink(value = normalized, type = type)
                }
            }
        }
    }

    private fun collectRanges(
        regex: Regex,
        text: String,
        out: MutableList<IntRange>
    ) {
        regex.findAll(text).forEach { match ->
            if (match.range.first >= 0 && match.range.last >= match.range.first) {
                out += match.range
            }
        }
    }
}
