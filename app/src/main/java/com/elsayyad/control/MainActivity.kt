package com.elsayyad.control

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

private val Navy = Color(0xFF08111F)
private val Surface = Color(0xFF142238)
private val Mint = Color(0xFF35D7C3)
private val Soft = Color(0xFFA9B8CC)

class MainActivity : ComponentActivity() {
    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVoiceService()
    }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Mint, background = Navy, surface = Surface)) {
                HomeScreen(
                    onVoice = {
                        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) startVoiceService()
                        else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onStop = { stopService(Intent(this, VoiceForegroundService::class.java)) },
                    onAccessibility = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    onGestures = { startActivity(Intent(this, GestureActivity::class.java)) }
                )
            }
        }
    }

    private fun startVoiceService() {
        val intent = Intent(this, VoiceForegroundService::class.java).setAction(VoiceForegroundService.ACTION_START)
        ContextCompat.startForegroundService(this, intent)
    }
}

@Composable
private fun HomeScreen(onVoice: () -> Unit, onStop: () -> Unit, onAccessibility: () -> Unit, onGestures: () -> Unit) {
    var listening by remember { mutableStateOf(false) }
    Surface(modifier = Modifier.fillMaxSize(), color = Navy) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("EL SAYYAD", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("مساعد التحكم الذكي", color = Soft, fontSize = 13.sp)
                }
                Surface(color = Color(0xFF123C42), shape = RoundedCornerShape(30.dp)) {
                    Text("الإصدار 1.1", color = Mint, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 11.sp)
                }
            }
            Text("تحكّم في هاتفك بصوتك", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("افتح التطبيقات والأوامر اليومية بصوتك أو بإشارات يدك.", color = Soft, fontSize = 14.sp)
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF10313B)), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = Mint)
                        Spacer(Modifier.width(9.dp))
                        Text("المساعد الصوتي", color = Mint, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(if (listening) "خدمة الاستماع قيد التشغيل" else "ابدأ الخدمة للاستماع إلى أوامرك", color = Color.White, fontSize = 15.sp)
                    Button(
                        onClick = { if (listening) { onStop(); listening = false } else { onVoice(); listening = true } },
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Navy),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Text(if (listening) "إيقاف الاستماع" else "ابدأ الاستماع", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("مثال: افتح فيسبوك، افتح أي تطبيق، ارفع الصوت، ارجع للرئيسية.", color = Soft, fontSize = 12.sp)
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, tint = Mint)
                        Spacer(Modifier.width(8.dp))
                        Text("إشارات اليد الست", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("الإبهام لأعلى/لأسفل للصوت، تحريك اليد للتمرير، كف مفتوح للرئيسية، وقبضة لفتح التطبيق.", color = Soft, fontSize = 13.sp)
                    Button(onClick = onGestures, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Navy), shape = RoundedCornerShape(16.dp)) {
                        Text("تشغيل التحكم بإشارات اليد", fontWeight = FontWeight.Bold)
                    }
                    Text("تعمل الكاميرا أثناء فتح شاشة الإيماءات فقط، ويظهر معاينة الكاميرا بوضوح.", color = Soft, fontSize = 11.sp)
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("الصلاحيات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("فعّل خدمة إمكانية الوصول لتنفيذ الرجوع والرئيسية والتمرير على مستوى النظام.", color = Soft, fontSize = 13.sp)
                    OutlinedButton(onClick = onAccessibility, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("إعداد إمكانية الوصول")
                    }
                }
            }
            Text("الاستماع الصوتي خدمة أمامية بإشعار دائم. استمرارها يعتمد على إصدار أندرويد وإعدادات البطارية.", color = Soft, fontSize = 11.sp)
        }
    }
}