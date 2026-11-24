package com.elsayyad.control

import android.accessibilityservice.AccessibilityService
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent

class ControlAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() { instance = this }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    fun scroll(directionUp: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < 24) return false
        val metrics = resources.displayMetrics
        val x = metrics.widthPixels * 0.5f
        val startY = if (directionUp) metrics.heightPixels * 0.72f else metrics.heightPixels * 0.28f
        val endY = if (directionUp) metrics.heightPixels * 0.28f else metrics.heightPixels * 0.72f
        val path = Path().apply { moveTo(x, startY); lineTo(x, endY) }
        val gesture = android.accessibilityservice.GestureDescription.Builder()
            .addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 420))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile var instance: ControlAccessibilityService? = null
            private set
    }
}