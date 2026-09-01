package threeway.henroute.orchard.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import threeway.henroute.orchard.R

class SoundManager(private val context: Context) {

    enum class MusicTrack { Menu, Game }
    enum class SoundEffect { Jump, Swipe, Feather, Coin, Hit, Fork, Win, Lose, Error, Click }

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val effects = mutableMapOf<SoundEffect, Int>()
    private val loadedSoundIds = mutableSetOf<Int>()
    private var runLoopSound: Int = 0

    private var musicPlayer: MediaPlayer? = null
    private var currentTrack: MusicTrack? = null
    private var runStreamId: Int = 0
    private var pendingRunLoop = false
    private var soundEnabled = true
    private var musicEnabled = false
    private var soundVolume = 0.9f
    private var musicVolume = 0.75f
    private var appInForeground = true

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                synchronized(loadedSoundIds) {
                    loadedSoundIds += sampleId
                }
                if (sampleId == runLoopSound && pendingRunLoop) {
                    startRunLoopIfReady()
                }
            }
        }

        effects[SoundEffect.Jump] = soundPool.load(context, R.raw.sfx_jump, 1)
        effects[SoundEffect.Swipe] = soundPool.load(context, R.raw.sfx_swipe, 1)
        effects[SoundEffect.Feather] = soundPool.load(context, R.raw.sfx_feather, 1)
        effects[SoundEffect.Coin] = soundPool.load(context, R.raw.sfx_coin, 1)
        effects[SoundEffect.Hit] = soundPool.load(context, R.raw.sfx_hit, 1)
        effects[SoundEffect.Fork] = soundPool.load(context, R.raw.sfx_fork, 1)
        effects[SoundEffect.Win] = soundPool.load(context, R.raw.sfx_win, 1)
        effects[SoundEffect.Lose] = soundPool.load(context, R.raw.sfx_lose, 1)
        effects[SoundEffect.Error] = soundPool.load(context, R.raw.sfx_error, 1)
        effects[SoundEffect.Click] = soundPool.load(context, R.raw.sfx_click, 1)
        runLoopSound = soundPool.load(context, R.raw.sfx_run_loop, 1)
    }

    fun applySettings(
        soundEnabled: Boolean,
        musicEnabled: Boolean,
        soundVolume: Float,
        musicVolume: Float
    ) {
        this.soundEnabled = soundEnabled
        this.musicEnabled = musicEnabled
        this.soundVolume = soundVolume.coerceIn(0f, 1f)
        this.musicVolume = musicVolume.coerceIn(0f, 1f)

        musicPlayer?.setVolume(this.musicVolume, this.musicVolume)
        if (!musicEnabled) {
            musicPlayer?.pause()
        } else {
            val requestedTrack = currentTrack
            if (musicPlayer == null && requestedTrack != null && appInForeground) {
                playMusic(requestedTrack)
            } else {
                resumeMusic()
            }
        }

        if (!soundEnabled) {
            stopRunLoop()
        } else if (pendingRunLoop) {
            startRunLoopIfReady()
        }
    }

    fun playMusic(track: MusicTrack) {
        val canReuseCurrentPlayer = currentTrack == track && musicPlayer != null
        currentTrack = track

        if (!musicEnabled || !appInForeground) return

        if (canReuseCurrentPlayer) {
            musicPlayer?.apply {
                setVolume(musicVolume, musicVolume)
                if (!isPlaying) start()
            }
            return
        }

        val resource = when (track) {
            MusicTrack.Menu -> R.raw.music_menu
            MusicTrack.Game -> R.raw.music_game
        }

        musicPlayer?.release()
        musicPlayer = MediaPlayer.create(context, resource)?.apply {
            isLooping = true
            setVolume(musicVolume, musicVolume)
            start()
        }
    }

    fun playEffect(effect: SoundEffect) {
        if (!soundEnabled) return
        val id = effects[effect] ?: return
        if (!isLoaded(id)) return
        soundPool.play(id, soundVolume, soundVolume, 1, 0, 1f)
    }

    fun startRunLoop() {
        pendingRunLoop = true
        startRunLoopIfReady()
    }

    private fun startRunLoopIfReady() {
        if (!pendingRunLoop || !soundEnabled || runStreamId != 0 || !isLoaded(runLoopSound)) return
        runStreamId = soundPool.play(
            runLoopSound,
            soundVolume * 0.35f,
            soundVolume * 0.35f,
            0,
            -1,
            1f
        )
    }

    fun stopRunLoop() {
        pendingRunLoop = false
        if (runStreamId != 0) {
            soundPool.stop(runStreamId)
        }
        runStreamId = 0
    }

    fun pauseMusic() {
        appInForeground = false
        musicPlayer?.takeIf { it.isPlaying }?.pause()
        stopRunLoop()
    }

    fun resumeMusic() {
        appInForeground = true
        if (!musicEnabled) return

        val player = musicPlayer
        if (player != null) {
            if (!player.isPlaying) player.start()
        } else {
            currentTrack?.let(::playMusic)
        }
    }

    fun release() {
        stopRunLoop()
        musicPlayer?.release()
        musicPlayer = null
        soundPool.release()
        synchronized(loadedSoundIds) {
            loadedSoundIds.clear()
        }
    }

    private fun isLoaded(soundId: Int): Boolean = synchronized(loadedSoundIds) {
        soundId != 0 && soundId in loadedSoundIds
    }
}
