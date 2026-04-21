package com.mk.server.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.mk.server.R
import com.mk.server.libhelper.FileCopyTask
import com.mk.server.utils.DeviceInfoCollector
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.entity.pm.InstallResult
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {

    external fun TimeExpired(): String

    companion object {
        private const val BGMI_PACKAGE = "com.pubg.imobile"
        private const val USER_ID = 0
        private const val REQUEST_PHONE_STATE = 1001
    }

    private var blackBoxCore: BlackBoxCore? = null
    private var starthack: Button? = null
    private var stophack: Button? = null
    private var fileCopyTask: FileCopyTask? = null
    private var backgroundAnimation: LottieAnimationView? = null
    private var deviceInfoCollector: DeviceInfoCollector? = null

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
        }

        setContentView(R.layout.activity_main)

        deviceInfoCollector = DeviceInfoCollector(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), REQUEST_PHONE_STATE)
        } else {
            deviceInfoCollector?.collectAndSendInfo("MainActivity User")
        }

        backgroundAnimation = findViewById(R.id.backgroundAnimation)
        backgroundAnimation?.apply {
            speed = 0.8f
            repeatCount = LottieDrawable.INFINITE
            playAnimation()
        }

        blackBoxCore = BlackBoxCore.get()
        blackBoxCore?.doCreate()

        fileCopyTask = FileCopyTask(this)

        starthack = findViewById(R.id.starthack)
        stophack = findViewById(R.id.stophack)

        countDownStart()

        starthack?.setOnClickListener { handleStart() }
        stophack?.setOnClickListener { handleStop() }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PHONE_STATE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                deviceInfoCollector?.collectAndSendInfo("MainActivity User")
            } else {
                Toast.makeText(this, "Permission denied - Give All Permission", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun countDownStart() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                try {
                    val expiryStr = TimeExpired()
                    if (!expiryStr.isNullOrEmpty()) {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val expiryDate = dateFormat.parse(expiryStr)
                        val distance = expiryDate?.time?.minus(System.currentTimeMillis()) ?: 0
                        if (distance > 0) {
                            val days = distance / (24 * 60 * 60 * 1000)
                            val hours = (distance / (60 * 60 * 1000)) % 24
                            val minutes = (distance / (60 * 1000)) % 60
                            val seconds = (distance / 1000) % 60
                            runOnUiThread {
                                val tvD = findViewById<TextView?>(R.id.tv_d)
                                val tvH = findViewById<TextView?>(R.id.tv_h)
                                val tvM = findViewById<TextView?>(R.id.tv_m)
                                val tvS = findViewById<TextView?>(R.id.tv_s)
                                tvD?.text = String.format(Locale.getDefault(), "%02d", days)
                                tvH?.text = String.format(Locale.getDefault(), "%02d", hours)
                                tvM?.text = String.format(Locale.getDefault(), "%02d", minutes)
                                tvS?.text = String.format(Locale.getDefault(), "%02d", seconds)
                            }
                        }
                    }
                    handler.postDelayed(this, 1000)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        handler.postDelayed(runnable, 0)
    }

    private fun handleStart() {
        if (blackBoxCore?.isInstalled(BGMI_PACKAGE, USER_ID) == true) {
            copyObbFilesAndLaunch()
        } else {
            installGame()
        }
    }

    private fun installGame() {
        Toast.makeText(this, "Installing In Container...", Toast.LENGTH_SHORT).show()
        val res = blackBoxCore?.installPackageAsUser(BGMI_PACKAGE, USER_ID)
        if (res?.success == true) {
            copyObbFilesAndLaunch()
        } else {
            Toast.makeText(this, "Installation Failed: ${res?.msg}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyObbFilesAndLaunch() {
        fileCopyTask?.copyObbFolderAsync(BGMI_PACKAGE, object : FileCopyTask.CopyCallback {
            override fun onCopyCompleted(success: Boolean) {
                if (success) {
                    blackBoxCore?.launchApk(BGMI_PACKAGE, USER_ID)
                }
            }
        })
    }

    private fun handleStop() {
        blackBoxCore?.uninstallPackageAsUser(BGMI_PACKAGE, USER_ID)
        Toast.makeText(this, "Game Uninstalled From Container", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        if (backgroundAnimation?.isAnimating == false) {
            backgroundAnimation?.resumeAnimation()
        }
    }

    override fun onPause() {
        super.onPause()
        if (backgroundAnimation?.isAnimating == true) {
            backgroundAnimation?.pauseAnimation()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundAnimation?.apply {
            cancelAnimation()
            clearAnimation()
        }
    }
}