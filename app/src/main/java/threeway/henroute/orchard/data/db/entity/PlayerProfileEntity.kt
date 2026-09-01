package threeway.henroute.orchard.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val coins: Int = 100,
    val selectedSkinId: String = "hen_classic",
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val soundVolume: Int = 90,
    val musicVolume: Int = 75,
    val maxUnlockedLevel: Int = 1
)
