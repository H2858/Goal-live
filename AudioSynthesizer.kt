package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.sin

object AudioSynthesizer {
    private val random = Random()
    private val scope = CoroutineScope(Dispatchers.Default)
    private var activeTrack: AudioTrack? = null
    var isMuted = false

    fun playGoalCheer() {
        if (isMuted) return
        
        scope.launch {
            try {
                stopAll()

                val sampleRate = 22050
                val durationSec = 3.5
                val numSamples = (sampleRate * durationSec).toInt()
                val buffer = FloatArray(numSamples)

                var lastVal = 0f
                for (i in 0 until numSamples) {
                    val progress = i.toFloat() / numSamples
                    val envelope = if (progress < 0.15f) {
                        progress / 0.15f
                    } else {
                        1.0f - ((progress - 0.15f) / 0.85f)
                    }

                    val noise = random.nextFloat() * 2f - 1f
                    
                    val filterCoeff = 0.08f + 0.12f * envelope
                    val filteredNoise = lastVal + filterCoeff * (noise - lastVal)
                    lastVal = filteredNoise

                    var fanfare = 0f
                    val notes = floatArrayOf(261.63f, 329.63f, 392.00f, 523.25f, 659.25f, 783.99f) // C4, E4, G4, C5, E5, G5
                    if (progress in 0.1f..0.8f) {
                        val noteProgress = (progress - 0.1f) / 0.7f
                        val noteIndex = (noteProgress * notes.size).toInt().coerceIn(0, notes.lastIndex)
                        val freq = notes[noteIndex]
                        fanfare = sin(2.0 * Math.PI * freq * (i.toDouble() / sampleRate)).toFloat() * 0.12f
                        
                        val vibrato = sin(2.0 * Math.PI * 6.0 * (i.toDouble() / sampleRate)).toFloat() * 0.02f
                        fanfare += sin(2.0 * Math.PI * (freq * 1.5) * (i.toDouble() / sampleRate) + vibrato).toFloat() * 0.04f
                    }

                    buffer[i] = (filteredNoise * 0.65f + fanfare * 0.35f) * envelope * 0.8f
                }

                val pcmBuffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    pcmBuffer[i] = (buffer[i].coerceIn(-1.0f, 1.0f) * Short.MAX_VALUE).toInt().toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(numSamples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcmBuffer, 0, numSamples)
                track.play()
                activeTrack = track

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopAll() {
        try {
            activeTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
            activeTrack = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
