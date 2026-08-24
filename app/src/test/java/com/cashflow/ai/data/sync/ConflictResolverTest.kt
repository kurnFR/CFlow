package com.cashflow.ai.data.sync

import com.cashflow.ai.domain.model.Transaction
import com.cashflow.ai.domain.model.sync.ConflictResolution
import org.junit.Assert.assertEquals
import org.junit.Test

class ConflictResolverTest {

    @Test
    fun resolve_localVersionHigher_returnsLocalWins() {
        val local = Transaction(
            description = "Lunch",
            amount = 50000.0,
            category = "Food & Dining",
            date = "2026-08-21",
            syncVersion = 2,
            updatedAt = 1000L
        )
        val remote = Transaction(
            description = "Lunch Edit",
            amount = 45000.0,
            category = "Food & Dining",
            date = "2026-08-21",
            syncVersion = 1,
            updatedAt = 2000L
        )

        val resolution = ConflictResolver.resolve(local, remote)
        assertEquals(ConflictResolution.LOCAL_WINS, resolution)
    }

    @Test
    fun resolve_remoteVersionHigher_returnsRemoteWins() {
        val local = Transaction(
            description = "Lunch",
            amount = 50000.0,
            category = "Food & Dining",
            date = "2026-08-21",
            syncVersion = 1,
            updatedAt = 1000L
        )
        val remote = Transaction(
            description = "Lunch Edit",
            amount = 45000.0,
            category = "Food & Dining",
            date = "2026-08-21",
            syncVersion = 2,
            updatedAt = 1000L
        )

        val resolution = ConflictResolver.resolve(local, remote)
        assertEquals(ConflictResolution.REMOTE_WINS, resolution)
    }

    @Test
    fun resolve_sameVersionRemoteNewerTimestamp_returnsRemoteWins() {
        val local = Transaction(
            description = "Lunch",
            amount = 50000.0,
            category = "Food & Dining",
            date = "2026-08-21",
            syncVersion = 1,
            updatedAt = 1000L
        )
        val remote = Transaction(
            description = "Lunch Edit",
            amount = 45000.0,
            category = "Food & Dining",
            date = "2026-08-21",
            syncVersion = 1,
            updatedAt = 2500L
        )

        val resolution = ConflictResolver.resolve(local, remote)
        assertEquals(ConflictResolution.REMOTE_WINS, resolution)
    }
}
