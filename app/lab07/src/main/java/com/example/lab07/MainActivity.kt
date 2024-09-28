package com.example.lab07

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.lab07.ui.theme.Andriod_LabsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.system.measureTimeMillis


const val N = 100

class MainActivity : ComponentActivity() {

    // Account class to manage bank account operations
    class Account {
        private var amount: Double = 0.0
        private val mutex = Mutex()

        // Simulate deposit operation
        suspend fun deposit(amount: Double) {
            mutex.withLock {
                val x = this.amount
                delay(1) // Simulates processing time
                this.amount = x + amount
            }
        }

        // Return current account balance
        fun saldo(): Double = amount
    }

    // Function to measure the execution time of a given block of code
    fun withTimeMeasurement(title: String, isActive: Boolean = true, code: () -> Unit) {
        if (!isActive) return
        val time = measureTimeMillis { code() }
        Log.i("MSU", "operation in '$title' took ${time} ms")
    }

    // Data class to hold saldo values
    data class Saldos(val saldo1: Double, val saldo2: Double)

    // Bank process function that performs deposit operations and measures execution time
    fun bankProcess(account: Account): Saldos {
        var saldo1: Double = 0.0
        var saldo2: Double = 0.0

        // Measure the execution time of one deposit task
        withTimeMeasurement("Single coroutine deposit $N times") {
            runBlocking {
                launch {
                    for (i in 1..N) {
                        account.deposit(0.0)
                    }
                }
            }
            saldo1 = account.saldo()
        }

        // Measure the execution time of two simultaneous deposit tasks using coroutines
        withTimeMeasurement("Two $N times deposit coroutines together", isActive = true) {
            runBlocking {
                launch {
                    for (i in 1..N) account.deposit(1.0)
                }
                launch {
                    for (i in 1..N) account.deposit(1.0)
                }
            }
            saldo2 = account.saldo()
        }

        return Saldos(saldo1, saldo2)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Perform the bank process and store results
        val results = bankProcess(Account())

        // Display the results using Jetpack Compose
        setContent {
            Andriod_LabsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ShowResults(saldo1 = results.saldo1, saldo2 = results.saldo2)
                }
            }
        }
    }
}

@Composable
fun ShowResults(saldo1: Double, saldo2: Double) {
    Column {
        Text(text = "Saldo1: $saldo1")
        Text(text = "Saldo2: $saldo2")
    }
}