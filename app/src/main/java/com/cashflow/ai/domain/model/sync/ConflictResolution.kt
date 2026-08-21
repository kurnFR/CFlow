package com.cashflow.ai.domain.model.sync

enum class ConflictResolution {
    LOCAL_WINS,
    REMOTE_WINS,
    MERGE,
    USER_DECISION
}
