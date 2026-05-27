package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppState
import com.example.ui.theme.PrimaryBlue

@Composable
fun AppScreen(
    state: AppState,
    onUserActivity: () -> Unit,
    onToggleService: () -> Unit,
    onTabSelected: (String) -> Unit,
    onThresholdChanged: (Int) -> Unit,
    onScopeChanged: (com.example.SleepScope) -> Unit,
    onDisableWhileChargingChanged: (Boolean) -> Unit
) {
    if (state.isScreenOff) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(onPress = { onUserActivity() })
                }
        ) {
            Text(
                text = "模擬螢幕關閉 (點擊任意處喚醒)",
                color = Color.DarkGray,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { Header() },
            bottomBar = {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = state.currentTab == "home",
                        onClick = { onTabSelected("home") },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("主畫面", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    NavigationBarItem(
                        selected = state.currentTab == "settings",
                        onClick = { onTabSelected("settings") },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("設定", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                if (state.currentTab == "home") {
                    HomeContent(state, onToggleService)
                } else {
                    SettingsContent(
                        currentThreshold = state.inactiveThresholdSeconds, 
                        onThresholdChanged = onThresholdChanged,
                        currentScope = state.sleepScope,
                        onScopeChanged = onScopeChanged,
                        disableWhileCharging = state.disableWhileCharging,
                        onDisableWhileChargingChanged = onDisableWhileChargingChanged
                    )
                }
            }
        }
    }
}

@Composable
fun Header() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column {
                Text(
                    text = "Lumina 休眠",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.5).sp
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = alpha))
                    )
                    Text(
                        text = "系統運作中",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
        IconButton(
            onClick = { /* Settings action */ },
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun HomeContent(state: AppState, onToggleService: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DecisionCenter(state)
        QuickActionsCard(state.isServiceActive, onToggleService)

        val chargeConditionMet = !state.disableWhileCharging || !state.isCharging
        val isReadyToSleep = state.lightLevel <= 2f && state.timeInactiveSeconds >= state.inactiveThresholdSeconds && 
            (state.sleepScope == com.example.SleepScope.GLOBAL || !state.isAppInForeground) && 
            state.isServiceActive && chargeConditionMet
        SleepStatusBar(isReadyToSleep)
    }
}

@Composable
fun DecisionCenter(state: AppState) {
    val gradientBrush = rememberRadialGradient()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(40.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(40.dp))
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(gradientBrush)
                .alpha(if (state.lightLevel <= 2f) 1f else 0.3f)
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "決策中心",
                color = PrimaryBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format("%.1f", state.lightLevel),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Light,
                    lineHeight = 64.sp
                )
                Text(
                    text = " 勒克斯",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            
            Text(
                text = if (state.lightLevel <= 2f) "偵測到極暗環境" else "環境光線正常",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SensorWidget(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.DirectionsWalk,
                    iconTint = PrimaryBlue,
                    label = "活動狀態",
                    value = if (state.timeInactiveSeconds < state.inactiveThresholdSeconds) "活動中" else "無",
                    subValue = "已靜止 ${state.timeInactiveSeconds} 秒",
                    subValueColor = if (state.timeInactiveSeconds >= state.inactiveThresholdSeconds) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                SensorWidget(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Bolt,
                    iconTint = MaterialTheme.colorScheme.errorContainer,
                    label = "電源",
                    value = if (state.isCharging) "充電中" else "使用電池",
                    subValue = if (state.isCharging) "系統監聽中" else "放電中",
                    subValueColor = if (state.isCharging) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            LinearProgressBar(progress = (state.timeInactiveSeconds.toFloat() / state.inactiveThresholdSeconds.toFloat()).coerceIn(0f, 1f))
            Text(
                text = "正在分析即時感測器數據...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 12.dp).alpha(0.7f)
            )
        }
    }
}

@Composable
fun rememberRadialGradient(): Brush {
    val color = PrimaryBlue.copy(alpha = 0.15f)
    return Brush.radialGradient(
        colors = listOf(color, Color.Transparent),
        center = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        radius = 800f
    )
}

@Composable
fun SensorWidget(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    subValue: String,
    subValueColor: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = value, color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(text = subValue, color = subValueColor, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun LinearProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(6.dp)
                .clip(CircleShape)
                .background(PrimaryBlue)
        )
    }
}

@Composable
fun QuickActionsCard(isActive: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Text("快速動作", color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.8f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("系統介面", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ButtonWidget(
                    modifier = Modifier.weight(1f),
                    label = "啟動",
                    icon = Icons.Default.PlayArrow,
                    bgColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    iconBgColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    iconColor = if (isActive) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    textColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    onClick = { if (!isActive) onToggle() }
                )
                ButtonWidget(
                    modifier = Modifier.weight(1f),
                    label = "暫停",
                    icon = Icons.Default.Pause,
                    bgColor = if (!isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    iconBgColor = if (!isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    iconColor = if (!isActive) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    textColor = if (!isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    onClick = { if (isActive) onToggle() }
                )
            }
        }
    }
}

@Composable
fun ButtonWidget(
    modifier: Modifier = Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bgColor: Color,
    iconBgColor: Color,
    iconColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Text(text = label, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

@Composable
fun SleepStatusBar(isReady: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("狀態", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
            Text(if (isReady) "正在啟動休眠" else "等待條件達成", color = PrimaryBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        }
        LinearProgressBar(progress = if (isReady) 1f else 0.3f)
        Text(
            text = if (isReady) "螢幕即將關閉" else "等待全暗及靜止狀態...",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally)
        )
        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .width(128.dp)
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun SettingsContent(
    currentThreshold: Int, 
    onThresholdChanged: (Int) -> Unit,
    currentScope: com.example.SleepScope,
    onScopeChanged: (com.example.SleepScope) -> Unit,
    disableWhileCharging: Boolean,
    onDisableWhileChargingChanged: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("設定", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "這是一個設定頁面，可以調整自動休眠的相關參數。", 
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "自動休眠觸發範圍",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onScopeChanged(com.example.SleepScope.GLOBAL) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = currentScope == com.example.SleepScope.GLOBAL,
                        onClick = null,
                        colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = PrimaryBlue)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("全域自動關螢幕 (任何狀態皆生效)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onScopeChanged(com.example.SleepScope.ANDROID_DESKTOP) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = currentScope == com.example.SleepScope.ANDROID_DESKTOP,
                        onClick = null,
                        colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = PrimaryBlue)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("僅在「手機桌面」時執行", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("當 App 處於背景且無動作時觸發", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f), fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "充電時不休眠",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "開啟後，若裝置正在充電則不再自動關閉螢幕",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                androidx.compose.material3.Switch(
                    checked = disableWhileCharging,
                    onCheckedChange = { onDisableWhileChargingChanged(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "自動關閉螢幕秒數",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${currentThreshold} 秒",
                    style = MaterialTheme.typography.headlineMedium,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Slider(
                    value = currentThreshold.toFloat(),
                    onValueChange = { onThresholdChanged(it.toInt()) },
                    valueRange = 1f..60f,
                    steps = 58,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryBlue,
                        activeTrackColor = PrimaryBlue,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        val context = androidx.compose.ui.platform.LocalContext.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "無障礙功能權限 (用於真正進入休眠)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "請開啟「Lumina 休眠」無障礙服務，讓應用程式可以幫您鎖定系統螢幕進入休眠狀態，而不只是顯示黑色畫面。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.Button(
                    onClick = {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("前往系統設定開啟無障礙服務")
                }
            }
        }
    }
}

