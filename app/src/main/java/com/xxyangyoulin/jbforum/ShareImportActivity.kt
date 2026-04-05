package com.xxyangyoulin.jbforum

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ShareImportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        val uris = extractImageUris(intent)
        if (uris.isEmpty()) {
            Toast.makeText(this, "未收到可导入的图片", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        lifecycleScope.launch {
            val imported = uris.mapNotNull { uri ->
                runCatching {
                    LocalImageFavorites.add(
                        context = this@ShareImportActivity,
                        client = ForumRepository().imageClient(),
                        imageRef = uri.toString()
                    )
                }.getOrNull()
            }
            val message = when {
                imported.isEmpty() -> "导入失败"
                else -> "加入收藏成功"
            }
            Toast.makeText(this@ShareImportActivity, message, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    private fun extractImageUris(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                listOfNotNull(intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
            }
            else -> emptyList()
        }
    }
}
