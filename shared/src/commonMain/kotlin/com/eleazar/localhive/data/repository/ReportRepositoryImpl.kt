package com.eleazar.localhive.data.repository

import com.eleazar.localhive.domain.ReportRepository
import com.eleazar.localhive.domain.model.Report
import com.eleazar.localhive.ui.util.nowMillis
import dev.gitlive.firebase.firestore.FirebaseFirestore

/** Longest details and excerpt kept; matches firestore.rules. */
const val MAX_REPORT_DETAILS = 2000
private const val MAX_REPORT_EXCERPT = 1000

class ReportRepositoryImpl(private val firestore: FirebaseFirestore) : ReportRepository {

    override suspend fun submit(report: Report, reporterId: String, estateId: String?, appVersion: String): Result<Unit> =
        runCatching {
            // Read in the Firebase console; "status" is there to mark them dealt with.
            firestore.collection("reports").document.set(
                mapOf(
                    "subject" to report.subject.key,
                    "reason" to report.reason.key,
                    "details" to report.details.trim().take(MAX_REPORT_DETAILS),
                    "targetId" to report.targetId,
                    "postId" to report.postId,
                    "targetUserId" to report.targetUserId,
                    "excerpt" to report.excerpt?.take(MAX_REPORT_EXCERPT),
                    "reporterId" to reporterId,
                    "estateId" to estateId,
                    "appVersion" to appVersion.take(40),
                    "status" to "open",
                    "createdAt" to nowMillis()
                )
            )
        }
}
