package com.mk.server.libhelper

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import net.lingala.zip4j.ZipFile
import top.niunaijun.blackbox.core.env.BEnvironment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.Executors

class DownloadZip(private val context: Context) {
    private external fun PASSJKPAPA(): String
    private val progressDialog = ProgressDialog(context).apply { setCancelable(false) }
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private val ZIP_FILE_NAME = "save.zip"

    fun startDownload(downloadUrl: String) {
        val zipFile = File(context.filesDir, ZIP_FILE_NAME)
        progressDialog.apply {
            setTitle(if (zipFile.exists()) "Updating" else "⚡Online Lib downloading⚡")
            setMessage("Starting download...")
            show()
        }
        executor.execute {
            val success = downloadFile(downloadUrl)
            handler.post {
                progressDialog.setMessage("Finishing...")
                if (success) {
                    val zipPath = zipFile.absolutePath
                    val outputDir = context.filesDir.absolutePath
                    val password = PASSJKPAPA()
                    if (unzipEncrypted(zipPath, outputDir, password)) {
                        moveSoFiles(File(outputDir, "loader"))
                        zipFile.delete()
                        Toast.makeText(context, "Online Lib download successful!✅", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Failed to extract ZIP. Check ZIP and password.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Download failed. Check internet connection.❌", Toast.LENGTH_LONG).show()
                }
                progressDialog.dismiss()
            }
        }
    }

    private fun downloadFile(downloadUrl: String): Boolean {
        val outputZip = File(context.filesDir, ZIP_FILE_NAME)
        return try {
            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()
            val lengthOfFile = connection.contentLength
            url.openStream().use { input ->
                FileOutputStream(outputZip).use { output ->
                    val data = ByteArray(4096)
                    var total = 0
                    var count: Int
                    while (input.read(data).also { count = it } != -1) {
                        total += count
                        val progress = (total * 100) / lengthOfFile
                        handler.post { progressDialog.setMessage("Download: $progress%") }
                        output.write(data, 0, count)
                    }
                }
            }
            outputZip.exists()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun unzipEncrypted(zipPath: String, outputDir: String, password: String): Boolean {
        return try {
            ZipFile(zipPath, password.toCharArray()).use { zipFile ->
                zipFile.extractAll(outputDir)
                setPermissions(File(outputDir))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun moveSoFiles(loaderFolder: File) {
        val outputDir = context.filesDir
        if (!loaderFolder.exists()) loaderFolder.mkdirs()
        outputDir.listFiles { _, name -> name.endsWith(".so") }?.forEach { soFile ->
            try {
                Files.move(soFile.toPath(), File(loaderFolder, soFile.name).toPath(),
                    StandardCopyOption.REPLACE_EXISTING)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setPermissions(fileOrDir: File) {
        if (fileOrDir.isDirectory) {
            fileOrDir.listFiles()?.forEach { setPermissions(it) }
        }
        fileOrDir.apply {
            setExecutable(true, false)
            setReadable(true, false)
            setWritable(true, false)
        }
    }
}

class FileCopyTask(private val activity: Activity) {
    private val progressDialog = ProgressDialog(activity).apply {
        setTitle("Copying Files...")
        setMessage("Preparing...")
        setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        max = 100
        setCancelable(false)
    }
    private var errorMessage: String? = null

    interface CopyCallback {
        fun onCopyCompleted(success: Boolean)
    }

    fun isObbCopied(packageName: String): Boolean {
        val destDir = BEnvironment.getExternalObbDir(packageName)
        return destDir.exists() && destDir.isDirectory && destDir.list()?.isNotEmpty() == true
    }

    fun copyObbFolderAsync(packageName: String, callback: CopyCallback?) {
        if (isObbCopied(packageName)) {
            callback?.onCopyCompleted(true)
            return
        }
        object : AsyncTask<Void, Int, Boolean>() {
            override fun onPreExecute() {
                progressDialog.apply {
                    setMessage("Copying OBB: $packageName")
                    progress = 0
                    show()
                }
            }
            override fun doInBackground(vararg params: Void): Boolean {
                val sourceDir = File("/storage/emulated/0/Android/obb/", packageName)
                val destDir = BEnvironment.getExternalObbDir(packageName)
                if (!sourceDir.exists() || !sourceDir.isDirectory) {
                    errorMessage = "OBB not found!"
                    return false
                }
                if (!destDir.exists() && !destDir.mkdirs()) {
                    errorMessage = "Destination directory creation failed!"
                    return false
                }
                val files = sourceDir.listFiles()
                if (files.isNullOrEmpty()) {
                    errorMessage = "No files found to copy!"
                    return false
                }
                var totalBytes = 0L
                var copiedBytes = 0L
                for (file in files) { totalBytes += file.length() }
                return try {
                    val buffer = ByteArray(8192)
                    for (file in files) {
                        FileInputStream(file).use { inputStream ->
                            FileOutputStream(File(destDir, file.name)).use { outputStream ->
                                var bytesRead: Int
                                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                    outputStream.write(buffer, 0, bytesRead)
                                    copiedBytes += bytesRead
                                    publishProgress(((copiedBytes * 100) / totalBytes).toInt())
                                }
                            }
                        }
                    }
                    true
                } catch (e: IOException) {
                    errorMessage = "Error copying files: ${e.message}"
                    false
                }
            }
            override fun onProgressUpdate(vararg values: Int?) {
                values[0]?.let { progressDialog.progress = it }
            }
            override fun onPostExecute(result: Boolean) {
                progressDialog.dismiss()
                AlertDialog.Builder(activity).apply {
                    if (result) {
                        setTitle("Success").setMessage("Copy Successful!")
                    } else {
                        setTitle("Error").setMessage(errorMessage ?: "Unknown error")
                    }
                    setCancelable(false).setPositiveButton("OK", null).show()
                }
                callback?.onCopyCompleted(result)
            }
        }.execute()
    }

    companion object {
        fun deleteObbFolder(packageName: String): Boolean {
            val obbDestDir = BEnvironment.getExternalObbDir(packageName)
            return deleteDirectory(obbDestDir)
        }
        private fun deleteDirectory(dir: File): Boolean {
            if (dir.isDirectory) {
                dir.listFiles()?.forEach { child -> deleteDirectory(child) }
            }
            return dir.delete()
        }
    }
}