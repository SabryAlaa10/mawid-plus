package com.mawidplus.patient.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class DispatcherProvider {
    val main: CoroutineDispatcher get() = Dispatchers.Main
    val io: CoroutineDispatcher get() = Dispatchers.IO
    val default: CoroutineDispatcher get() = Dispatchers.Default
}
