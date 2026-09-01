package threeway.henroute.orchard.data.repository

import threeway.henroute.orchard.data.db.dao.ScoreDao
import threeway.henroute.orchard.data.db.entity.ScoreEntity

class ScoreRepository(
    private val scoreDao: ScoreDao
) {

    suspend fun saveScore(gameId: String, score: Int, durationMs: Long) {
        scoreDao.insert(
            ScoreEntity(
                gameId = gameId,
                score = score,
                durationMs = durationMs,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getBestScore(gameId: String): Int {
        return scoreDao.getBestScore(gameId) ?: 0
    }

    suspend fun getBestScores(gameId: String): List<ScoreEntity> {
        return scoreDao.getBestScores(gameId)
    }
}
