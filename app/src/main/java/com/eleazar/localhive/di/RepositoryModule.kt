package com.eleazar.localhive.di

import com.eleazar.localhive.data.authentication.AuthRepositoryImpl
import com.eleazar.localhive.data.repository.EstateRepositoryImpl
import com.eleazar.localhive.data.repository.MessageRepositoryImpl
import com.eleazar.localhive.data.repository.PostRepositoryImpl
import com.eleazar.localhive.data.repository.UserRepositoryImpl
import com.eleazar.localhive.domain.AuthRepository
import com.eleazar.localhive.domain.EstateRepository
import com.eleazar.localhive.domain.MessageRepository
import com.eleazar.localhive.domain.PostRepository
import com.eleazar.localhive.domain.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds @Singleton
    abstract fun bindEstateRepository(impl: EstateRepositoryImpl): EstateRepository

    @Binds @Singleton
    abstract fun bindPostRepository(impl: PostRepositoryImpl): PostRepository

    @Binds @Singleton
    abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository
}
