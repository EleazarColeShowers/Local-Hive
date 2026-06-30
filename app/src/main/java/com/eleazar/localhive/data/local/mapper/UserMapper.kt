package com.eleazar.localhive.data.local.mapper

import com.eleazar.localhive.data.local.entity.UserEntity
import com.eleazar.localhive.domain.model.User

fun UserEntity.toDomain(): User = User(
    id = id, email = email, displayName = displayName,
    username = username, profileImageUrl = profileImageUrl,
    bio = bio, estateId = estateId,
    address = address, occupation = occupation, phone = phone
)

fun User.toEntity(): UserEntity = UserEntity(
    id = id, email = email, displayName = displayName,
    username = username, profileImageUrl = profileImageUrl,
    bio = bio, estateId = estateId,
    address = address, occupation = occupation, phone = phone
)
