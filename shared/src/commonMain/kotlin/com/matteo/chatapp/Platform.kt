package com.matteo.chatapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform