package com.xxyangyoulin.jbforum

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class ShortcutEntryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        when (intent.action) {
            ACTION_OPEN_LOCAL_FAVORITES -> {
                startActivity(
                    LocalFavoritesActivity.createIntent(this).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        }
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        const val ACTION_OPEN_LOCAL_FAVORITES = "com.xxyangyoulin.jbforum.action.OPEN_LOCAL_FAVORITES"
    }
}
