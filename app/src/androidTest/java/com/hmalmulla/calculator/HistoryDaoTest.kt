package com.hmalmulla.calculator

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hmalmulla.calculator.data.AppDatabase
import com.hmalmulla.calculator.data.HistoryEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** t19: history is stored in Room and survives a database re-open. */
@RunWith(AndroidJUnit4::class)
class HistoryDaoTest {
    @Test
    fun historyPersists() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        ctx.deleteDatabase("test-history.db")
        var db = Room.databaseBuilder(ctx, AppDatabase::class.java, "test-history.db").build()
        db.history().insert(HistoryEntry(expression = "2+2", result = "4", timestamp = 1L))
        db.close()
        db = Room.databaseBuilder(ctx, AppDatabase::class.java, "test-history.db").build()
        val all = db.history().all().first()
        assertEquals(1, all.size)
        assertEquals("4", all.first().result)
        db.close()
    }
}
