package com.eleazar.localhive.data.repository

import com.eleazar.localhive.data.remote.LocalHiveApi
import com.eleazar.localhive.domain.UtilityRepository
import com.eleazar.localhive.domain.model.UtilityKind
import com.eleazar.localhive.domain.model.UtilityReport
import com.eleazar.localhive.domain.model.UtilityStatus
import com.eleazar.localhive.ui.util.nowMillis
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UtilityRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val api: LocalHiveApi
) : UtilityRepository {

    private fun estate(estateId: String) = firestore.collection("estates").document(estateId)
    // One document per supply, with its id the supply's key.
    private fun statuses(estateId: String) = estate(estateId).collection("utilities")
    private fun reports(estateId: String) = estate(estateId).collection("utilityReports")

    override fun observeStatuses(estateId: String): Flow<Map<UtilityKind, UtilityStatus>> =
        statuses(estateId).snapshots.map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                val kind = UtilityKind.fromKey(doc.id) ?: return@mapNotNull null
                val isOn = doc.boolean("isOn") ?: return@mapNotNull null
                kind to UtilityStatus(
                    kind = kind,
                    isOn = isOn,
                    since = doc.long("since") ?: 0L,
                    updatedAt = doc.long("updatedAt") ?: 0L,
                    updatedByName = doc.string("updatedByName").orEmpty()
                )
            }.toMap()
        }

    override fun observeReports(estateId: String, limit: Long): Flow<List<UtilityReport>> =
        reports(estateId)
            .orderBy("at", Direction.DESCENDING)
            .limit(limit)
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    UtilityReport(
                        id = doc.id,
                        kind = UtilityKind.fromKey(doc.string("kind")) ?: return@mapNotNull null,
                        isOn = doc.boolean("isOn") ?: return@mapNotNull null,
                        byId = doc.string("by").orEmpty(),
                        byName = doc.string("byName").orEmpty(),
                        at = doc.long("at") ?: 0L
                    )
                }
            }

    override suspend fun report(
        estateId: String,
        kind: UtilityKind,
        isOn: Boolean,
        current: UtilityStatus?,
        userId: String,
        userName: String
    ): Result<Unit> = runCatching {
        val now = nowMillis()
        val since = if (current != null && current.isOn == isOn && current.since > 0L) current.since else now
        firestore.batch()
            .set(
                reports(estateId).document,
                mapOf("kind" to kind.key, "isOn" to isOn, "by" to userId, "byName" to userName, "at" to now)
            )
            .set(
                statuses(estateId).document(kind.key),
                mapOf(
                    "isOn" to isOn, "since" to since, "updatedAt" to now,
                    "updatedBy" to userId, "updatedByName" to userName
                )
            )
            .commit()
        // Only the change from off to on is news; the Worker checks it and sends at most one an hour.
        if (kind == UtilityKind.POWER && isOn && current?.isOn == false) {
            api.notifyInBackground("type" to "light_back", "estateId" to estateId)
        }
    }
}
