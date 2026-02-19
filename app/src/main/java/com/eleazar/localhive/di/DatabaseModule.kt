package com.eleazar.localhive.di

import android.content.Context
import androidx.room.Room
import com.eleazar.localhive.data.local.dao.UserDao
import com.eleazar.localhive.data.local.database.LocalHiveDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): LocalHiveDatabase {
        return Room.databaseBuilder(
            context,
            LocalHiveDatabase::class.java,
            "localhive_db"
        ).build()
    }

    @Provides
    fun provideUserDao(db: LocalHiveDatabase): UserDao {
        return db.userDao()
    }
}
