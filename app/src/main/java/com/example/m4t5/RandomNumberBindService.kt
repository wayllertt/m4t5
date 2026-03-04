package com.example.m4t5

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class RandomNumberBindService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private val _randomNumber = MutableStateFlow(0)
    val randomNumber: StateFlow<Int> = _randomNumber.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            while (true) {
                _randomNumber.value = Random.nextInt(0, 101)
                delay(1_000)
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    inner class LocalBinder : Binder() {
        fun getService(): RandomNumberBindService = this@RandomNumberBindService
    }
}
