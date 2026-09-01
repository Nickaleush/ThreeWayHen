package threeway.henroute.orchard.games.hen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.DrawableRes
import threeway.henroute.orchard.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

class HenGameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface Callback {
        fun onHudChanged(state: HenHudState)
        fun onJumpSound()
        fun onSwipeSound()
        fun onCollectFeather()
        fun onCollectCoin()
        fun onHitObstacle()
        fun onForkAppeared()
        fun onGameFinished(result: HenGameResult)
    }

    var callback: Callback? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val destinationRect = RectF()
    private val seamMaskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    private val bitmapCache = mutableMapOf<Int, Bitmap>()

    private var config = HenLevelConfig.forLevel(1)
    private var ability = HenAbility.None
    private var selectedSkinId = "hen_classic"
    private var idleSkinResId = R.drawable.skin_default
    private var runSkinResId = R.drawable.skin_default_run

    private var plans: List<ForkPlan> = emptyList()
    private val objects = mutableListOf<RoadObject>()
    private var startedAtMs = 0L
    private var pausedAtMs = 0L
    private var accumulatedPauseMs = 0L
    private var paused = false
    private var finished = false
    private var pendingLane = Lane.CENTER
    private var activeLane = Lane.CENTER
    private var lastPreviewForkIndex = -1
    private var jumpStartedAt = -100f
    private var invulnerableUntil = -1f
    private var hitShakeUntil = -1f
    private var finishStartedAt = -1f
    private var resultDelivered = false

    private var lives = 3
    private var feathers = 0
    private var coins = 0
    private var branchesTaken = 0
    private var featherBranches = 0
    private var coinBranches = 0
    private var safeBranches = 0
    private var streak = 0
    private var bestStreak = 0
    private var directionsMask = 0
    private var score = 0

    private var downX = 0f
    private var downY = 0f
    private var downAt = 0L
    private var swipeHandled = false

    fun setAppearance(skinId: String) {
        selectedSkinId = skinId
        ability = HenSkins.abilityFor(skinId)
        idleSkinResId = skinIdleResource(skinId)
        runSkinResId = skinRunResource(skinId)
        bitmap(idleSkinResId)
        bitmap(runSkinResId)
        invalidate()
    }

    fun startLevel(level: Int) {
        config = HenLevelConfig.forLevel(level)
        plans = buildPlans(config)
        objects.clear()
        plans.forEach { objects += it.objects }
        startedAtMs = SystemClock.uptimeMillis()
        pausedAtMs = 0L
        accumulatedPauseMs = 0L
        paused = false
        finished = false
        resultDelivered = false
        pendingLane = Lane.CENTER
        activeLane = Lane.CENTER
        lastPreviewForkIndex = -1
        jumpStartedAt = -100f
        invulnerableUntil = -1f
        hitShakeUntil = -1f
        finishStartedAt = -1f
        lives = 3
        feathers = 0
        coins = 0
        branchesTaken = 0
        featherBranches = 0
        coinBranches = 0
        safeBranches = 0
        streak = 0
        bestStreak = 0
        directionsMask = 0
        score = 0
        notifyHud(0f)
        postInvalidateOnAnimation()
    }

    fun restart() = startLevel(config.level)

    fun setPaused(value: Boolean) {
        if (paused == value || finished) return
        val now = SystemClock.uptimeMillis()
        paused = value
        if (value) {
            pausedAtMs = now
        } else {
            if (pausedAtMs > 0L) accumulatedPauseMs += now - pausedAtMs
            pausedAtMs = 0L
            postInvalidateOnAnimation()
        }
    }

    fun stopGameLoop() {
        paused = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (paused || finished) return true

        val swipeThreshold = resources.displayMetrics.density * 32f
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                downAt = event.eventTime
                swipeHandled = false
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!swipeHandled) {
                    val dx = event.x - downX
                    val dy = event.y - downY
                    if (abs(dx) >= swipeThreshold && abs(dx) > abs(dy) * 1.05f) {
                        swipeHandled = shiftLane(if (dx < 0f) -1 else 1)
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (!swipeHandled && abs(dx) >= swipeThreshold && abs(dx) > abs(dy) * 1.05f) {
                    swipeHandled = shiftLane(if (dx < 0f) -1 else 1)
                }
                if (!swipeHandled && abs(dx) < swipeThreshold && abs(dy) < swipeThreshold &&
                    event.eventTime - downAt < 500L
                ) {
                    performClick()
                    jump(elapsedSeconds())
                }
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                swipeHandled = false
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = elapsedSeconds()
        if (!paused && !finished) update(now)
        drawScene(canvas, now)
        if (!paused && (!finished || !resultDelivered)) postInvalidateOnAnimation()
    }

    private fun update(now: Float) {
        val preview = previewFork(now)
        if (preview != null && preview.index != lastPreviewForkIndex) {
            lastPreviewForkIndex = preview.index
            callback?.onForkAppeared()
        }

        plans.forEach { fork ->
            if (!fork.choiceCommitted && now >= fork.startSec + choiceGrace()) {
                commitChoice(fork)
            }
            if (fork.choiceCommitted && !fork.completed && now >= fork.endSec) {
                fork.completed = true
                activeLane = Lane.CENTER
                pendingLane = plans.firstOrNull { !it.choiceCommitted }?.selectedLane ?: Lane.CENTER
                streak++
                bestStreak = max(bestStreak, streak)
            }
        }

        val currentFork = plans.firstOrNull { now in it.startSec..it.endSec }
        activeLane = currentFork?.selectedLane ?: Lane.CENTER

        if (ability == HenAbility.AutoJump && !isJumping(now)) {
            val obstacle = objects.firstOrNull {
                !it.processed && it.kind.isObstacle && laneMatches(it.lane, activeLane) &&
                    it.timeSec - now in 0.28f..0.68f
            }
            if (obstacle != null) jump(now)
        }

        objects.asSequence()
            .filter { !it.processed && now >= it.timeSec }
            .forEach { resolveObject(it, now) }

        score = distanceAt(now) + coins * 12 + feathers * 7 + bestStreak * 20
        notifyHud(now)

        if (lives <= 0) finish(false, now)
        else if (now >= config.durationSeconds) finish(true, now)
    }

    private fun shiftLane(direction: Int): Boolean {
        val now = elapsedSeconds()
        val fork = plans.firstOrNull {
            !it.choiceCommitted && now <= it.startSec + choiceGrace()
        } ?: return false

        val targetValue = (fork.selectedLane.value + direction)
            .coerceIn(Lane.LEFT.value, Lane.RIGHT.value)
        val targetLane = Lane.entries.first { it.value == targetValue }
        val changed = fork.selectedLane != targetLane
        fork.selectedLane = targetLane
        pendingLane = targetLane
        if (changed) callback?.onSwipeSound()
        invalidate()
        return changed
    }

    private fun commitChoice(fork: ForkPlan) {
        val committedLane = fork.selectedLane
        fork.choiceCommitted = true
        pendingLane = committedLane
        activeLane = committedLane
        branchesTaken++
        directionsMask = directionsMask or when (committedLane) {
            Lane.LEFT -> 0b001
            Lane.CENTER -> 0b010
            Lane.RIGHT -> 0b100
        }
        when (fork.contentFor(committedLane)) {
            BranchContent.FEATHER -> featherBranches++
            BranchContent.COIN -> coinBranches++
            BranchContent.SAFE -> safeBranches++
            BranchContent.DANGER -> Unit
        }
    }

    private fun resolveObject(roadObject: RoadObject, now: Float) {
        // Every object is processed once when it reaches the player. Collectibles are
        // hidden only when the player actually collected them; items on other lanes
        // remain visible and continue scrolling beyond the chicken.
        roadObject.processed = true
        val sameLane = laneMatches(roadObject.lane, activeLane)
        when (roadObject.kind) {
            ObjectKind.FEATHER -> {
                val magnet = ability == HenAbility.FeatherMagnet &&
                    abs(roadObject.lane.value - activeLane.value) <= 1
                if (sameLane || magnet) {
                    roadObject.collected = true
                    feathers++
                    callback?.onCollectFeather()
                }
            }

            ObjectKind.COIN -> if (sameLane) {
                roadObject.collected = true
                coins++
                callback?.onCollectCoin()
            }

            ObjectKind.ROCK, ObjectKind.STUMP, ObjectKind.LOG -> {
                if (sameLane && !isJumping(now) && now >= invulnerableUntil) {
                    lives--
                    streak = 0
                    invulnerableUntil = now + 1.1f
                    hitShakeUntil = now + 0.42f
                    callback?.onHitObstacle()
                }
            }
        }
    }

    private fun jump(now: Float) {
        if (isJumping(now)) return
        jumpStartedAt = now
        callback?.onJumpSound()
    }

    private fun finish(passed: Boolean, now: Float) {
        if (finished) return
        finished = true
        finishStartedAt = now
        val reward = if (passed) config.rewardCoins + coins else coins
        val result = HenGameResult(
            level = config.level,
            score = score,
            durationSeconds = min(now.roundToInt(), config.durationSeconds),
            distance = distanceAt(now),
            feathersCollected = feathers,
            coinsCollected = coins,
            livesRemaining = lives.coerceAtLeast(0),
            branchesTaken = branchesTaken,
            featherBranches = featherBranches,
            coinBranches = coinBranches,
            safeBranches = safeBranches,
            bestStreak = bestStreak,
            directionsMask = directionsMask,
            passed = passed,
            rewardCoins = reward
        )
        postDelayed({
            if (!resultDelivered) {
                resultDelivered = true
                callback?.onGameFinished(result)
            }
        }, if (passed) 900L else 450L)
    }

    private fun notifyHud(now: Float) {
        callback?.onHudChanged(
            HenHudState(
                level = config.level,
                distance = distanceAt(now),
                feathers = feathers,
                coins = coins,
                lives = lives.coerceAtLeast(0),
                streak = streak,
                timeLeftSeconds = (config.durationSeconds - now).coerceAtLeast(0f).roundToInt(),
                selectedLane = (plans.firstOrNull { !it.completed }?.selectedLane ?: pendingLane).value
            )
        )
    }

    private fun drawScene(canvas: Canvas, now: Float) {
        drawMovingBaseBackground(canvas, now)
        drawMovingLaneBackgrounds(canvas, now)
        drawFinishRibbon(canvas, now)
        drawRoadObjects(canvas, now)
        drawPreviewIcons(canvas, now)
        drawChicken(canvas, now)
        if (finished && lives > 0) drawConfetti(canvas, now)
    }

    /**
     * The normal road is always moving, even while a fork texture is on screen.
     * Adjacent copies overlap and cross-fade. This is important because the supplied
     * illustration is not a mathematically tileable texture: a hard butt-joint exposes
     * a visible horizontal seam on every loop.
     */
    private fun drawMovingBaseBackground(canvas: Canvas, now: Float) {
        val background = bitmap(R.drawable.game_bg)
        val scale = width.toFloat() / background.width.toFloat()
        val tileHeight = background.height * scale
        if (tileHeight <= 0f) return

        val overlap = min(tileHeight * 0.12f, height * 0.22f).coerceAtLeast(48f)
        val stride = (tileHeight - overlap).coerceAtLeast(1f)
        val offset = (now * scrollSpeedPx()) % stride
        var top = offset - stride * 2f
        var tileIndex = 0

        while (top < height + overlap) {
            destinationRect.set(0f, top, width.toFloat(), top + tileHeight)
            if (tileIndex == 0) {
                canvas.drawBitmap(background, null, destinationRect, paint)
            } else {
                drawBitmapWithTopCrossFade(canvas, background, destinationRect, overlap)
            }
            top += stride
            tileIndex++
        }
    }

    private fun drawBitmapWithTopCrossFade(
        canvas: Canvas,
        source: Bitmap,
        destination: RectF,
        fadeHeight: Float
    ) {
        val saveCount = canvas.saveLayer(destination, null)
        canvas.drawBitmap(source, null, destination, paint)
        seamMaskPaint.shader = LinearGradient(
            0f,
            destination.top,
            0f,
            destination.top + fadeHeight,
            intArrayOf(Color.TRANSPARENT, Color.WHITE, Color.WHITE),
            floatArrayOf(0f, 0.92f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(destination, seamMaskPaint)
        seamMaskPaint.shader = null
        canvas.restoreToCount(saveCount)
    }

    /**
     * Fork artwork scrolls at exactly the same speed as the base road. Its transparent
     * top and bottom fades reveal the base texture underneath, so road -> fork -> road
     * has no hard cut or flashing seam.
     */
    private fun drawMovingLaneBackgrounds(canvas: Canvas, now: Float) {
        val lanes = bitmap(R.drawable.game_lanes_bg)
        val scale = width.toFloat() / lanes.width.toFloat()
        val tileHeight = lanes.height * scale
        val splitLocalY = tileHeight * LANE_SPLIT_FRACTION
        val speed = scrollSpeedPx()
        plans.forEach { fork ->
            val top = playerGroundY() - splitLocalY + (now - fork.startSec) * speed
            val bottom = top + tileHeight
            if (bottom < 0f || top > height.toFloat()) return@forEach
            destinationRect.set(0f, top, width.toFloat(), bottom)
            canvas.drawBitmap(lanes, null, destinationRect, paint)
        }
    }

    private fun drawRoadObjects(canvas: Canvas, now: Float) {
        val speed = scrollSpeedPx().coerceAtLeast(1f)
        val pastWindow = (height - playerGroundY()) / speed + 1.2f
        objects.asSequence()
            // Only collectibles actually picked up by the player disappear. Missed items
            // and all obstacles remain visible until they naturally leave the screen.
            .filter { !it.collected &&
                it.timeSec - now in -pastWindow..visibleAheadSeconds() }
            .sortedByDescending { it.timeSec }
            .forEach { roadObject ->
                val fork = plans[roadObject.forkIndex]
                val x = branchX(fork, roadObject.lane, roadObject.timeSec)
                val y = screenY(roadObject.timeSec, now)
                val bitmap = bitmapFor(roadObject.kind)
                val targetWidth = width * when (roadObject.kind) {
                    ObjectKind.LOG -> 0.23f
                    ObjectKind.ROCK, ObjectKind.STUMP -> 0.19f
                    ObjectKind.FEATHER, ObjectKind.COIN -> 0.105f
                }
                drawBitmapWithBottomAnchor(canvas, bitmap, x, y, targetWidth)
            }
    }

    private fun drawPreviewIcons(canvas: Canvas, now: Float) {
        val fork = previewFork(now) ?: return
        val remaining = fork.startSec - now
        val pulse = if (remaining <= 2f) {
            1f + 0.08f * sin(((2f - remaining) * 8f).toDouble()).toFloat()
        } else {
            1f
        }
        Lane.entries.forEach { lane ->
            val x = width * (0.5f + lane.value * 0.26f)
            val y = height * 0.17f
            val size = width * 0.115f * pulse
            if (fork.selectedLane == lane) {
                strokePaint.color = Color.WHITE
                strokePaint.strokeWidth = max(3f, width * 0.009f)
                canvas.drawCircle(x, y, size * 0.62f, strokePaint)
            }
            val icon = bitmapFor(fork.contentFor(lane))
            drawBitmapCentered(canvas, icon, x, y, size)
        }
    }

    private fun drawFinishRibbon(canvas: Canvas, now: Float) {
        val y = screenY(config.durationSeconds.toFloat(), now)
        if (y < -width * 0.1f || y > height + width * 0.1f) return
        val left = width * 0.31f
        val right = width * 0.69f
        val cell = max(5f, width * 0.038f)
        var x = left
        var dark = true
        while (x < right) {
            paint.color = if (dark) Color.rgb(69, 58, 43) else Color.WHITE
            canvas.drawRect(x, y - cell, min(x + cell, right), y, paint)
            dark = !dark
            x += cell
        }
    }

    private fun drawChicken(canvas: Canvas, now: Float) {
        val fork = plans.firstOrNull { now in it.startSec..it.endSec }
        val lane = fork?.selectedLane ?: Lane.CENTER
        var x = fork?.let { branchX(it, lane, now) } ?: width * 0.5f
        if (now < hitShakeUntil) {
            x += sin((now * 55f).toDouble()).toFloat() * width * 0.025f
        }

        val jumping = isJumping(now)
        val jumpProgress = ((now - jumpStartedAt) / JUMP_DURATION).coerceIn(0f, 1f)
        val jumpHeight = if (jumping) {
            sin(PI * jumpProgress).toFloat() * height * 0.13f
        } else {
            0f
        }
        val runBob = if (jumping) 0f else sin((now * 13f).toDouble()).toFloat() * height * 0.004f
        val groundY = playerGroundY() - jumpHeight + runBob
        val chicken = bitmap(if (jumping) idleSkinResId else runSkinResId)
        val targetWidth = width * 0.215f
        val targetHeight = targetWidth * chicken.height / chicken.width.toFloat()

        destinationRect.set(
            x - targetWidth / 2f,
            groundY - targetHeight,
            x + targetWidth / 2f,
            groundY
        )

        val blink = now < invulnerableUntil && (now * 12f).toInt() % 2 == 0
        paint.alpha = if (blink) 95 else 255
        canvas.save()
        val tilt = if (jumping) (0.5f - jumpProgress) * 8f else sin((now * 8f).toDouble()).toFloat() * 1.4f
        canvas.rotate(tilt, x, groundY - targetHeight * 0.45f)
        canvas.drawBitmap(chicken, null, destinationRect, paint)
        canvas.restore()
        paint.alpha = 255
    }

    private fun drawConfetti(canvas: Canvas, now: Float) {
        val elapsed = (now - finishStartedAt).coerceAtLeast(0f)
        val random = Random(config.level * 811)
        repeat(45) { index ->
            val x = random.nextFloat() * width
            val speed = 0.18f + random.nextFloat() * 0.45f
            val y = ((random.nextFloat() + elapsed * speed) % 1.15f) * height
            val size = width * (0.008f + random.nextFloat() * 0.012f)
            paint.color = CONFETTI_COLORS[index % CONFETTI_COLORS.size]
            canvas.save()
            canvas.rotate(elapsed * 180f + index * 17f, x, y)
            canvas.drawRect(x - size, y - size * 0.4f, x + size, y + size * 0.4f, paint)
            canvas.restore()
        }
    }

    private fun drawBitmapCentered(
        canvas: Canvas,
        bitmap: Bitmap,
        centerX: Float,
        centerY: Float,
        targetWidth: Float
    ) {
        val targetHeight = targetWidth * bitmap.height / bitmap.width.toFloat()
        destinationRect.set(
            centerX - targetWidth / 2f,
            centerY - targetHeight / 2f,
            centerX + targetWidth / 2f,
            centerY + targetHeight / 2f
        )
        canvas.drawBitmap(bitmap, null, destinationRect, paint)
    }

    private fun drawBitmapWithBottomAnchor(
        canvas: Canvas,
        bitmap: Bitmap,
        centerX: Float,
        bottomY: Float,
        targetWidth: Float
    ) {
        val targetHeight = targetWidth * bitmap.height / bitmap.width.toFloat()
        destinationRect.set(
            centerX - targetWidth / 2f,
            bottomY - targetHeight,
            centerX + targetWidth / 2f,
            bottomY
        )
        canvas.drawBitmap(bitmap, null, destinationRect, paint)
    }

    private fun buildPlans(config: HenLevelConfig): List<ForkPlan> {
        val random = Random(config.level * 97 + 13)
        val spacing = (config.durationSeconds - 8f) / config.forkCount
        val duration = laneTravelDuration(config)
        return List(config.forkCount) { index ->
            val jitter = if (index == 0) 0f else {
                (random.nextFloat() - 0.5f) * min(1.8f, spacing * 0.22f)
            }
            val start = (6f + index * spacing + jitter)
                .coerceAtMost(config.durationSeconds - duration - 0.8f)
            val end = min(start + duration, config.durationSeconds - 0.8f)
            val contents = buildContents(random, config.level, index)
            ForkPlan(index, start, end, contents).also { plan ->
                plan.objects += buildObjects(plan, random, config)
            }
        }
    }

    private fun buildContents(random: Random, level: Int, index: Int): Array<BranchContent> {
        val base = arrayOf(BranchContent.FEATHER, BranchContent.COIN, BranchContent.SAFE)
        if (level >= 3 && (index + level) % 4 == 0) base[2] = BranchContent.DANGER
        if (random.nextFloat() < 0.42f) base.shuffle(random)
        return base
    }

    private fun buildObjects(
        plan: ForkPlan,
        random: Random,
        config: HenLevelConfig
    ): List<RoadObject> {
        val result = mutableListOf<RoadObject>()
        Lane.entries.forEach { lane ->
            val content = plan.contentFor(lane)
            val obstacleCount = when (content) {
                BranchContent.SAFE -> max(0, config.obstaclesPerBranch - 2)
                BranchContent.DANGER -> config.obstaclesPerBranch + 1
                else -> config.obstaclesPerBranch
            }
            val collectibleCount = when (content) {
                BranchContent.FEATHER -> 3 + config.level / 14
                BranchContent.COIN -> 3 + config.level / 18
                else -> 0
            }
            val slots = (obstacleCount + collectibleCount).coerceAtLeast(1)
            val available = (plan.endSec - plan.startSec - 0.55f).coerceAtLeast(0.45f)
            val times = (0 until slots).map { slot ->
                plan.startSec + 0.28f + available * (slot + 1f) / (slots + 1f)
            }.shuffled(random)
            repeat(collectibleCount) { index ->
                result += RoadObject(
                    forkIndex = plan.index,
                    lane = lane,
                    timeSec = times[index],
                    kind = if (content == BranchContent.FEATHER) {
                        ObjectKind.FEATHER
                    } else {
                        ObjectKind.COIN
                    }
                )
            }
            repeat(obstacleCount) { index ->
                val kind = when ((index + plan.index + lane.value + config.level).mod(3)) {
                    0 -> ObjectKind.ROCK
                    1 -> ObjectKind.STUMP
                    else -> ObjectKind.LOG
                }
                result += RoadObject(
                    forkIndex = plan.index,
                    lane = lane,
                    timeSec = times[collectibleCount + index],
                    kind = kind
                )
            }
        }
        return result
    }

    private fun previewFork(now: Float): ForkPlan? = plans.firstOrNull {
        !it.choiceCommitted && it.startSec - now in -choiceGrace()..choiceWindow()
    }

    private fun branchX(fork: ForkPlan, lane: Lane, absoluteTime: Float): Float {
        val progress = ((absoluteTime - fork.startSec) / (fork.endSec - fork.startSec))
            .coerceIn(0f, 1f)
        val spread = sin(PI * progress).toFloat()
        return width * 0.5f + lane.value * width * LANE_OFFSET_FRACTION * spread
    }

    private fun screenY(absoluteTime: Float, now: Float): Float =
        playerGroundY() - (absoluteTime - now) * scrollSpeedPx()

    private fun playerGroundY(): Float = height * PLAYER_GROUND_FRACTION

    private fun scrollSpeedPx(): Float =
        width * SCROLL_WIDTHS_PER_SECOND * config.speedMultiplier

    private fun visibleAheadSeconds(): Float =
        if (width <= 0) 6f else playerGroundY() / scrollSpeedPx().coerceAtLeast(1f) + 1.1f

    private fun laneTravelDuration(config: HenLevelConfig): Float =
        (GAME_LANE_ASPECT * (LANE_SPLIT_FRACTION - LANE_MERGE_FRACTION)) /
            (SCROLL_WIDTHS_PER_SECOND * config.speedMultiplier)

    private fun choiceWindow(): Float = if (ability == HenAbility.PreviewFar) 7.2f else 5.2f
    private fun choiceGrace(): Float = if (ability == HenAbility.QuickSwipe) 0.85f else 0.08f
    private fun isJumping(now: Float): Boolean = now - jumpStartedAt in 0f..JUMP_DURATION
    private fun laneMatches(first: Lane, second: Lane): Boolean = first == second
    private fun distanceAt(now: Float): Int =
        (now.coerceAtMost(config.durationSeconds.toFloat()) * 8.5f * config.speedMultiplier)
            .roundToInt()

    private fun elapsedSeconds(): Float {
        if (startedAtMs == 0L) return 0f
        val now = if (paused && pausedAtMs > 0L) pausedAtMs else SystemClock.uptimeMillis()
        return ((now - startedAtMs - accumulatedPauseMs).coerceAtLeast(0L) / 1000f)
    }

    private fun bitmapFor(content: BranchContent): Bitmap = bitmap(
        when (content) {
            BranchContent.FEATHER -> R.drawable.ic_feather
            BranchContent.COIN -> R.drawable.ic_coin
            BranchContent.SAFE -> R.drawable.ic_light_way
            BranchContent.DANGER -> R.drawable.ic_stone
        }
    )

    private fun bitmapFor(kind: ObjectKind): Bitmap = bitmap(
        when (kind) {
            ObjectKind.FEATHER -> R.drawable.icon_feather
            ObjectKind.COIN -> R.drawable.ic_coin
            ObjectKind.ROCK -> R.drawable.ic_stone
            ObjectKind.STUMP -> R.drawable.ic_stump
            ObjectKind.LOG -> R.drawable.ic_trunk
        }
    )

    private fun bitmap(@DrawableRes resource: Int): Bitmap = bitmapCache.getOrPut(resource) {
        requireNotNull(BitmapFactory.decodeResource(resources, resource))
    }

    @DrawableRes
    private fun skinIdleResource(id: String): Int = when (id) {
        "hen_preview" -> R.drawable.skin_blue
        "hen_quick" -> R.drawable.skin_green
        "hen_magnet" -> R.drawable.skin_red
        "hen_auto" -> R.drawable.skin_yellow
        else -> R.drawable.skin_default
    }

    @DrawableRes
    private fun skinRunResource(id: String): Int = when (id) {
        "hen_preview" -> R.drawable.skin_blue_run
        "hen_quick" -> R.drawable.skin_green_run
        "hen_magnet" -> R.drawable.skin_red_run
        "hen_auto" -> R.drawable.skin_yellow_run
        else -> R.drawable.skin_default_run
    }

    private enum class Lane(val value: Int) {
        LEFT(-1), CENTER(0), RIGHT(1)
    }

    private enum class BranchContent { FEATHER, COIN, SAFE, DANGER }

    private enum class ObjectKind(val isObstacle: Boolean) {
        FEATHER(false), COIN(false), ROCK(true), STUMP(true), LOG(true)
    }

    private data class RoadObject(
        val forkIndex: Int,
        val lane: Lane,
        val timeSec: Float,
        val kind: ObjectKind,
        var processed: Boolean = false,
        var collected: Boolean = false
    )

    private data class ForkPlan(
        val index: Int,
        val startSec: Float,
        val endSec: Float,
        val contents: Array<BranchContent>,
        val objects: MutableList<RoadObject> = mutableListOf(),
        var selectedLane: Lane = Lane.CENTER,
        var choiceCommitted: Boolean = false,
        var completed: Boolean = false
    ) {
        fun contentFor(lane: Lane): BranchContent = contents[lane.value + 1]
    }

    companion object {
        private const val PLAYER_GROUND_FRACTION = 0.80f
        private const val SCROLL_WIDTHS_PER_SECOND = 0.235f
        private const val GAME_LANE_ASPECT = 1432f / 824f
        private const val LANE_SPLIT_FRACTION = 0.78f
        private const val LANE_MERGE_FRACTION = 0.20f
        private const val LANE_OFFSET_FRACTION = 0.282f
        private const val JUMP_DURATION = 0.86f
        private val CONFETTI_COLORS = intArrayOf(
            Color.rgb(246, 187, 52),
            Color.rgb(235, 92, 66),
            Color.rgb(104, 166, 68),
            Color.rgb(255, 244, 200),
            Color.rgb(85, 150, 196)
        )
    }
}

data class HenHudState(
    val level: Int,
    val distance: Int,
    val feathers: Int,
    val coins: Int,
    val lives: Int,
    val streak: Int,
    val timeLeftSeconds: Int,
    val selectedLane: Int
)

private fun <T> Array<T>.shuffle(random: Random) {
    for (index in lastIndex downTo 1) {
        val swapIndex = random.nextInt(index + 1)
        val value = this[index]
        this[index] = this[swapIndex]
        this[swapIndex] = value
    }
}

private fun <T> List<T>.shuffled(random: Random): List<T> {
    val copy = toMutableList()
    for (index in copy.lastIndex downTo 1) {
        val swapIndex = random.nextInt(index + 1)
        val value = copy[index]
        copy[index] = copy[swapIndex]
        copy[swapIndex] = value
    }
    return copy
}
