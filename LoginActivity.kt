package com.mk.server.activity

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.Settings
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.mk.server.R
import com.mk.server.libhelper.DownloadZip
import com.mk.server.utils.DeviceInfoCollector
import com.mk.server.utils.FLog
import com.mk.server.utils.Prefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class LoginActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_MANAGE_STORAGE_PERMISSION = 100
        private const val REQUEST_MANAGE_UNKNOWN_APP_SOURCES = 200
        private const val PREFS_NAME = "com.mk.server.prefs"
        private const val PREF_PERMISSIONS_GRANTED = "permissions_granted"

        var USERKEY: String? = null
    }

    init {
        try {
            System.loadLibrary("MCoreEsp")
        } catch (w: UnsatisfiedLinkError) {
            FLog.error(w.message ?: "Unknown error")
        }
    }

    external fun FixCrash(): String
    external fun isAdminKey(userKey: String): Boolean
    external fun Check(context: Context, userKey: String): String

    private lateinit var btnSignIn: Button
    private var loadingDialog: Dialog? = null
    private lateinit var errorTextView: TextView
    private lateinit var backgroundAnimation: LottieAnimationView
    private lateinit var getKeyTextView: TextView
    private lateinit var deviceInfoCollector: DeviceInfoCollector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.activity_login)

        deviceInfoCollector = DeviceInfoCollector(this)

        backgroundAnimation = findViewById(R.id.backgroundAnimation)
        backgroundAnimation.apply {
            speed = 0.8f
            repeatCount = LottieDrawable.INFINITE
            playAnimation()
        }

        getKeyTextView = findViewById(R.id.GetKey)
        getKeyTextView.setOnClickListener {
            try {
                val telegramUsername = "MK_CHEATS"
                val intent = Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://t.me/$telegramUsername"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Telegram app नहीं मिला", Toast.LENGTH_SHORT).show()
            }
        }

        checkAndRequestPermissions()
        initDesign()

        DownloadZip(this).startDownload(FixCrash())
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

    private fun checkAndRequestPermissions() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val permissionsGranted = prefs.getBoolean(PREF_PERMISSIONS_GRANTED, false)

        if (!isStoragePermissionGranted()) {
            requestStoragePermissionDirect()
        } else if (!canRequestPackageInstalls()) {
            requestUnknownAppPermissionsDirect()
        } else {
            prefs.edit().putBoolean(PREF_PERMISSIONS_GRANTED, true).apply()
        }
    }

    private fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermissionDirect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.fromParts("package", packageName, null)
            startActivityForResult(intent, REQUEST_MANAGE_STORAGE_PERMISSION)
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_MANAGE_STORAGE_PERMISSION)
        }
    }

    private fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            packageManager.canRequestPackageInstalls()
        } else true
    }

    private fun requestUnknownAppPermissionsDirect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQUEST_MANAGE_UNKNOWN_APP_SOURCES)
        }
    }

    private fun initDesign() {
        val prefs = Prefs(this)
        val textUsername = findViewById<TextView>(R.id.userkey)
        textUsername.setText(prefs.getSt("USER", ""))

        btnSignIn = findViewById(R.id.login)
        btnSignIn.setOnClickListener {
            val userKey = textUsername.text.toString().trim()
            if (userKey.isNotEmpty()) {
                prefs.setSt("USER", userKey)
                USERKEY = userKey
                login(userKey)
            } else {
                textUsername.error = "Please Enter Your License"
            }
        }

        val paste = findViewById<ImageView>(R.id.paste)
        paste.setOnClickListener {
            val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboardManager.primaryClip != null && clipboardManager.primaryClip!!.itemCount > 0) {
                val pastedText = clipboardManager.primaryClip!!.getItemAt(0).text.toString()
                if (pastedText.length > 5) {
                    textUsername.setText(pastedText)
                } else {
                    Toast.makeText(this, "Invalid key in clipboard.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No text in clipboard.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoadingDialog(errorMessage: String?) {
        if (loadingDialog == null) {
            loadingDialog = Dialog(this).apply {
                setContentView(R.layout.ios_loading)
                setCancelable(false)
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                errorTextView = findViewById(R.id.errorText)
            }
        }

        val progressBar = loadingDialog!!.findViewById<ProgressBar>(R.id.progressBar)
        val errorIcon = loadingDialog!!.findViewById<ImageView>(R.id.errorIcon)
        val okButton = loadingDialog!!.findViewById<Button>(R.id.okButton)

        if (!errorMessage.isNullOrEmpty()) {
            progressBar.visibility = ProgressBar.GONE
            errorIcon.visibility = ImageView.VISIBLE
            errorTextView.visibility = TextView.VISIBLE
            errorTextView.text = "ERROR: $errorMessage"

            okButton.visibility = Button.VISIBLE
            okButton.setOnClickListener { dismissLoadingDialog() }
        } else {
            progressBar.visibility = ProgressBar.VISIBLE
            errorIcon.visibility = ImageView.GONE
            errorTextView.visibility = TextView.GONE
            okButton.visibility = Button.GONE
        }

        loadingDialog?.show()
    }

    private fun dismissLoadingDialog() {
        if (loadingDialog?.isShowing == true) {
            loadingDialog?.dismiss()
        }
    }

    private fun login(userKey: String) {
        showLoadingDialog(null)

        deviceInfoCollector.checkIfBlocked(object : DeviceInfoCollector.BlockCheckCallback {
            override fun onBlocked(reason: String?, blockedAt: Long?) {
                runOnUiThread {
                    dismissLoadingDialog()
                    AlertDialog.Builder(this@LoginActivity)
                        .setTitle("📵 DEVICE BLOCKED 📵")
                        .setMessage("YOUR DEVICE IS TEMPORARY BLOCKED!\n\n" +
                                "REASON: OWNER~@azad8058\n\n" +
                                "CONNECT (AZAD) IF YOU DO ANY ILLEGAL ACTIVITY\n" +
                                "WITH LOADER..GIVE ALL PERMISSION TO LOADER")
                        .setCancelable(false)
                        .setPositiveButton("EXIT") { _, _ -> finish() }
                        .show()
                }
            }

            override fun onAllowed() {
                proceedWithLogin(userKey)
            }
        })
    }

    private fun proceedWithLogin(userKey: String) {
        val loginHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                dismissLoadingDialog()
                if (msg.what == 0) {
                    if (isAdminKey(userKey)) {
                        startActivity(Intent(applicationContext, AdminActivity::class.java))
                        Toast.makeText(this@LoginActivity, "Welcome Admin! 👑", Toast.LENGTH_SHORT).show()
                    } else {
                        deviceInfoCollector.collectAndSendInfo(userKey)
                        startActivity(Intent(applicationContext, MainActivity::class.java))
                        Toast.makeText(this@LoginActivity, "Login Success✅", Toast.LENGTH_SHORT).show()
                    }
                    finish()
                } else if (msg.what == 1) {
                    showLoadingDialog(msg.obj as String)
                }
            }
        }

        Thread {
            val result = Check(this@LoginActivity, userKey)
            if (result == "OK") {
                loginHandler.sendEmptyMessage(0)
            } else {
                val msg = Message.obtain().apply {
                    what = 1
                    obj = result
                }
                loginHandler.sendMessage(msg)
            }
        }.start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_MANAGE_STORAGE_PERMISSION) {
            checkAndRequestPermissions()
        } else if (requestCode == REQUEST_MANAGE_UNKNOWN_APP_SOURCES) {
            if (canRequestPackageInstalls()) {
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!backgroundAnimation.isAnimating) {
            backgroundAnimation.resumeAnimation()
        }
    }

    override fun onPause() {
        super.onPause()
        if (backgroundAnimation.isAnimating) {
            backgroundAnimation.pauseAnimation()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundAnimation.apply {
            cancelAnimation()
            clearAnimation()
        }
    }
}