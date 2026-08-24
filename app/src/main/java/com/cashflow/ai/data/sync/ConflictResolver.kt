package com.cashflow.ai.data.sync

import com.cashflow.ai.domain.model.Transaction
import com.cashflow.ai.domain.model.sync.ConflictResolution

object ConflictResolver {

    fun resolve(local: Transaction, remote: Transaction): ConflictResolution {
        return when {
            local.syncVersion > remote.syncVersion -> ConflictResolution.LOCAL_WINS
            remote.syncVersion > local.syncVersion -> ConflictResolution.REMOTE_WINS
            local.updatedAt > remote.updatedAt -> ConflictResolution.LOCAL_WINS
            remote.updatedAt > local.updatedAt -> ConflictResolution.REMOTE_WINS
            else -> ConflictResolution.LOCAL_WINS
        }
    }
}
