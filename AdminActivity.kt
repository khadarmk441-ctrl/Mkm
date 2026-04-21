package com.mk.server.activity

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.mk.server.R
import java.text.SimpleDateFormat
import java.util.*

class AdminActivity : Activity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var devicesListView: ListView
    private lateinit var refreshButton: Button
    private lateinit var blockButton: Button
    private lateinit var unblockButton: Button
    private lateinit var deleteButton: Button
    private lateinit var totalCountText: TextView
    private lateinit var adapter: ArrayAdapter<String>

    private val deviceDisplayList = mutableListOf<String>()
    private val deviceIdList = mutableListOf<String>()
    private val devicesData = mutableListOf<Map<String, Any>>()
    private var selectedPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        db = FirebaseFirestore.getInstance()

        devicesListView = findViewById(R.id.devicesListView)
        refreshButton = findViewById(R.id.refreshButton)
        blockButton = findViewById(R.id.blockButton)
        unblockButton = findViewById(R.id.unblockButton)
        deleteButton = findViewById(R.id.deleteButton)
        totalCountText = findViewById(R.id.totalCountText)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, deviceDisplayList)
        devicesListView.adapter = adapter
        devicesListView.choiceMode = ListView.CHOICE_MODE_SINGLE

        devicesListView.setOnItemClickListener { _, _, position, _ ->
            selectedPosition = position
            showDeviceDetails(position)
        }

        refreshButton.setOnClickListener { loadDevices() }

        blockButton.setOnClickListener {
            if (selectedPosition == -1) {
                Toast.makeText(this, "❌ पहले device select करो!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showBlockConfirmDialog()
        }

        unblockButton.setOnClickListener {
            if (selectedPosition == -1) {
                Toast.makeText(this, "❌ पहले device select करो!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showUnblockConfirmDialog()
        }

        deleteButton.setOnClickListener {
            if (selectedPosition == -1) {
                Toast.makeText(this, "❌ पहले device select करो!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showDeleteConfirmDialog()
        }

        loadDevices()
    }

    private fun loadDevices() {
        db.collection("devices")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    deviceDisplayList.clear()
                    deviceIdList.clear()
                    devicesData.clear()
                    selectedPosition = -1

                    val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())

                    for (document in task.result) {
                        val deviceId = document.id
                        val deviceModel = document.getString("deviceModel") ?: "Unknown"
                        val userName = document.getString("userName")
                        val lastSeen = document.getLong("lastSeen")

                        val isBlocked = document.getBoolean("isBlocked") ?: false
                        val blockStatus = if (isBlocked) "🔴 BLOCKED" else "🟢 ACTIVE"

                        val simCards = document.get("simCards") as? List<Map<String, String>>
                        var phoneNumber = "📞 No SIM"
                        var carrier = ""

                        if (!simCards.isNullOrEmpty()) {
                            val firstSim = simCards[0]
                            val number = firstSim["phoneNumber"]
                            if (!number.isNullOrEmpty()) {
                                phoneNumber = "📞 $number"
                                carrier = firstSim["carrierName"] ?: ""
                            }
                        }

                        val lastSeenStr = lastSeen?.let { sdf.format(Date(it)) } ?: "Never"

                        val display = if (!userName.isNullOrEmpty()) {
                            "$blockStatus\n👤 $userName\n📱 $deviceModel\n$phoneNumber $carrier\n⏱️ $lastSeenStr"
                        } else {
                            "$blockStatus | $deviceModel\n$phoneNumber $carrier\n⏱️ $lastSeenStr"
                        }

                        deviceDisplayList.add(display)
                        deviceIdList.add(deviceId)
                        devicesData.add(document.data)
                    }

                    totalCountText.text = "📱 Total Users: ${deviceDisplayList.size}"
                    adapter.notifyDataSetChanged()

                } else {
                    Toast.makeText(this, "Error loading devices", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showDeviceDetails(position: Int) {
        val device = devicesData[position]
        val deviceId = deviceIdList[position]

        val details = StringBuilder()
        details.append("📱 Device ID: ${deviceId.substring(0, 8)}...\n\n")

        val userName = device["userName"] as? String
        if (!userName.isNullOrEmpty()) {
            details.append("👤 Name: $userName\n\n")
        }

        details.append("📱 Model: ${device["deviceModel"]}\n")
        details.append("🏭 Manufacturer: ${device["deviceManufacturer"]}\n")
        details.append("🤖 Android: ${device["androidVersion"]}\n\n")

        val simCards = device["simCards"] as? List<Map<String, String>>
        if (!simCards.isNullOrEmpty()) {
            details.append("📞 SIM Cards:\n")
            simCards.forEachIndexed { i, sim ->
                val number = sim["phoneNumber"] ?: "Unknown"
                val carrier = sim["carrierName"] ?: ""
                details.append("   SIM ${i + 1}: $number")
                if (carrier.isNotEmpty()) {
                    details.append(" ($carrier)")
                }
                details.append("\n")
            }
        } else {
            details.append("📞 No SIM data\n")
        }

        val isBlocked = device["isBlocked"] as? Boolean ?: false
        if (isBlocked) {
            details.append("\n🚫 BLOCKED")
            val reason = device["blockedReason"] as? String
            if (!reason.isNullOrEmpty()) {
                details.append("\nReason: $reason")
            }
        } else {
            details.append("\n✅ ACTIVE")
        }

        val lastSeen = device["lastSeen"] as? Long
        lastSeen?.let {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            details.append("\n\n⏱️ Last Seen: ${sdf.format(Date(it))}")
        }

        AlertDialog.Builder(this)
            .setTitle("📱 Device Details")
            .setMessage(details.toString())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showBlockConfirmDialog() {
        val deviceInfo = deviceDisplayList[selectedPosition]
        AlertDialog.Builder(this)
            .setTitle("🔴 BLOCK DEVICE?")
            .setMessage("क्या आप इस device को BLOCK करना चाहते हैं?\n\n$deviceInfo")
            .setPositiveButton("हाँ, BLOCK करो") { _, _ ->
                blockDevice(deviceIdList[selectedPosition])
            }
            .setNegativeButton("रद्द करो", null)
            .show()
    }

    private fun showUnblockConfirmDialog() {
        val deviceInfo = deviceDisplayList[selectedPosition]
        AlertDialog.Builder(this)
            .setTitle("🟢 UNBLOCK DEVICE?")
            .setMessage("क्या आप इस device को UNBLOCK करना चाहते हैं?\n\n$deviceInfo")
            .setPositiveButton("हाँ, UNBLOCK करो") { _, _ ->
                unblockDevice(deviceIdList[selectedPosition])
            }
            .setNegativeButton("रद्द करो", null)
            .show()
    }

    private fun showDeleteConfirmDialog() {
        val deviceInfo = deviceDisplayList[selectedPosition]
        AlertDialog.Builder(this)
            .setTitle("🗑️ DELETE DEVICE?")
            .setMessage("क्या आप इस device को **पूरी तरह DELETE** करना चाहते हैं?\n\n$deviceInfo\n\n⚠️ यह device का सारा डेटा Firebase से हट जाएगा!")
            .setPositiveButton("हाँ, DELETE करो") { _, _ ->
                deleteDevice(deviceIdList[selectedPosition])
            }
            .setNegativeButton("रद्द करो", null)
            .show()
    }

    private fun blockDevice(deviceId: String) {
        val updates = mapOf(
            "isBlocked" to true,
            "blockedAt" to System.currentTimeMillis(),
            "blockedReason" to "Admin द्वारा block किया गया"
        )

        db.collection("devices").document(deviceId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Device BLOCKED!", Toast.LENGTH_LONG).show()
                loadDevices()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun unblockDevice(deviceId: String) {
        val updates = mapOf("isBlocked" to false)

        db.collection("devices").document(deviceId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Device UNBLOCKED!", Toast.LENGTH_LONG).show()
                loadDevices()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteDevice(deviceId: String) {
        db.collection("devices").document(deviceId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Device DELETED from Firebase!", Toast.LENGTH_LONG).show()
                loadDevices()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}