package com.eleazar.localhive.di

import com.eleazar.localhive.data.remote.LocalHiveApi
import com.eleazar.localhive.data.remote.PushTokenSync
import com.eleazar.localhive.data.repository.AppSettingsRepositoryImpl
import com.eleazar.localhive.data.repository.AuthRepositoryImpl
import com.eleazar.localhive.data.repository.EstateRepositoryImpl
import com.eleazar.localhive.data.repository.FinanceRepositoryImpl
import com.eleazar.localhive.data.repository.MessageRepositoryImpl
import com.eleazar.localhive.data.repository.NotificationRepositoryImpl
import com.eleazar.localhive.data.repository.PollRepositoryImpl
import com.eleazar.localhive.data.repository.PostRepositoryImpl
import com.eleazar.localhive.data.repository.StorageRepositoryImpl
import com.eleazar.localhive.data.repository.UserRepositoryImpl
import com.eleazar.localhive.data.repository.UtilityRepositoryImpl
import com.eleazar.localhive.domain.AppSettingsRepository
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.EstateRepository
import com.eleazar.localhive.domain.FinanceRepository
import com.eleazar.localhive.domain.MessageRepository
import com.eleazar.localhive.domain.NotificationRepository
import com.eleazar.localhive.domain.PollRepository
import com.eleazar.localhive.domain.PostRepository
import com.eleazar.localhive.domain.StorageRepository
import com.eleazar.localhive.domain.UserRepository
import com.eleazar.localhive.domain.UtilityRepository
import com.eleazar.localhive.ui.auth.AuthViewModel
import com.eleazar.localhive.ui.auth.ProfileSetUpViewModel
import com.eleazar.localhive.ui.auth.SplashViewModel
import com.eleazar.localhive.ui.blocking.BlockingViewModel
import com.eleazar.localhive.ui.directory.DirectoryViewModel
import com.eleazar.localhive.ui.estate.EstateViewModel
import com.eleazar.localhive.ui.feed.FeedViewModel
import com.eleazar.localhive.ui.finance.FinanceViewModel
import com.eleazar.localhive.ui.main.MembershipViewModel
import com.eleazar.localhive.ui.messages.MessagesViewModel
import com.eleazar.localhive.ui.notifications.NotificationViewModel
import com.eleazar.localhive.ui.polls.PollsViewModel
import com.eleazar.localhive.ui.profile.ProfileViewModel
import com.eleazar.localhive.ui.settings.SettingsViewModel
import com.eleazar.localhive.ui.utilities.UtilitiesViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.firestoreSettings
import dev.gitlive.firebase.firestore.persistentCacheSettings
import dev.gitlive.firebase.messaging.messaging
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.module

/** What each platform supplies: a [com.eleazar.localhive.data.platform.MediaReader] and a [com.eleazar.localhive.data.platform.KeyValueStore]. */
expect val platformModule: Module

/** Room for a few estates' feeds, members and chats; the oldest is dropped beyond this. */
private const val FIRESTORE_CACHE_BYTES = 200L * 1024 * 1024

/**
 * @param apiBaseUrl the Cloudflare Worker that signs uploads and sends pushes; blank runs the
 *   app without either.
 */
fun dataModule(apiBaseUrl: String) = module {
    single { Firebase.auth }
    single {
        Firebase.firestore.apply {
            // Set out explicitly so both platforms keep the same on-disk copy across restarts:
            // screens open from it at once, and a listener that resumes within 30 minutes is only
            // charged for documents that changed.
            settings = firestoreSettings {
                cacheSettings = persistentCacheSettings { sizeBytes = FIRESTORE_CACHE_BYTES }
            }
        }
    }
    single { Firebase.messaging }
    single { LocalHiveApi(get(), LocalHiveApi.defaultClient(), apiBaseUrl) }
    singleOf(::PushTokenSync)

    singleOf(::AuthRepositoryImpl) bind AuthRepository::class
    singleOf(::UserRepositoryImpl) bind UserRepository::class
    singleOf(::EstateRepositoryImpl) bind EstateRepository::class
    singleOf(::PostRepositoryImpl) bind PostRepository::class
    singleOf(::MessageRepositoryImpl) bind MessageRepository::class
    singleOf(::NotificationRepositoryImpl) bind NotificationRepository::class
    singleOf(::StorageRepositoryImpl) bind StorageRepository::class
    singleOf(::FinanceRepositoryImpl) bind FinanceRepository::class
    singleOf(::AppSettingsRepositoryImpl) bind AppSettingsRepository::class
    singleOf(::PollRepositoryImpl) bind PollRepository::class
    singleOf(::UtilityRepositoryImpl) bind UtilityRepository::class
}

val viewModelModule = module {
    viewModelOf(::AuthViewModel)
    viewModelOf(::SplashViewModel)
    viewModelOf(::ProfileSetUpViewModel)
    viewModelOf(::MembershipViewModel)
    viewModelOf(::NotificationViewModel)
    viewModelOf(::BlockingViewModel)
    viewModelOf(::DirectoryViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::EstateViewModel)
    viewModelOf(::FeedViewModel)
    viewModelOf(::MessagesViewModel)
    viewModelOf(::FinanceViewModel)
    viewModelOf(::PollsViewModel)
    viewModelOf(::UtilitiesViewModel)
}

fun initKoin(apiBaseUrl: String, appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(platformModule, dataModule(apiBaseUrl), viewModelModule)
}
