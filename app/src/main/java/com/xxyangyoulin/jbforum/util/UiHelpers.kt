package com.xxyangyoulin.jbforum.util

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import com.xxyangyoulin.jbforum.ForumDomainConfig
import com.xxyangyoulin.jbforum.ThreadDetailActivity
import com.xxyangyoulin.jbforum.ThreadLinkRecognizer
import com.xxyangyoulin.jbforum.ThreadSummary
import com.xxyangyoulin.jbforum.ThreadWebViewActivity

private var lastNavigateTime = 0L

fun tryNavigate(action: () -> Unit) {
    val now = System.currentTimeMillis()
    if (now - lastNavigateTime > 500) {
        lastNavigateTime = now
        action()
    }
}

fun readAppVersionName(context: Context): String {
    return runCatching {
        val pm = context.packageManager
        val pi = pm.getPackageInfo(context.packageName, 0)
        pi.versionName.orEmpty().ifBlank { "0.0.0" }
    }.getOrDefault("0.0.0")
}

fun openThreadByPreference(context: Context, thread: ThreadSummary) {
    tryNavigate {
        if (ForumDomainConfig.openThreadInWebDefault() && ForumDomainConfig.baseUrl().isNotBlank()) {
            context.startActivity(
                ThreadWebViewActivity.createIntent(
                    context = context,
                    url = thread.url,
                    title = thread.title.ifBlank { "帖子详情" }
                )
            )
        } else {
            context.startActivity(ThreadDetailActivity.createIntent(context, thread))
        }
    }
}

fun buildHighlightedText(text: String, accentColor: Int): CharSequence {
    val matches = ThreadLinkRecognizer.extractDisplayMatches(text)
    if (matches.isEmpty()) return text
    val spannable = SpannableString(text)
    matches.forEach { range ->
        val start = range.first
        val endExclusive = range.last + 1
        if (start in text.indices && endExclusive <= text.length && endExclusive > start) {
            spannable.setSpan(UnderlineSpan(), start, endExclusive, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(ForegroundColorSpan(accentColor), start, endExclusive, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
    return spannable
}
