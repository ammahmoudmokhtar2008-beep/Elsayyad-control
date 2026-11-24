package com.elsayyad.control

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import java.util.concurrent.Executors
import kotlin.math.abs

/**
 * Camera-based gesture controller.
 * Gesture map:
 * 1 Open_Palm held -> volume up
 * 2 Thumb/ILoveYou pose -> volume down
 * 3 vertical hand movement upward -> scroll up
 * 4 vertical hand movement downward -> scroll down
 * 5 Open_Palm moved sideways -> Home
 * 6 Closed_Fist held -> reopen Elsayyad Control
 */
class GestureActivity : ComponentActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var status: TextView
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var recognizer: GestureRecognizer? = null
    private var lastActionAt = 0L
    private var swipeAnchorY: Float? = null
    private var swipeAnchorX: Float? = null
    private var swipeAnchorAt = 0L
    private var stablePose = ""
    private var stableSince = 0L
    private var poseFired = false

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else showStatus("لازم تسمح بالكاميرا لتشغيل إشارات اليد")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this)
        previewView = PreviewView(this).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
        status = TextView(this).apply {
            text = "وجّه يدك للكاميرا"
            setTextColor(AndroidColor.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xAA07111F.toInt())
        }
        root.addView(previewView, FrameLayout.LayoutParams(-1, -1))
        root.addView(status, FrameLayout.LayoutParams(-1, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP))
        setContentView(root)

        try {
            val options = GestureRecognizer.GestureRecognizerOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath("gesture_recognizer.task").build())
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(1)
                .setResultListener { result, _ -> handleResult(result) }
                .setErrorListener { error -> runOnUiThread { showStatus("خطأ في التعرف على اليد: ${error.message}") } }
                .build()
            recognizer = GestureRecognizer.createFromOptions(this, options)
        } catch (error: Exception) {
            showStatus("تعذر تجهيز محرك الإيماءات: ${error.message}")
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(cameraExecutor) { proxy ->
                    try {
                        val bitmap = proxy.toBitmap()
                        val mpImage = BitmapImageBuilder(bitmap).build()
                        recognizer?.recognizeAsync(mpImage, SystemClock.uptimeMillis())
                    } catch (_: Exception) {
                    } finally {
                        proxy.close()
                    }
                }
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
                showStatus("الإيماءات نشطة • ارفع الإبهام أو حرّك يدك")
            } catch (error: Exception) {
                showStatus("تعذر تشغيل الكاميرا: ${error.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun handleResult(result: GestureRecognizerResult) {
        val now = SystemClock.elapsedRealtime()
        val category = result.gestures().firstOrNull()?.firstOrNull()?.categoryName().orEmpty()
        val points = result.landmarks().firstOrNull()
        val wrist = points?.firstOrNull()
        val wristY = wrist?.y()
        val wristX = wrist?.x()

        // Open palm moved sideways is the distinct HOME gesture shown near the end of the supplied clip.
        if (category == "Open_Palm" && wristX != null) {
            val anchorX = swipeAnchorX
            if (anchorX == null || now - swipeAnchorAt > 900L) {
                swipeAnchorX = wristX
                swipeAnchorAt = now
            } else if (now - swipeAnchorAt >= 220L && abs(wristX - anchorX) > 0.16f && now - lastActionAt > 1100L) {
                performGestureAction(5, "حركة الكف — الشاشة الرئيسية")
                swipeAnchorX = wristX
                swipeAnchorAt = now
                return
            }
        } else {
            swipeAnchorX = null
        }

        // Vertical movement is accumulated over time to avoid frame-to-frame camera jitter.
        if (wristY != null && category.isNotBlank() && category != "None") {
            val anchor = swipeAnchorY
            if (anchor == null || now - swipeAnchorAt > 900L) {
                swipeAnchorY = wristY
                swipeAnchorAt = now
            } else if (now - swipeAnchorAt >= 220L && now - lastActionAt > 1100L) {
                val delta = wristY - anchor
                if (abs(delta) > 0.16f) {
                    if (delta < 0) performGestureAction(3, "تحريك لأعلى — تمرير لأعلى")
                    else performGestureAction(4, "تحريك لأسفل — تمرير لأسفل")
                    swipeAnchorY = wristY
                    swipeAnchorAt = now
                    return
                }
            }
        } else {
            swipeAnchorY = null
        }

        if (category != stablePose) {
            stablePose = category
            stableSince = now
            poseFired = false
        }
        if (poseFired || now - stableSince < 700L || now - lastActionAt < 1100L) return

        when (category) {
            "Open_Palm" -> { poseFired = true; performGestureAction(1, "رفع الصوت") }
            "Thumb_Up", "Thumb_Down", "ILoveYou" -> { poseFired = true; performGestureAction(2, "خفض الصوت") }
            "Closed_Fist" -> { poseFired = true; performGestureAction(6, "فتح تطبيق الصياد كنترول") }
        }
    }

    private fun performGestureAction(number: Int, label: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastActionAt < 1100L) return
        lastActionAt = now
        when (number) {
            1 -> CommandExecutor.execute(this, "ارفع الصوت")
            2 -> CommandExecutor.execute(this, "اخفض الصوت")
            3 -> ControlAccessibilityService.instance?.scroll(true)
            4 -> ControlAccessibilityService.instance?.scroll(false)
            5 -> ControlAccessibilityService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
            6 -> {
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                if (intent != null) startActivity(intent)
            }
        }
        runOnUiThread { showStatus("الإشارة $number: $label") }
    }

    private fun showStatus(message: String) {
        if (::status.isInitialized) status.text = message
    }

    override fun onDestroy() {
        try { ProcessCameraProvider.getInstance(this).get().unbindAll() } catch (_: Exception) {}
        recognizer?.close()
        cameraExecutor.shutdown()
        super.onDestroy()
    }
}
