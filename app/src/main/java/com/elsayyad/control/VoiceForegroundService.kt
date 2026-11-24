package com.elsayyad.control

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * User-started foreground microphone service. Android requires a visible notification
 * and restricts when microphone foreground services may be started.
 */
class VoiceForegroundService : Service(), RecognitionListener {
    private var recognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var listeningRequested = false
    private var restartDelayMs = 700L
    private val channelId = "voice_assistant"

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            listeningRequested = false
            stopRecognition()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, notification())
        listeningRequested = true
        startRecognition()
        return START_STICKY
    }

    private fun startRecognition() {
        if (!listeningRequested) return
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition service unavailable")
            scheduleRestart(5000L)
            return
        }
        try {
            if (recognizer == null) {
                recognizer = if (Build.VERSION.SDK_INT >= 31)
                    SpeechRecognizer.createSpeechRecognizer(this)
                else SpeechRecognizer.createSpeechRecognizer(this)
                recognizer?.setRecognitionListener(this)
            }
            val request = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-EG")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            }
            recognizer?.startListening(request)
        } catch (error: Exception) {
            Log.e(TAG, "Could not start speech recognition", error)
            recreateAndRetry()
        }
    }

    private fun scheduleRestart(delay: Long = restartDelayMs) {
        if (!listeningRequested) return
        handler.removeCallbacks(restartRunnable)
        handler.postDelayed(restartRunnable, delay)
    }

    private val restartRunnable = Runnable {
        if (listeningRequested) {
            try { recognizer?.cancel() } catch (_: Exception) {}
            startRecognition()
        }
    }

    private fun recreateAndRetry() {
        stopRecognition()
        scheduleRestart(1200L)
    }

    private fun stopRecognition() {
        handler.removeCallbacks(restartRunnable)
        try { recognizer?.cancel() } catch (_: Exception) {}
        try { recognizer?.destroy() } catch (_: Exception) {}
        recognizer = null
    }

    override fun onResults(results: Bundle?) {
        val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
        if (words.isNotBlank()) {
            restartDelayMs = 700L
            CommandExecutor.execute(applicationContext, words)
        }
        scheduleRestart()
    }

    override fun onError(error: Int) {
        Log.w(TAG, "SpeechRecognizer error=$error")
        restartDelayMs = when (error) {
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> 3500L
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> 8000L
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 1400L
            else -> 900L
        }
        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) recreateAndRetry()
        else scheduleRestart(restartDelayMs)
    }

    override fun onReadyForSpeech(params: Bundle?) { restartDelayMs = 700L }
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun onDestroy() {
        listeningRequested = false
        stopRecognition()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(channelId, "المساعد الصوتي", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, VoiceForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("الصياد كنترول")
            .setContentText("الاستماع الصوتي يعمل — اضغط للإيقاف")
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_media_pause, "إيقاف", stop)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "ElsayyadVoice"
        const val ACTION_START = "com.elsayyad.control.START_VOICE"
        const val ACTION_STOP = "com.elsayyad.control.STOP_VOICE"
        const val NOTIFICATION_ID = 1001
    }
}