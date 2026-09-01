package threeway.henroute.orchard.games.hen

import kotlin.math.roundToInt

data class HenLevelConfig(
    val level: Int,
    val durationSeconds: Int,
    val speedMultiplier: Float,
    val forkCount: Int,
    val obstaclesPerBranch: Int,
    val theme: RoadTheme,
    val rapidForks: Boolean,
    val rewardCoins: Int
) {
    enum class RoadTheme { Orchard, Field, Forest, Mixed }

    companion object {
        const val MAX_LEVEL = 40

        fun forLevel(level: Int): HenLevelConfig {
            val value = level.coerceIn(1, MAX_LEVEL)
            val exact = when (value) {
                1 -> Base(60, 1.0f, 5, 1, RoadTheme.Field, false)
                2 -> Base(80, 1.1f, 7, 2, RoadTheme.Orchard, false)
                3 -> Base(100, 1.2f, 9, 2, RoadTheme.Mixed, false)
                4 -> Base(110, 1.35f, 11, 3, RoadTheme.Forest, true)
                5 -> Base(120, 1.5f, 13, 3, RoadTheme.Mixed, true)
                else -> {
                    val tier = (value - 6) / 7
                    val local = (value - 6) % 7
                    Base(
                        durationSeconds = (96 + tier * 6 + local * 3).coerceAtMost(130),
                        speedMultiplier = (1.45f + tier * 0.13f + local * 0.025f).coerceAtMost(2.25f),
                        forkCount = (10 + tier * 2 + local / 2).coerceAtMost(19),
                        obstaclesPerBranch = (2 + tier / 2 + if (local >= 4) 1 else 0).coerceAtMost(5),
                        theme = RoadTheme.entries[(value - 1) % RoadTheme.entries.size],
                        rapidForks = value >= 8
                    )
                }
            }
            return HenLevelConfig(
                level = value,
                durationSeconds = exact.durationSeconds,
                speedMultiplier = exact.speedMultiplier,
                forkCount = exact.forkCount,
                obstaclesPerBranch = exact.obstaclesPerBranch,
                theme = exact.theme,
                rapidForks = exact.rapidForks,
                rewardCoins = rewardForLevel(value)
            )
        }

        fun rewardForLevel(level: Int): Int = when (level.coerceIn(1, MAX_LEVEL)) {
            in 1..10 -> 100
            in 11..20 -> 200
            in 21..30 -> 300
            else -> 400
        }

        fun displayDifficulty(level: Int): Int = (forLevel(level).speedMultiplier * 100).roundToInt()

        private data class Base(
            val durationSeconds: Int,
            val speedMultiplier: Float,
            val forkCount: Int,
            val obstaclesPerBranch: Int,
            val theme: RoadTheme,
            val rapidForks: Boolean
        )
    }
}
