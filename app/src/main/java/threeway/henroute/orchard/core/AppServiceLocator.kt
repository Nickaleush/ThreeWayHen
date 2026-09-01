package threeway.henroute.orchard.core

import android.content.Context
import threeway.henroute.orchard.core.audio.SoundManager
import threeway.henroute.orchard.core.dispatchers.AppDispatchers
import threeway.henroute.orchard.data.db.AppDatabase
import threeway.henroute.orchard.data.repository.GameRepository
import threeway.henroute.orchard.data.repository.ScoreRepository

class AppServiceLocator(
    private val context: Context
) {

    val database: AppDatabase by lazy {
        AppDatabase.create(context)
    }

    val soundManager: SoundManager by lazy {
        SoundManager(context.applicationContext)
    }

    val scoreRepository: ScoreRepository by lazy {
        ScoreRepository(database.scoreDao())
    }

    val gameRepository: GameRepository by lazy {
        GameRepository(
            scoreRepository = scoreRepository,
            playerDao = database.playerDao(),
            inventoryDao = database.inventoryDao(),
            achievementDao = database.achievementDao(),
            gameStatsDao = database.gameStatsDao()
        )
    }

    val dispatchers: AppDispatchers by lazy {
        AppDispatchers()
    }
}
