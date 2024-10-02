
enum class GuessResult {HIGH, LOW, HIT }

class NumberGame(val range: IntRange) {
    private val secret = range.random()
    var guesses = listOf<Int>()
        private set

    fun makeGuess(guess: Int): GuessResult {
        guesses = guesses.plus(guess)
        return when (guess) {
            in Int.MIN_VALUE..<secret -> GuessResult.LOW
            in secret+1..Int.MAX_VALUE -> GuessResult.HIGH
            else -> GuessResult.HIT
        }
    }
}