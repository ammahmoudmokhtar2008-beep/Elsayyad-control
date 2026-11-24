package com.elsayyad.control

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.Toast
import android.content.pm.PackageManager

/**
 * Arabic command router. It resolves installed launcher apps by their visible labels,
 * rather than maintaining a hard-coded list of package names.
 */
object CommandExecutor {
    fun execute(context: Context, raw: String) {
        val text = normalize(raw)
        val service = ControlAccessibilityService.instance
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        when {
            containsAny(text, "ارفع الصوت", "علي الصوت", "زود الصوت", "اعلى الصوت", "الصوت للاخر") ->
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            containsAny(text, "اخفض الصوت", "وطي الصوت", "قلل الصوت", "نزل الصوت") ->
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            containsAny(text, "اكتم الصوت", "اقفل الصوت", "اسكت الصوت") ->
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
            containsAny(text, "الغاء الكتم", "شغل الصوت") ->
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI)
            containsAny(text, "الشاشه الرئيسيه", "الشاشة الرئيسية", "روح للرئيسيه", "افتح الرئيسيه", "الرئيسيه") ->
                global(service, android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME, context)
            containsAny(text, "ارجع", "للخلف", "رجوع") ->
                global(service, android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK, context)
            containsAny(text, "التطبيقات المفتوحه", "التطبيقات الأخيرة", "التطبيقات الاخيره", "اعرض التطبيقات") ->
                global(service, android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS, context)
            containsAny(text, "الاشعارات", "افتح الاشعارات") ->
                global(service, android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS, context)
            containsAny(text, "الاعدادات السريعه", "لوحه الاختصارات") ->
                global(service, android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS, context)
            containsAny(text, "اقفل الشاشه", "اطفي الشاشه", "قفل الهاتف") ->
                global(service, android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN, context)
            containsAny(text, "افتح الاعدادات", "افتح الضبط") ->
                launch(context, Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            containsAny(text, "افتح الكاميرا", "شغل الكاميرا") ->
                launch(context, Intent("android.media.action.IMAGE_CAPTURE").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            containsAny(text, "افتح الهاتف", "افتح الاتصال", "افتح المكالمات") ->
                launch(context, Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            containsAny(text, "افتح الرسائل", "افتح المسجات") ->
                launch(context, Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            containsAny(text, "افتح المتصفح", "افتح الانترنت", "افتح جوجل") ->
                launch(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            text.startsWith("اتصل") || text.startsWith("رن على") || text.startsWith("كلم") ->
                openDialerForPhrase(context)
            text.startsWith("ابحث عن") || text.startsWith("دور على") ->
                searchWeb(context, raw.substringAfter("عن", raw.substringAfter("على", "")).trim())
            else -> {
                val appName = extractAppName(text)
                if (appName.isNotBlank() && openInstalledApp(context, appName)) return
                Toast.makeText(context, "مش قادر أحدد الأمر: $raw. جرّب قول: افتح ثم اسم التطبيق.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun extractAppName(text: String): String {
        val prefixes = listOf("من فضلك افتح ", "ممكن تفتح ", "افتحلي ", "افتح ", "شغل ", "شغللي ", "روح على ", "اذهب الى ")
        var value = text
        for (prefix in prefixes) if (value.startsWith(prefix)) { value = value.removePrefix(prefix); break }
        return value.trim()
    }

    private fun openInstalledApp(context: Context, spokenName: String): Boolean {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val matches = pm.queryIntentActivities(launcher, PackageManager.MATCH_ALL)
        val target = normalize(spokenName)
        val best = matches.mapNotNull { info ->
            val label = normalize(info.loadLabel(pm).toString())
            val score = when {
                label == target -> 3
                label.contains(target) || target.contains(label) -> 2
                else -> 0
            }
            if (score > 0) info to score else null
        }.maxByOrNull { it.second }?.first ?: return false

        val launchIntent = pm.getLaunchIntentForPackage(best.activityInfo.packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try { context.startActivity(launchIntent); true } catch (_: Exception) { false }
    }

    private fun openDialerForPhrase(context: Context) {
        // Opening the dialer is intentional: the user confirms the recipient/call in the system UI.
        launch(context, Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        Toast.makeText(context, "اختر جهة الاتصال وأكد المكالمة من تطبيق الهاتف.", Toast.LENGTH_LONG).show()
    }

    private fun searchWeb(context: Context, query: String) {
        if (query.isBlank()) return
        val uri = Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))
        launch(context, Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun global(service: ControlAccessibilityService?, action: Int, context: Context) {
        if (service?.performGlobalAction(action) != true)
            Toast.makeText(context, "فعّل خدمة إمكانية الوصول من إعدادات التطبيق أولاً.", Toast.LENGTH_SHORT).show()
    }

    private fun launch(context: Context, intent: Intent) {
        try { context.startActivity(intent) }
        catch (_: Exception) { Toast.makeText(context, "لم أجد تطبيقاً مناسباً لتنفيذ الأمر.", Toast.LENGTH_SHORT).show() }
    }

    private fun containsAny(text: String, vararg values: String) = values.any { text.contains(normalize(it)) }

    fun normalize(value: String): String = value.lowercase()
        .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا")
        .replace("ى", "ي").replace("ة", "ه")
        .replace(Regex("[ًٌٍَُِّْـ]"), "")
        .replace(Regex("\\s+"), " ").trim()
}