package threeway.henroute.orchard.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_stats")
data class GameStatsEntity(
    @PrimaryKey val id: Int = 1,
    val branchesTaken: Int = 0,
    val featherBranches: Int = 0,
    val coinBranches: Int = 0,
    val safeBranches: Int = 0,
    val levelsPassed: Int = 0,
    val highestLevelPassed: Int = 0,
    val noHitLevels: Int = 0,
    val allDirectionsLevels: Int = 0,
    val feathersCollected: Int = 0,
    val coinsCollected: Int = 0,
    val bestStreak: Int = 0,
    val totalDistance: Int = 0
)
