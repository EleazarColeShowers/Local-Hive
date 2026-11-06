package com.eleazar.localhive.domain

import com.eleazar.localhive.domain.model.Report

/** Reports and feedback, sent to the developer. Users can send them but never read them back. */
interface ReportRepository {
    suspend fun submit(report: Report, reporterId: String, estateId: String?, appVersion: String): Result<Unit>
}
