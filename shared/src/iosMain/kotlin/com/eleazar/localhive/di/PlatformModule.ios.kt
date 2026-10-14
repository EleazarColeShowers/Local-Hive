package com.eleazar.localhive.di

import com.eleazar.localhive.data.platform.IosMediaReader
import com.eleazar.localhive.data.platform.KeyValueStore
import com.eleazar.localhive.data.platform.MediaReader
import com.eleazar.localhive.data.platform.UserDefaultsStore
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<MediaReader> { IosMediaReader() }
    single<KeyValueStore> { UserDefaultsStore() }
}
