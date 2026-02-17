package com.eleazar.localhive.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.eleazar.localhive.data.local.dao.UserDao
import com.eleazar.localhive.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class],
    version = 1
)
abstract class LocalHiveDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
}
