package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.AppScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: DarkSleepViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.updateNotification()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        setContent {
            val state by viewModel.state.collectAsState()
            
            LaunchedEffect(state.isScreenOff) {
                val window = this@MainActivity.window
                val layoutParams = window.attributes
                if (state.isScreenOff) {
                    layoutParams.screenBrightness = 0.01f // Min brightness
                    window.attributes = layoutParams
                } else {
                    layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    window.attributes = layoutParams
                }
            }

            MyApplicationTheme {
                AppScreen(
                    state = state, 
                    onUserActivity = { viewModel.onUserActivity() }, 
                    onToggleService = { viewModel.toggleService() },
                    onTabSelected = { viewModel.setTab(it) },
                    onThresholdChanged = { viewModel.setInactiveThreshold(it) },
                    onScopeChanged = { viewModel.setSleepScope(it) },
                    onDisableWhileChargingChanged = { viewModel.setDisableWhileCharging(it) }
                )
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        viewModel.onUserActivity()
        return super.dispatchTouchEvent(ev)
    }

    override fun onResume() {
        super.onResume()
        // Force state update to foreground when app resumes
        viewModel.setAppInForeground(true)
        viewModel.onUserActivity()
    }

    override fun onPause() {
        super.onPause()
        viewModel.setAppInForeground(false)
    }
}
