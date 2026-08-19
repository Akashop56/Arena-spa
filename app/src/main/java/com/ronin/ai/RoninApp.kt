package com.ronin.ai

import android.app.Application
import com.ronin.ai.core.device.CrashGuard
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * RONIN AI — application entry point.
 * Boots the dependency graph (Hilt), installs the global crash guard that
 * records failures into the experience system so RONIN can learn from them.
 */
@HiltAndroidApp
class RoninApp : Application() {

    @Inject
    lateinit var crashGuard: CrashGuard

    override fun onCreate() {
        super.onCreate()
        crashGuard.install()
    }
}
