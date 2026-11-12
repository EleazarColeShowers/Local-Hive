package com.eleazar.localhive.data.repository

import com.eleazar.localhive.domain.SosRepository
import com.eleazar.localhive.domain.model.SosStanding
import com.eleazar.localhive.ui.util.nowMillis
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class SosRepositoryImpl(private val firestore: FirebaseFirestore) : SosRepository {

    private fun estate(estateId: String) = firestore.collection("estates").document(estateId)

    // Admins' settings per member, keyed by user id: { disabled, restoredAt, updatedBy, updatedAt }.
    private fun settings(estateId: String) = estate(estateId).collection("sosMembers")

    // One per emergency marked a false alarm, keyed by the post: { userId, postId, markedBy, markedByName, at }.
    private fun falseAlarms(estateId: String) = estate(estateId).collection("falseAlarms")

    override fun observeStanding(estateId: String, userId: String): Flow<SosStanding> =
        combine(
            settings(estateId).document(userId).snapshots,
            falseAlarms(estateId).where { "userId" equalTo userId }.snapshots
        ) { setting, alarms ->
            standing(setting.takeIf { it.exists }, alarms.documents.mapNotNull { it.long("at") })
        }

    override fun observeStandings(estateId: String): Flow<Map<String, SosStanding>> =
        combine(
            settings(estateId).snapshots.map { snapshot -> snapshot.documents.associateBy { it.id } },
            falseAlarms(estateId).snapshots.map { snapshot ->
                snapshot.documents.groupBy({ it.string("userId").orEmpty() }, { it.long("at") ?: 0L })
            }
        ) { settingsById, alarmsById ->
            (settingsById.keys + alarmsById.keys).filter { it.isNotBlank() }.associateWith { id ->
                standing(settingsById[id], alarmsById[id].orEmpty())
            }
        }

    override suspend fun setDisabled(estateId: String, userId: String, disabled: Boolean, adminId: String): Result<Unit> =
        runCatching {
            settings(estateId).document(userId).set(
                mapOf("disabled" to disabled, "updatedBy" to adminId, "updatedAt" to nowMillis()),
                merge = true
            )
        }

    override suspend fun restoreSiren(estateId: String, userId: String, adminId: String): Result<Unit> = runCatching {
        val now = nowMillis()
        settings(estateId).document(userId).set(
            mapOf("restoredAt" to now, "updatedBy" to adminId, "updatedAt" to now),
            merge = true
        )
    }

    private fun standing(setting: DocumentSnapshot?, falseAlarmTimes: List<Long>) = SosStanding(
        disabled = setting?.boolean("disabled") ?: false,
        restoredAt = setting?.long("restoredAt") ?: 0L,
        falseAlarmTimes = falseAlarmTimes
    )
}
