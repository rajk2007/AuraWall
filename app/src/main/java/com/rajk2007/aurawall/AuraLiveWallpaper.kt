package com.rajk2007.aurawall

import android.service.wallpaper.WallpaperService

class AuraLiveWallpaper : WallpaperService() {
    override fun onCreateEngine(): Engine {
        return AuraWallpaperEngine(this)
    }
}
