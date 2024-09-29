package com.example.lab08

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageDownloader("https://users.metropolia.fi/~jarkkov/folderimage.jpg")
        }
    }
}

@Composable
fun ImageDownloader(imageUrl: String) {
    // State for storing the downloaded bitmap
    val bitmapState = remember { mutableStateOf<Bitmap?>(null) }

    // LaunchedEffect to download the image
    LaunchedEffect(imageUrl) {
        bitmapState.value = downloadImage(imageUrl)
    }

    // Display the downloaded image
    bitmapState.value?.let { bitmap ->
        BitmapCanvas(bitmap = bitmap, modifier = Modifier.fillMaxSize())
    }
}

// Composable to display Bitmap using Canvas
@Composable
fun BitmapCanvas(bitmap: Bitmap, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val canvas = drawContext.canvas.nativeCanvas // Get the native canvas
        canvas.drawBitmap(bitmap, 0f, 0f, null) // Draw the bitmap
    }
}

// Function to download the image in a coroutine
suspend fun downloadImage(url: String): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val inputStream = URL(url).openStream()
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

