package com.eleazar.localhive.di

import com.eleazar.localhive.data.platform.AndroidMediaReader
import com.eleazar.localhive.data.platform.KeyValueStore
import com.eleazar.localhive.data.platform.MediaReader
import com.eleazar.localhive.data.platform.SharedPreferencesStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<MediaReader> { AndroidMediaReader(androidContext()) }
    single<KeyValueStore> { SharedPreferencesStore(androidContext()) }
}
