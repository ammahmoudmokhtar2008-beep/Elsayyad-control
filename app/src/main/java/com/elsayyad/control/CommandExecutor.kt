package com.elsayyad.control

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

object CommandExecutor {
    fun execute(context: Context, raw: String) {
        val text = raw.lowercase().replace("أ", "ا").replace("إ", "ا").replace("آ", "ا").trim()
        val accessibility = ControlAccessibilityService.instance
        when {
            text.contains("ارفع الصوت") || text.contains("علي الصوت") || text.contains("الصوت للاخر") ->
                (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            text.contains("اخفض الصوت") || text.contains("وطي الصوت") ->
                (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            text.contains("اكتم الصوت") || text.contains("اقفل الصوت") ->
                (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
            text.contains("ارجع") || text.contains("للخلف") ->
                accessibility?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
                    ?: Toast.makeText(context, "فعّل خدمة إمكانية الوصول أولاً", Toast.LENGTH_SHORT).show()
            text.contains("الشاشة الرئيسية") || text == "الرئيسية" ->
                accessibility?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
                    ?: Toast.makeText(context, "فعّل خدمة إمكانية الوصول أولاً", Toast.LENGTH_SHORT).show()
            text.contains("افتح الاعدادات") ->
                launch(context, Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            text.contains("افتح يوتيوب") -> openPackage(context, "com.google.android.youtube")
            text.contains("افتح واتساب") -> openPackage(context, "com.whatsapp")
            text.startsWith("اتصل") || text.startsWith("رن على") -> {
                val number = text.substringAfter("بـ", "").substringAfter("على", "").trim()
                if (number.matches(Regex("[+0-9 ]{5,}"))) {
                    launch(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } else {
                    Toast.makeText(context, "قل: اتصل ثم رقم الهاتف، أو اختر جهة الاتصال من تطبيق الهاتف", Toast.LENGTH_LONG).show()
                    launch(context, Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            else -> Toast.makeText(context, "لم أفهم الأمر: $raw", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPackage(context: Context, packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) Toast.makeText(context, "التطبيق غير مثبت", Toast.LENGTH_SHORT).show()
        else launch(context, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    private fun launch(context: Context, intent: Intent) {
        try { context.startActivity(intent) } catch (_: Exception) { Toast.makeText(context, "تعذر تنفيذ الأمر على هذا الجهاز", Toast.LENGTH_SHORT).show() }
    }
}
