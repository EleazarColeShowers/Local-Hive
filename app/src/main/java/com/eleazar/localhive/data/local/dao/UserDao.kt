package com.eleazar.localhive.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.eleazar.localhive.data.local.entity.UserEntity

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUser(id: String): UserEntity?
}
