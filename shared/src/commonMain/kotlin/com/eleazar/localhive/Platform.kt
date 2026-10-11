package com.eleazar.localhive

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform