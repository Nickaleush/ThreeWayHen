package threeway.henroute.orchard.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import threeway.henroute.orchard.data.db.dao.AchievementDao
import threeway.henroute.orchard.data.db.dao.GameStatsDao
import threeway.henroute.orchard.data.db.dao.InventoryDao
import threeway.henroute.orchard.data.db.dao.PlayerDao
import threeway.henroute.orchard.data.db.dao.ScoreDao
import threeway.henroute.orchard.data.db.entity.AchievementEntity
import threeway.henroute.orchard.data.db.entity.GameStatsEntity
import threeway.henroute.orchard.data.db.entity.InventoryItemEntity
import threeway.henroute.orchard.data.db.entity.PlayerProfileEntity
import threeway.henroute.orchard.data.db.entity.ScoreEntity

@Database(
    entities = [
        ScoreEntity::class,
        PlayerProfileEntity::class,
        InventoryItemEntity::class,
        AchievementEntity::class,
        GameStatsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scoreDao(): ScoreDao
    abstract fun playerDao(): PlayerDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun achievementDao(): AchievementDao
    abstract fun gameStatsDao(): GameStatsDao

    companion object {
        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "three_way_hen.db"
        ).fallbackToDestructiveMigration().build()
    }
}
