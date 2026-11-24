package com.elsayyad.control

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class ControlAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() { if (instance === this) instance = null; super.onDestroy() }
    companion object { @Volatile var instance: ControlAccessibilityService? = null; private set }
}
