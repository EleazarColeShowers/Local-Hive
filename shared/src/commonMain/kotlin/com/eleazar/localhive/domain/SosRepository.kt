package com.eleazar.localhive.domain

import com.eleazar.localhive.domain.model.SosStanding
import kotlinx.coroutines.flow.Flow

/**
 * What an estate's admins have set for each member's SOS alerts, and the false alarms that count
 * against them. Marking a false alarm happens on the post, through [PostRepository.markFalseAlarm].
 */
interface SosRepository {
    /** The user's own standing in their estate. */
    fun observeStanding(estateId: String, userId: String): Flow<SosStanding>

    /** Every member with anything on record, by user id. Admins only. */
    fun observeStandings(estateId: String): Flow<Map<String, SosStanding>>

    /** An admin stops [userId] sending SOS alerts, or lets them again. */
    suspend fun setDisabled(estateId: String, userId: String, disabled: Boolean, adminId: String): Result<Unit>

    /** An admin gives [userId] the siren back: their earlier false alarms stop counting. */
    suspend fun restoreSiren(estateId: String, userId: String, adminId: String): Result<Unit>
}
