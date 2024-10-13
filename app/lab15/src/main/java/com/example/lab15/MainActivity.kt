package com.example.lab15

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lab15.ui.theme.Andriod_LabsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioViewModel : ViewModel() {
    val activeAudio = MutableLiveData<String?>(null)
    val playingState = MutableLiveData<Boolean>(false)
    val recordingState = MutableLiveData<Boolean>(false)
    val audioFilesList = MutableLiveData<List<String>?>(null)

    private val fileExtension = ".pcm"
    private val bufferSize = AudioRecord.getMinBufferSize(
        44100,
        AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    private val formatConfig = AudioFormat.Builder()
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setSampleRate(44100)
        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
        .build()
    private lateinit var recorder: AudioRecord
    private lateinit var player: AudioTrack

    fun startPlayback(context: Context, audioFile: String) {
        playingState.postValue(true)
        activeAudio.postValue(audioFile)

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val file = File(dir, audioFile + fileExtension)

        if (!file.exists()) {
            Log.e("AUDIO_APP", "File not found: $audioFile$fileExtension")
            return
        }

        val audioData = file.readBytes()
        player = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(formatConfig)
            .setBufferSizeInBytes(audioData.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        player.write(audioData, 0, audioData.size)

        viewModelScope.launch(Dispatchers.IO) {
            player.play()

            val totalSamples = audioData.size / 4 // 4 bytes per stereo sample

            while (playingState.value == true) {
                val currentPosition = player.playbackHeadPosition

                if (currentPosition >= totalSamples) {
                    stopPlayback()
                    break
                }
                kotlinx.coroutines.delay(100)
            }
        }
    }

    fun stopPlayback() {
        player.stop()
        player.release()
        activeAudio.postValue(null)
        playingState.postValue(false)
    }

    fun loadAudioFiles(context: Context) {
        audioFilesList.value = null
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)

        if (storageDir != null && storageDir.exists()) {
            val files = storageDir.listFiles()
            val fileList = mutableListOf<String>()

            files?.forEach { file ->
                val name = file.nameWithoutExtension
                fileList.add(name)
            }

            fileList.sortBy {
                it.substringAfterLast("record_").toIntOrNull() ?: Int.MAX_VALUE
            }

            audioFilesList.postValue(fileList)
        }
    }

    private fun generateFileName(): String {
        val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "record_$date$fileExtension"
    }

    fun deleteAudioFile(context: Context, fileName: String): Boolean {
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val fileToDelete = File(storageDir, "$fileName$fileExtension")

        if (fileToDelete.exists()) {
            val success = fileToDelete.delete()
            loadAudioFiles(context)
            return success
        }
        return false
    }

    fun startRecording(context: Context) {
        val fileName = generateFileName()
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val outputFile = File(dir, fileName)
        val buffer = ByteArray(bufferSize)

        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            recorder = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(formatConfig)
                .setBufferSizeInBytes(bufferSize)
                .build()
        } else {
            // Handle the case where the permission is not granted
            Log.e("AUDIO_APP", "RECORD_AUDIO permission not granted")
        }

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            Log.e("AUDIO_APP", "Recorder failed to initialize.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            recorder.startRecording()
            recordingState.postValue(true)

            FileOutputStream(outputFile).use { fos ->
                DataOutputStream(BufferedOutputStream(fos)).use { dos ->
                    try {
                        while (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                            val readBytes = recorder.read(buffer, 0, buffer.size)
                            if (readBytes > 0) dos.write(buffer, 0, readBytes)
                        }
                    } finally {
                        dos.close()
                    }
                }
            }
        }
    }

    fun stopRecording() {
        recorder.stop()
        recorder.release()
        recordingState.postValue(false)
    }
}

@Composable
fun AudioApp(viewModel: AudioViewModel) {
    val context = LocalContext.current
    val recordings by viewModel.audioFilesList.observeAsState(emptyList())
    val currentAudio by viewModel.activeAudio.observeAsState(null)
    val isPlaying by viewModel.playingState.observeAsState(false)
    val isRecording by viewModel.recordingState.observeAsState(false)

    viewModel.loadAudioFiles(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(recordings ?: emptyList()) { audioFile ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = audioFile, modifier = Modifier.weight(1f))

                    Spacer(modifier = Modifier.width(8.dp))

                    if (!isRecording) {
                        Icon(
                            imageVector = if ((!isPlaying && audioFile == currentAudio) || audioFile != currentAudio) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = "Play",
                            tint = Color.DarkGray,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable {
                                    if (!isPlaying) viewModel.startPlayback(
                                        context,
                                        audioFile
                                    ) else viewModel.stopPlayback()
                                },
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { viewModel.deleteAudioFile(context, audioFile) }
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = Color.Gray
        )

        Button(
            onClick = {
                if (!isRecording) viewModel.startRecording(context)
                else viewModel.stopRecording()
            },
            enabled = !isPlaying,
            modifier = Modifier.padding(10.dp),
            shape = RoundedCornerShape(8.dp),
            colors = if (!isRecording) {
                ButtonDefaults.buttonColors(
                    containerColor = Color.Unspecified,
                    contentColor = Color.Black
                )
            } else {
                ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
            }
        ) {
            Text(if (!isRecording) "Record" else "Stop recording")
        }
    }
}


class MainActivity : ComponentActivity() {
    private fun checkPermissions(): Boolean {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.d("AUDIO_APP", "Requesting permissions for recording.")
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            return false
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Andriod_LabsTheme {
                if (checkPermissions()) {
                    val audioViewModel: AudioViewModel = viewModel()
                    AudioApp(viewModel = audioViewModel)
                }
            }
        }
    }
}

//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Andriod_LabsTheme {
        val audioViewModel: AudioViewModel = viewModel()
        AudioApp(viewModel = audioViewModel)

    }
}