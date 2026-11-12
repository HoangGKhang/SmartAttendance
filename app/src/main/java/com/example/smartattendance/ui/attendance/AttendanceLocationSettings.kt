package com.example.smartattendance.ui.attendance

import android.app.AlertDialog
import android.content.Context
import android.location.LocationManager

internal fun Context.isAttendanceLocationEnabled(): Boolean =
    getSystemService(LocationManager::class.java)?.isLocationEnabled == true

internal fun Context.showEnableAttendanceLocationDialog(
    message: String,
    onEnable: () -> Unit,
    onLater: () -> Unit
) {
    AlertDialog.Builder(this)
        .setTitle("Bật vị trí?")
        .setMessage(message)
        .setPositiveButton("Mở cài đặt") { _, _ -> onEnable() }
        .setNegativeButton("Để sau") { _, _ -> onLater() }
        .setOnCancelListener { onLater() }
        .show()
}