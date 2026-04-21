package com.mk.server

import android.app.Application
import android.content.Context
import com.mk.server.utils.FLog
import top.niunaijun.blackbox.core.system.api.MetaActivationManager;
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.app.configuration.ClientConfiguration

class BoxApplication : Application() {

    companion object {
        const val STATUS_BY = "online"
        var gApp: BoxApplication? = null

        init {
            try {
                System.loadLibrary("MCoreEsp")
            } catch (w: UnsatisfiedLinkError) {
                FLog.error(w.message ?: "Unknown error")
            }
        }
    }

    private external fun BoxApp(): String

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        try {
            BlackBoxCore.get().doAttachBaseContext(base, object : ClientConfiguration() {
                override fun getHostPackageName(): String = base.packageName
              //  override fun isHideRoot(): Boolean = true
       //         override fun isHideXposed(): Boolean = true
                override fun isEnableDaemonService(): Boolean = true
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreate() {
        super.onCreate()
        gApp = this
        BlackBoxCore.get().doCreate()
        try {
            MetaActivationManager.activateSdk(BoxApp())
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }
}