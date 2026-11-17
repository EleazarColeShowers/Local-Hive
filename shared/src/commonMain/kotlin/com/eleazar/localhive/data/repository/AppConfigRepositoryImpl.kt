package com.eleazar.localhive.data.repository

import com.eleazar.localhive.domain.AppConfig
import com.eleazar.localhive.domain.AppConfigRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class AppConfigRepositoryImpl(private val firestore: FirebaseFirestore) : AppConfigRepository {
    override fun observeConfig(): Flow<AppConfig> =
        firestore.collection("config").document("app").snapshots
            .map { doc ->
                if (!doc.exists) AppConfig()
                else AppConfig(
                    minVersion = doc.string("minVersion").orEmpty(),
                    androidUrl = doc.string("androidUrl").orEmpty(),
                    iosUrl = doc.string("iosUrl").orEmpty()
                )
            }
            // Offline or unreadable: never stop someone using the app over it.
            .catch { emit(AppConfig()) }
}
