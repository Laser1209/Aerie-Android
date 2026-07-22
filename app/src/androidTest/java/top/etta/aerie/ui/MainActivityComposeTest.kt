package top.etta.aerie.ui

import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import top.etta.aerie.MainActivity

@RunWith(AndroidJUnit4::class)
class MainActivityComposeTest {
    @Test
    fun appShellStartsAndDisplaysTheProductName() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val intent = Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        instrumentation.targetContext.startActivity(intent)

        val deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MILLIS
        var productNameVisible = false
        while (!productNameVisible && SystemClock.uptimeMillis() < deadline) {
            productNameVisible = instrumentation.uiAutomation.rootInActiveWindow
                ?.containsText(PRODUCT_NAME) == true
            if (!productNameVisible) SystemClock.sleep(POLL_INTERVAL_MILLIS)
        }
        assertTrue("Aerie product name was not visible after cold start", productNameVisible)
    }

    private fun AccessibilityNodeInfo.containsText(expected: String): Boolean {
        if (text?.toString() == expected || contentDescription?.toString() == expected) return true
        for (index in 0 until childCount) {
            if (getChild(index)?.containsText(expected) == true) return true
        }
        return false
    }

    private companion object {
        const val PRODUCT_NAME = "Aerie 云栖"
        const val UI_TIMEOUT_MILLIS = 10_000L
        const val POLL_INTERVAL_MILLIS = 100L
    }
}
