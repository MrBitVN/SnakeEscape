package com.snakeescape.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.snakeescape.R

class SoundManager(private val context: Context) {

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(audioAttributes)
        .build()

    private var tapSoundId: Int = 0
    private var escapeSoundId: Int = 0
    private var blockedSoundId: Int = 0
    private var winSoundId: Int = 0

    private var mediaPlayer: MediaPlayer? = null
    var isSoundEnabled: Boolean = true
    var isMusicEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) {
                pauseMusic()
            } else {
                resumeMusic()
            }
        }

    init {
        try {
            tapSoundId = soundPool.load(context, R.raw.sfx_tap, 1)
            escapeSoundId = soundPool.load(context, R.raw.sfx_escape, 1)
            blockedSoundId = soundPool.load(context, R.raw.sfx_blocked, 1)
            winSoundId = soundPool.load(context, R.raw.sfx_win, 1)
        } catch (_: Exception) {}
    }

    fun playTap() {
        if (isSoundEnabled && tapSoundId != 0) {
            soundPool.play(tapSoundId, 0.7f, 0.7f, 1, 0, 1.0f)
        }
    }

    fun playEscape() {
        if (isSoundEnabled && escapeSoundId != 0) {
            soundPool.play(escapeSoundId, 0.9f, 0.9f, 2, 0, 1.0f)
        }
    }

    fun playBlocked() {
        if (isSoundEnabled && blockedSoundId != 0) {
            soundPool.play(blockedSoundId, 0.9f, 0.9f, 2, 0, 1.0f)
        }
    }

    fun playWin() {
        if (isSoundEnabled && winSoundId != 0) {
            soundPool.play(winSoundId, 1.0f, 1.0f, 3, 0, 1.0f)
        }
    }

    fun startMusic() {
        if (!isMusicEnabled) return
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(context, R.raw.bgm_loop)?.apply {
                    isLooping = true
                    setVolume(0.4f, 0.4f)
                    start()
                }
            } else if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
        } catch (_: Exception) {}
    }

    fun pauseMusic() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (_: Exception) {}
    }

    fun resumeMusic() {
        if (isMusicEnabled) {
            startMusic()
        }
    }

    fun release() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            soundPool.release()
        } catch (_: Exception) {}
    }
}
