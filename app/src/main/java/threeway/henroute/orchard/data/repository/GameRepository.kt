package threeway.henroute.orchard.data.repository

import threeway.henroute.orchard.data.db.dao.AchievementDao
import threeway.henroute.orchard.data.db.dao.GameStatsDao
import threeway.henroute.orchard.data.db.dao.InventoryDao
import threeway.henroute.orchard.data.db.dao.PlayerDao
import threeway.henroute.orchard.data.db.entity.AchievementEntity
import threeway.henroute.orchard.data.db.entity.GameStatsEntity
import threeway.henroute.orchard.data.db.entity.InventoryItemEntity
import threeway.henroute.orchard.data.db.entity.PlayerProfileEntity
import threeway.henroute.orchard.domain.model.GameResult
import threeway.henroute.orchard.games.hen.HenGameResult
import threeway.henroute.orchard.games.hen.HenLevelConfig
import threeway.henroute.orchard.games.hen.HenSkins
import kotlin.math.max

class GameRepository(
    private val scoreRepository: ScoreRepository,
    private val playerDao: PlayerDao,
    private val inventoryDao: InventoryDao,
    private val achievementDao: AchievementDao,
    private val gameStatsDao: GameStatsDao
) {

    suspend fun saveGameResult(result: GameResult) {
        scoreRepository.saveScore(result.gameId, result.score, result.durationMs)
    }

    suspend fun saveHenResult(result: HenGameResult) {
        ensureDefaults()
        saveGameResult(
            GameResult(
                gameId = GAME_ID,
                score = result.score,
                durationMs = result.durationSeconds * 1000L,
                finishedAt = System.currentTimeMillis()
            )
        )

        if (result.rewardCoins > 0) addCoins(result.rewardCoins)
        if (result.passed && result.level < MAX_LEVEL) unlockUpToLevel(result.level + 1)

        val old = getStats()
        val updated = old.copy(
            branchesTaken = old.branchesTaken + result.branchesTaken,
            featherBranches = old.featherBranches + result.featherBranches,
            coinBranches = old.coinBranches + result.coinBranches,
            safeBranches = old.safeBranches + result.safeBranches,
            levelsPassed = old.levelsPassed + if (result.passed) 1 else 0,
            highestLevelPassed = if (result.passed) max(old.highestLevelPassed, result.level) else old.highestLevelPassed,
            noHitLevels = old.noHitLevels + if (result.passed && result.livesRemaining == 3) 1 else 0,
            allDirectionsLevels = old.allDirectionsLevels + if (result.passed && result.directionsMask == 0b111) 1 else 0,
            feathersCollected = old.feathersCollected + result.feathersCollected,
            coinsCollected = old.coinsCollected + result.coinsCollected,
            bestStreak = max(old.bestStreak, result.bestStreak),
            totalDistance = old.totalDistance + result.distance
        )
        gameStatsDao.update(updated)
        refreshAchievements(updated)
    }

    suspend fun unlockUpToLevel(level: Int) {
        val profile = getProfile()
        val target = level.coerceIn(1, MAX_LEVEL)
        if (target > profile.maxUnlockedLevel) playerDao.update(profile.copy(maxUnlockedLevel = target))
    }

    suspend fun getProfile(): PlayerProfileEntity {
        ensureDefaults()
        return requireNotNull(playerDao.getProfile())
    }

    suspend fun getStats(): GameStatsEntity {
        ensureDefaults()
        return requireNotNull(gameStatsDao.getStats())
    }

    suspend fun getMaxUnlockedLevel(): Int = getProfile().maxUnlockedLevel.coerceIn(1, MAX_LEVEL)
    suspend fun getBestScore(gameId: String = GAME_ID): Int = scoreRepository.getBestScore(gameId)
    suspend fun getTopScores(limit: Int = 10) = scoreRepository.getBestScores(GAME_ID).take(limit)
    suspend fun getCoins(): Int = getProfile().coins

    suspend fun addCoins(amount: Int) {
        val profile = getProfile()
        playerDao.update(profile.copy(coins = max(0, profile.coins + amount)))
    }

    suspend fun getShopItems(): List<InventoryItemEntity> {
        ensureDefaults()
        return inventoryDao.getAll()
    }

    suspend fun buyOrSelectItem(itemId: String): PurchaseResult {
        ensureDefaults()
        val item = inventoryDao.getById(itemId) ?: return PurchaseResult.NotFound
        val profile = getProfile()
        if (!item.isUnlocked && profile.coins < item.price) return PurchaseResult.NotEnoughCoins

        if (!item.isUnlocked) {
            playerDao.update(profile.copy(coins = profile.coins - item.price))
            inventoryDao.update(item.copy(isUnlocked = true))
        }
        inventoryDao.clearSelected(TYPE_SKIN)
        inventoryDao.select(item.id)
        val latestProfile = getProfile()
        playerDao.update(latestProfile.copy(selectedSkinId = item.id))
        refreshAchievements(getStats())
        return PurchaseResult.Success
    }

    suspend fun getAchievements(): List<AchievementEntity> {
        ensureDefaults()
        refreshAchievements(getStats())
        return achievementDao.getAll()
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        val profile = getProfile()
        playerDao.update(profile.copy(soundEnabled = enabled))
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        val profile = getProfile()
        playerDao.update(profile.copy(musicEnabled = enabled))
    }

    suspend fun setSoundVolume(volume: Int) {
        val profile = getProfile()
        playerDao.update(profile.copy(soundVolume = volume.coerceIn(0, 100)))
    }

    suspend fun setMusicVolume(volume: Int) {
        val profile = getProfile()
        playerDao.update(profile.copy(musicVolume = volume.coerceIn(0, 100)))
    }

    private suspend fun ensureDefaults() {
        if (playerDao.getProfile() == null) playerDao.insert(PlayerProfileEntity())
        if (gameStatsDao.getStats() == null) gameStatsDao.insert(GameStatsEntity())

        inventoryDao.insertDefaults(
            HenSkins.all.mapIndexed { index, skin ->
                InventoryItemEntity(
                    id = skin.id,
                    type = TYPE_SKIN,
                    title = skin.title,
                    description = skin.description,
                    price = skin.price,
                    isUnlocked = index == 0,
                    isSelected = index == 0
                )
            }
        )
        HenSkins.all.forEach { skin ->
            inventoryDao.updateCatalogFields(skin.id, skin.title, skin.description, skin.price)
        }

        achievementDao.insertDefaults(defaultAchievements())
    }

    private suspend fun refreshAchievements(stats: GameStatsEntity) {
        val previewBought = inventoryDao.getById("hen_preview")?.isUnlocked == true
        val values = mapOf(
            "first_branch" to stats.branchesTaken,
            "feather_path" to stats.featherBranches,
            "coin_path" to stats.coinBranches,
            "safe_choice" to stats.safeBranches,
            "three_boss" to stats.highestLevelPassed,
            "route_king" to stats.highestLevelPassed,
            "preview_fan" to if (previewBought) 1 else 0,
            "marathon_hen" to stats.levelsPassed,
            "no_hit" to stats.noHitLevels,
            "all_branches" to stats.allDirectionsLevels
        )
        val updated = achievementDao.getAll().map { achievement ->
            val value = values[achievement.id] ?: achievement.currentValue
            achievement.copy(
                currentValue = value.coerceAtMost(achievement.targetValue),
                isUnlocked = value >= achievement.targetValue
            )
        }
        if (updated.isNotEmpty()) achievementDao.update(updated)
    }

    private fun defaultAchievements() = listOf(
        achievement("first_branch", "First Branch", "Take your first branch at a fork.", 1),
        achievement("feather_path", "Feather Path", "Choose the feather branch 30 times.", 30),
        achievement("coin_path", "Coin Path", "Choose the coin branch 30 times.", 30),
        achievement("safe_choice", "Safe Choice", "Choose the safe branch 50 times.", 50),
        achievement("three_boss", "Three Boss", "Beat level 20.", 20),
        achievement("route_king", "Route King", "Beat all 40 levels.", 40),
        achievement("preview_fan", "Preview Fan", "Buy the Preview Far skin.", 1),
        achievement("marathon_hen", "Marathon Hen", "Beat 25 levels.", 25),
        achievement("no_hit", "No Hit", "Beat 5 levels without losing a life.", 5),
        achievement("all_branches", "All Branches", "Use left, straight, and right in one level.", 1)
    )

    private fun achievement(id: String, title: String, description: String, target: Int) =
        AchievementEntity(id, title, description, 0, target, false)

    sealed interface PurchaseResult {
        data object Success : PurchaseResult
        data object NotEnoughCoins : PurchaseResult
        data object NotFound : PurchaseResult
    }

    companion object {
        const val GAME_ID = "three_way_hen"
        const val TYPE_SKIN = "skin"
        const val MAX_LEVEL = HenLevelConfig.MAX_LEVEL
    }
}
