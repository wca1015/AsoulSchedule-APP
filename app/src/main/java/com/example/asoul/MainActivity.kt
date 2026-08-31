package com.example.asoul

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.asoul.ui.AsoulApp
import com.example.asoul.ui.theme.AsoulTheme

class MainActivity : ComponentActivity() {

    /** 系统日历读写权限（模块3：日程写入系统日历）。 */
    private val calendarPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestCalendarPermissionIfNeeded()
        setContent {
            AsoulTheme {
                AsoulApp()
            }
        }
    }

    private fun requestCalendarPermissionIfNeeded() {
        val permissions = arrayOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
        )
        val needRequest = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needRequest) {
            calendarPermissionLauncher.launch(permissions)
        }
    }
}
