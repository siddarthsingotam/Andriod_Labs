package com.example.myfirstcoroutine

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.myfirstcoroutine.ui.theme.Andriod_LabsTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activitty_main)

//        //"""1. Using delays with coroutines """
//        GlobalScope.launch {
//            delay(3000L)
//            Log.d(TAG, "Coroutine says hello ${Thread.currentThread().name}")
//        }
//
//
//        Log.d(TAG, "Hello from thread ${Thread.currentThread().name}")


//        //"""2. Suspending functions """
//        GlobalScope.launch {
//            delay(1000L)
//            val doCallAnswer = doNetworkCall()
//            val doCallAnswer2 = doNetworkCall2()
//
//            Log.d(TAG, doCallAnswer)
//            Log.d(TAG, doCallAnswer2)
//        }

//      //"""3.Coroutine Context"""
//        GlobalScope.launch(Dispatchers.IO) {
//            Log.d(TAG, "Starting coroutine thread at ${Thread.currentThread().name}")
//            val answer = doNetworkCall()
//            withContext(Dispatchers.Main) {
//                Log.d(TAG, "Starting coroutine thread ____ at ${Thread.currentThread().name}")
//
//            }
//        }

//        //"""4.Run blocking """
//        Log.d(TAG, "Before run blocking")
//        runBlocking {
//            launch (Dispatchers.IO){
//                delay(3000L)
//                Log.d(TAG, "Executed coroutine task 1 with ${Thread.currentThread().name}")
//            }
//            launch (Dispatchers.IO){
//                delay(3000L)
//                Log.d(TAG, "Executed coroutine task 2 with ${Thread.currentThread().name}")
//            }
//            delay(5000L)
//            Log.d(TAG, "end of run blocking")
//        }
//
//        Log.d(TAG, "After run blocking")
//
        //"""5. Jobs, Waiting and Cancellation""" Using withTimeout
        val job = GlobalScope.launch(Dispatchers.Default) {
            Log.d(TAG, "Thread is running")
            delay(2000L)
            for (i in 30..40) {
                if (isActive) {
                    Log.d(TAG, "Calculation number of f($i) = ${fib(n = i)}")
                }
            }
            Log.d(TAG, "Job is finished properly")
        }

        runBlocking {
            delay(3000L)
            job.cancel()
            Log.d(TAG, "Finished execution of runBlock")

        }


    }


    //"""Sample suspending functions for 2nd lesson """
    suspend fun doNetworkCall(): String {
        delay(3000L)
        return "The network call has been called"
    }

    suspend fun doNetworkCall2(): String {
        delay(3000L)
        return "The network call has been called2"
    }

    suspend fun fib(n: Int): Long {
        return if (n == 0) 0
        else if (n == 1) 1
        else fib(n - 1) + fib(n - 2)
    }


}
