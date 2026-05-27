package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SleepScope { GLOBAL, ANDROID_DESKTOP }

data class AppState(
    val lightLevel: Float = 100f,
    val isCharging: Boolean = false,
    val timeInactiveSeconds: Int = 0,
    val isServiceActive: Boolean = false,
    val isScreenOff: Boolean = false,
    val currentTab: String = "home",
    val inactiveThresholdSeconds: Int = 10,
    val sleepScope: SleepScope = SleepScope.ANDROID_DESKTOP,
    val isAppInForeground: Boolean = true,
    val currentForegroundPackage: String? = null,
    val disableWhileCharging: Boolean = true
)

class DarkSleepViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    private val lightThreshold = 2f // lux (Very dark)

    private var timerJob: Job? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status: Int = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val plugged: Int = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                             status == BatteryManager.BATTERY_STATUS_FULL ||
                             plugged > 0
            _state.update { it.copy(isCharging = isCharging) }
            checkConditions()
        }
    }

    private val lightListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
                val lux = event.values[0]
                _state.update { it.copy(lightLevel = lux) }
                checkConditions()
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val toggleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.ACTION_TOGGLE_SERVICE") {
                toggleService()
            }
        }
    }

    private val foregroundReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.ACTION_FOREGROUND_APP_CHANGED") {
                val pkg = intent.getStringExtra("packageName")
                _state.update { it.copy(currentForegroundPackage = pkg) }
                checkConditions()
            }
        }
    }

    private val notificationManager = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "dark_sleep_channel"

    private val prefs = application.getSharedPreferences("dark_sleep_prefs", Context.MODE_PRIVATE)

    init {
        val savedThreshold = prefs.getInt("inactiveThresholdSeconds", 10)
        val savedScopeName = prefs.getString("sleepScope", SleepScope.ANDROID_DESKTOP.name) ?: SleepScope.ANDROID_DESKTOP.name
        val savedScope = try { SleepScope.valueOf(savedScopeName) } catch (e: Exception) { SleepScope.ANDROID_DESKTOP }
        val savedDisableWhileCharging = prefs.getBoolean("disableWhileCharging", true)
        val savedIsServiceActive = prefs.getBoolean("isServiceActive", false)

        _state.update { 
            it.copy(
                inactiveThresholdSeconds = savedThreshold,
                sleepScope = savedScope,
                disableWhileCharging = savedDisableWhileCharging,
                isServiceActive = savedIsServiceActive
            ) 
        }

        val app = getApplication<Application>()
        app.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        ContextCompat.registerReceiver(
            app,
            toggleReceiver,
            IntentFilter("com.example.ACTION_TOGGLE_SERVICE"),
            ContextCompat.RECEIVER_EXPORTED
        )
        ContextCompat.registerReceiver(
            app,
            foregroundReceiver,
            IntentFilter("com.example.ACTION_FOREGROUND_APP_CHANGED"),
            ContextCompat.RECEIVER_EXPORTED
        )
        lightSensor?.let {
            sensorManager.registerListener(lightListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        createNotificationChannel()
        startInactivityTimer()
        updateNotification()
    }

    override fun onCleared() {
        super.onCleared()
        val app = getApplication<Application>()
        app.unregisterReceiver(batteryReceiver)
        app.unregisterReceiver(toggleReceiver)
        app.unregisterReceiver(foregroundReceiver)
        notificationManager.cancel(1)
        sensorManager.unregisterListener(lightListener)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Lumina Sleep",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Dark Sleep Quick Actions"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun updateNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(getApplication(), android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = Intent("com.example.ACTION_TOGGLE_SERVICE").apply {
            setPackage(getApplication<Application>().packageName)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isScreenOff = _state.value.isScreenOff
        val isActive = _state.value.isServiceActive
        val title = if (isActive) {
            if (isScreenOff) "Lumina 休眠 (已觸發關閉)" else "Lumina 休眠 (運作中)"
        } else {
            "Lumina 休眠 (已暫停)"
        }
        val actionText = if (isActive) "暫停" else "啟動"

        val notification = NotificationCompat.Builder(getApplication(), channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText("輕觸按鈕以切換狀態")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) 
            .addAction(R.mipmap.ic_launcher, actionText, pendingIntent)
            .build()

        notificationManager.notify(1, notification)
    }

    fun onUserActivity() {
        val currentState = _state.value
        if (currentState.timeInactiveSeconds > 0 || currentState.isScreenOff) {
            _state.update { it.copy(timeInactiveSeconds = 0, isScreenOff = false) }
            checkConditions()
            if (currentState.isScreenOff) updateNotification()
        }
    }

    fun toggleService() {
        val newState = !_state.value.isServiceActive
        _state.update { it.copy(isServiceActive = newState, isScreenOff = false) }
        prefs.edit().putBoolean("isServiceActive", newState).apply()
        checkConditions()
        updateNotification()
    }

    fun setTab(tab: String) {
        _state.update { it.copy(currentTab = tab) }
        checkConditions()
    }

    fun setInactiveThreshold(seconds: Int) {
        _state.update { it.copy(inactiveThresholdSeconds = seconds) }
        prefs.edit().putInt("inactiveThresholdSeconds", seconds).apply()
        checkConditions()
    }

    fun setSleepScope(scope: SleepScope) {
        _state.update { it.copy(sleepScope = scope) }
        prefs.edit().putString("sleepScope", scope.name).apply()
        checkConditions()
    }

    fun setAppInForeground(isForeground: Boolean) {
        _state.update { it.copy(isAppInForeground = isForeground) }
        checkConditions()
    }

    fun setDisableWhileCharging(disable: Boolean) {
        _state.update { it.copy(disableWhileCharging = disable) }
        prefs.edit().putBoolean("disableWhileCharging", disable).apply()
        checkConditions()
    }

    private fun startInactivityTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.update { it.copy(timeInactiveSeconds = it.timeInactiveSeconds + 1) }
                checkConditions()
            }
        }
    }

    private fun getLauncherPackages(): List<String> {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfos = getApplication<Application>().packageManager.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_ALL)
        return resolveInfos.map { it.activityInfo.packageName }
    }

    private fun checkConditions() {
        val currentState = _state.value
        
        if (!currentState.isServiceActive) {
            if (currentState.isScreenOff) {
                _state.update { it.copy(isScreenOff = false) }
                updateNotification()
            }
            return
        }

        val isDark = currentState.lightLevel <= lightThreshold
        val isInactive = currentState.timeInactiveSeconds >= currentState.inactiveThresholdSeconds
        val isCharging = currentState.isCharging
        val chargeConditionMet = !currentState.disableWhileCharging || !isCharging

        val conditionsMet = if (currentState.sleepScope == SleepScope.ANDROID_DESKTOP) {
            val launcherPackages = getLauncherPackages()
            // We also treat null/empty as launcher just in case accessibility is delayed 
            // but usually we rely on accessibility. We could explicitly check.
            val isLauncher = currentState.currentForegroundPackage == null || currentState.currentForegroundPackage in launcherPackages
            isDark && isInactive && isLauncher && chargeConditionMet
        } else {
            isDark && isInactive && chargeConditionMet
        }

        if (conditionsMet && !currentState.isScreenOff) {
            _state.update { it.copy(isScreenOff = true) }
            updateNotification()
            
            val lockIntent = Intent("com.example.ACTION_LOCK_SCREEN")
            lockIntent.setPackage(getApplication<Application>().packageName)
            getApplication<Application>().sendBroadcast(lockIntent)
            
        } else if (!conditionsMet && currentState.isScreenOff) {
            _state.update { it.copy(isScreenOff = false) }
            updateNotification()
        }
    }
}
