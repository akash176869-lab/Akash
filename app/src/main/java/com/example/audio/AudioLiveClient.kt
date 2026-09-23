package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

class AudioLiveClient(
    private val scope: CoroutineScope,
    private val onAudioChunkRecorded: (String) -> Unit,
    private val onInputVolumeChanged: (Float) -> Unit,
    private val onOutputVolumeChanged: (Float) -> Unit,
    private val onPlaybackStateChanged: (Boolean) -> Unit
) {
    companion object {
        const val SAMPLE_RATE_IN = 16000
        const val SAMPLE_RATE_OUT = 24000
    }

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    private var recordJob: Job? = null
    private var playJob: Job? = null

    private val isRecording = AtomicBoolean(false)
    private val isPlaying = AtomicBoolean(false)

    private val playbackQueue = ConcurrentLinkedQueue<ByteArray>()

    @SuppressLint("MissingPermission")
    fun startRecording(): Boolean {
        if (isRecording.get()) return true

        val minBufSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_IN,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufSize <= 0) return false

        val bufferSize = (minBufSize * 2).coerceAtLeast(4096)

        return try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE_IN,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return false
            }

            audioRecord = record
            record.startRecording()
            isRecording.set(true)

            recordJob = scope.launch(Dispatchers.IO) {
                val shortBuffer = ShortArray(1024)
                val byteBuffer = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN)

                while (isActive && isRecording.get()) {
                    val readShorts = record.read(shortBuffer, 0, shortBuffer.size)
                    if (readShorts > 0) {
                        byteBuffer.clear()
                        var sumSq = 0.0
                        for (i in 0 until readShorts) {
                            val sample = shortBuffer[i]
                            byteBuffer.putShort(sample)
                            sumSq += (sample.toDouble() * sample.toDouble())
                        }
                        val rms = sqrt(sumSq / readShorts)
                        val normalized = (rms / 8000.0).coerceIn(0.0, 1.0).toFloat()
                        onInputVolumeChanged(normalized)

                        val pcmBytes = ByteArray(readShorts * 2)
                        byteBuffer.position(0)
                        byteBuffer.get(pcmBytes)

                        val base64 = Base64.encodeToString(pcmBytes, Base64.NO_WRAP)
                        onAudioChunkRecorded(base64)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun stopRecording() {
        isRecording.set(false)
        recordJob?.cancel()
        recordJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioRecord = null
        onInputVolumeChanged(0f)
    }

    fun initAudioTrack(): Boolean {
        if (audioTrack != null) return true

        val minBufSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE_OUT,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = (minBufSize * 2).coerceAtLeast(8192)

        return try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE_OUT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            if (track.state != AudioTrack.STATE_INITIALIZED) {
                track.release()
                return false
            }

            track.play()
            audioTrack = track
            isPlaying.set(true)
            startPlaybackWorker()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun startPlaybackWorker() {
        playJob?.cancel()
        playJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val chunk = playbackQueue.poll()
                if (chunk != null) {
                    onPlaybackStateChanged(true)
                    calculateAndEmitOutputVolume(chunk)
                    audioTrack?.write(chunk, 0, chunk.size)
                } else {
                    onPlaybackStateChanged(false)
                    onOutputVolumeChanged(0f)
                    kotlinx.coroutines.delay(10)
                }
            }
        }
    }

    private fun calculateAndEmitOutputVolume(chunk: ByteArray) {
        val shortCount = chunk.size / 2
        if (shortCount == 0) return
        var sumSq = 0.0
        val buffer = ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until shortCount) {
            val sample = buffer.short
            sumSq += (sample.toDouble() * sample.toDouble())
        }
        val rms = sqrt(sumSq / shortCount)
        val normalized = (rms / 9000.0).coerceIn(0.0, 1.0).toFloat()
        onOutputVolumeChanged(normalized)
    }

    fun enqueueAudioData(pcmBytes: ByteArray) {
        if (audioTrack == null) {
            initAudioTrack()
        }
        playbackQueue.offer(pcmBytes)
    }

    fun stopAndFlushAudio() {
        playbackQueue.clear()
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onOutputVolumeChanged(0f)
        onPlaybackStateChanged(false)
    }

    fun release() {
        stopRecording()
        isPlaying.set(false)
        playJob?.cancel()
        playJob = null
        playbackQueue.clear()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioTrack = null
    }
}
