package com.xxyangyoulin.jbforum

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class ShortcutEntryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    companion object {
        const val ACTION_OPEN_LOCAL_FAVORITES = "com.xxyangyoulin.jbforum.action.OPEN_LOCAL_FAVORITES"
    }
}
