package threeway.henroute.orchard.games.hen

data class HenGameResult(
    val level: Int,
    val score: Int,
    val durationSeconds: Int,
    val distance: Int,
    val feathersCollected: Int,
    val coinsCollected: Int,
    val livesRemaining: Int,
    val branchesTaken: Int,
    val featherBranches: Int,
    val coinBranches: Int,
    val safeBranches: Int,
    val bestStreak: Int,
    val directionsMask: Int,
    val passed: Boolean,
    val rewardCoins: Int
)
