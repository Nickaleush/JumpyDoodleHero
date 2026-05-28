package com.businessdoomguy.minigamestarter.core.dispatchers

import kotlinx.coroutines.Dispatchers

class AppDispatchers {
    val main = Dispatchers.Main
    val io = Dispatchers.IO
    val default = Dispatchers.Default
}
