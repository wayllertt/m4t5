package com.example.m4t5

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.m4t5.ui.theme.M4t5Theme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var counterSeconds by mutableIntStateOf(0)
    private var timerInput by mutableStateOf("30")
    private var bindServiceValue by mutableIntStateOf(0)
    private var bindState by mutableStateOf("Отключено")

    private var randomService: RandomNumberBindService? = null
    private var randomCollectJob: Job? = null
    private var isBound = false

    private val counterReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ForegroundCounterService.ACTION_COUNTER_TICK) {
                counterSeconds = intent.getIntExtra(ForegroundCounterService.EXTRA_SECONDS, counterSeconds)
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RandomNumberBindService.LocalBinder
            randomService = binder.getService()
            bindState = "Подключено"
            randomCollectJob = lifecycleScope.launch {
                randomService?.randomNumber?.collect { value ->
                    bindServiceValue = value
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bindState = "Отключено"
            randomCollectJob?.cancel()
            randomService = null
        }
    }

    private val requestNotificationsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        askNotificationPermissionIfNeeded()

        setContent {
            M4t5Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ServicesScreen(
                        modifier = Modifier.padding(innerPadding),
                        counterSeconds = counterSeconds,
                        timerInput = timerInput,
                        onTimerInputChange = { timerInput = it.filter(Char::isDigit) },
                        bindServiceValue = bindServiceValue,
                        bindState = bindState,
                        onStartCounter = { startCounterService() },
                        onStopCounter = { stopCounterService() },
                        onStartTimer = { startOneShotTimer() },
                        onConnect = { connectBindService() },
                        onDisconnect = { disconnectBindService() }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            counterReceiver,
            IntentFilter(ForegroundCounterService.ACTION_COUNTER_TICK),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        unregisterReceiver(counterReceiver)
        if (isBound) {
            disconnectBindService()
        }
        super.onStop()
    }

    private fun startCounterService() {
        val intent = Intent(this, ForegroundCounterService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopCounterService() {
        stopService(Intent(this, ForegroundCounterService::class.java))
    }

    private fun startOneShotTimer() {
        val seconds = timerInput.toIntOrNull() ?: return
        val intent = Intent(this, OneShotTimerService::class.java)
            .putExtra(OneShotTimerService.EXTRA_TIMER_SECONDS, seconds)
        startService(intent)
    }

    private fun connectBindService() {
        if (isBound) return
        val intent = Intent(this, RandomNumberBindService::class.java)
        isBound = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun disconnectBindService() {
        if (!isBound) return
        randomCollectJob?.cancel()
        unbindService(serviceConnection)
        isBound = false
        bindState = "Отключено"
    }

    private fun askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                requestNotificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun ServicesScreen(
    modifier: Modifier = Modifier,
    counterSeconds: Int,
    timerInput: String,
    onTimerInputChange: (String) -> Unit,
    bindServiceValue: Int,
    bindState: String,
    onStartCounter: () -> Unit,
    onStopCounter: () -> Unit,
    onStartTimer: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Задание 5: Foreground service")
        Text(text = "Прошло $counterSeconds секунд", fontSize = 32.sp)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStartCounter, modifier = Modifier.fillMaxWidth()) {
                Text("Старт")
            }
            Button(onClick = onStopCounter, modifier = Modifier.fillMaxWidth()) {
                Text("Стоп")
            }
        }

        Text(text = "Задание 6: Background service")
        OutlinedTextField(
            value = timerInput,
            onValueChange = onTimerInputChange,
            label = { Text("Секунды") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors()
        )
        Button(onClick = onStartTimer, modifier = Modifier.fillMaxWidth()) {
            Text("Запустить таймер")
        }

        Text(text = "Задание 7: Bind service")
        Text(text = "Состояние: $bindState")
        Text(text = "Случайное число: $bindServiceValue", fontSize = 24.sp)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onConnect, modifier = Modifier.fillMaxWidth()) {
                Text("Подключиться")
            }
            Button(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) {
                Text("Отключиться")
            }
        }
    }
}
