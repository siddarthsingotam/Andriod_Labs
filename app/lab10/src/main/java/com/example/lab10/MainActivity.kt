package com.example.lab10

import GuessResult
import NumberGame
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lab10.ui.theme.Andriod_LabsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Andriod_LabsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NumberGuessingGame(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun NumberGuessingGame(modifier: Modifier = Modifier) {
    // Game logic instance
    val game = remember { NumberGame(1..10) }

    // State variables for input, result, and guess count
    var guessInput by remember { mutableStateOf("") }
    var guessResult by remember { mutableStateOf<GuessResult?>(null) }
    var guesses by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        // Text field for user input
        TextField(
            value = guessInput,
            onValueChange = { guessInput = it },
            label = { Text("Enter your guess") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Button to make the guess
        Button(
            onClick = {
                val guess = guessInput.toIntOrNull()
                if (guess != null) {
                    guessResult = game.makeGuess(guess)
                    guesses = game.guesses.size
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Check Guess")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Display result
        Text(
            text = when (guessResult) {
                GuessResult.HIGH -> "Too High!"
                GuessResult.LOW -> "Too Low!"
                GuessResult.HIT -> "Correct! You've guessed it in $guesses attempts!"
                else -> "Make a guess!"
            },
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NumberGuessingGamePreview() {
    Andriod_LabsTheme {
        NumberGuessingGame()
    }
}
