package top.etta.aerie.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PeriodicSyncSchedulerTest {
    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder()
                .setExecutor(SynchronousExecutor())
                .build(),
        )
        workManager = WorkManager.getInstance(context)
    }

    @After
    fun tearDown() {
        workManager.cancelAllWork().result.get(5, TimeUnit.SECONDS)
        WorkManagerTestInitHelper.closeWorkDatabase()
    }

    @Test
    fun repeatedSchedulingKeepsOnePeriodicStatusJob() {
        val scheduler = AndroidPeriodicSyncScheduler(context)

        scheduler.ensureScheduled()
        scheduler.ensureScheduled()

        val work = uniqueWork()
        assertEquals(1, work.size)
        assertEquals(WorkInfo.State.ENQUEUED, work.single().state)
        assertTrue(work.single().tags.contains(AndroidPeriodicSyncScheduler.WORK_TAG))
        assertEquals(15L, AndroidPeriodicSyncScheduler.PERIOD_MINUTES)
        assertEquals(5L, AndroidPeriodicSyncScheduler.RETRY_BACKOFF_MINUTES)
    }

    @Test
    fun cancelStopsTheUniquePeriodicStatusJob() {
        val scheduler = AndroidPeriodicSyncScheduler(context)
        scheduler.ensureScheduled()

        scheduler.cancel()

        val work = uniqueWork()
        assertEquals(1, work.size)
        assertEquals(WorkInfo.State.CANCELLED, work.single().state)
    }

    private fun uniqueWork(): List<WorkInfo> = workManager
        .getWorkInfosForUniqueWork(AndroidPeriodicSyncScheduler.UNIQUE_WORK_NAME)
        .get(5, TimeUnit.SECONDS)
}
