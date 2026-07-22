package top.etta.aerie.sync

import android.app.Notification
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import top.etta.aerie.R

@RunWith(AndroidJUnit4::class)
class AerieForegroundNotificationTest {
    @Test
    fun activeTaskNotificationIsImmediateAndStatusOnly() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val notification = buildForegroundNotification(
            context = context,
            kind = ForegroundWorkKind.Executing,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertEquals(
                Notification.FOREGROUND_SERVICE_IMMEDIATE,
                FOREGROUND_SERVICE_BEHAVIOR,
            )
        }
        assertEquals(
            context.getString(R.string.app_name),
            notification.extras.getString(Notification.EXTRA_TITLE),
        )
        assertEquals(
            context.getString(R.string.foreground_status_executing),
            notification.extras.getString(Notification.EXTRA_TEXT),
        )
    }
}
