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
        ThemeModePersistence.init(this)
        MetaTubeConfig.init(this)
        MetaTubeImageCache.init(this)
        LocalImageFavorites.init(this)
        LocalLinkFavorites.init(this)
        LocalCodeMetadataStore.init(this)
        ThreadBrowseHistory.init(this)
        ThreadImageCache.init(this)
        AppCacheManager.init(this)
    }
}
