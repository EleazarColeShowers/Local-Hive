package com.eleazar.localhive.domain

import com.eleazar.localhive.domain.model.UtilityKind
import com.eleazar.localhive.domain.model.UtilityReport
import com.eleazar.localhive.domain.model.UtilityStatus
import kotlinx.coroutines.flow.Flow

/** "Is there light?": the light as the estate's members report them. */
interface UtilityRepository {
    /** The latest status of each supply anyone has reported on. */
    fun observeStatuses(estateId: String): Flow<Map<UtilityKind, UtilityStatus>>
    /** Newest first. */
    fun observeReports(estateId: String, limit: Long = 100): Flow<List<UtilityReport>>
    /** Records the report and updates the supply's status, keeping [UtilityStatus.since] when it only confirms it. */
    suspend fun report(
        estateId: String,
        kind: UtilityKind,
        isOn: Boolean,
        current: UtilityStatus?,
        userId: String,
        userName: String
    ): Result<Unit>
}
