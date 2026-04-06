package com.xxyangyoulin.jbforum

import android.app.Application

class JbForumApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CookiePersistence.init(this)
        LoginPersistence.init(this)
        MessageStatusPersistence.init(this)
        MessageStatusStore.init()
        ForumDomainConfig.init(this)
        LocalImageFavorites.init(this)
        LocalLinkFavorites.init(this)
        ThreadBrowseHistory.init(this)
        ThreadImageCache.init(this)
        AppCacheManager.init(this)
    }
}
