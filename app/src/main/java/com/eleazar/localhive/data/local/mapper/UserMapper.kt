package com.eleazar.localhive.data.local.mapper

import com.eleazar.localhive.data.local.entity.UserEntity
import com.eleazar.localhive.domain.model.User

fun UserEntity.toDomain(): User {
    return User(
        id = id,
        email = email,
        displayName = displayName
    )
}

fun User.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        email = email,
        displayName = displayName
    )
}
