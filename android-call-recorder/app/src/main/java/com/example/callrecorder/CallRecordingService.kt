package com.example.callrecorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Foreground service that records audio for the duration of a call.
 *
 * NOTE on Android 10+ (API 29+): the OS no longer lets third-party apps capture
 * the remote party's voice. MediaRecorder.AudioSource.VOICE_CALL is reserved for
 * system/carrier apps. On stock modern Android this records YOUR microphone only;
 * the other side will be faint or silent. Two-way recording works on Android <= 9,
 * some OEM ROMs, or rooted devices. This is an OS limitation, not a bug.
 */
class CallRecordingService : Service() {

    private var recorder: MediaRecorder? = null
    private var isRecording = false
    private var currentFile: File? = null

    companion object {
        const val ACTION_START = "com.example.callrecorder.START"
        const val ACTION_STOP = "com.example.callrecorder.STOP"
        private const val CHANNEL_ID = "call_recording_channel"
        private const val NOTIFICATION_ID = 42
        private const val TAG = "CallRecordingService"

        /** True while a recording is in progress; read by the UI to show state. */
        @Volatile
        var isActive: Boolean = false
            private set

        /** Folder where recordings are stored (app-private external files dir). */
        fun recordingsDir(context: Context): File {
            val dir = File(context.getExternalFilesDir(null), "recordings")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        if (isRecording) return

        startInForeground()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outFile = File(recordingsDir(this), "call_$timestamp.m4a")
        currentFile = outFile

        // Try audio sources in order; some devices reject VOICE_COMMUNICATION but
        // accept MIC. MIC always works for capturing your own side of the call.
        val sources = intArrayOf(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.MIC
        )

        for (source in sources) {
            try {
                val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(this)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }
                rec.setAudioSource(source)
                rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                rec.setAudioEncodingBitRate(128_000)
                rec.setAudioSamplingRate(44_100)
                rec.setOutputFile(outFile.absolutePath)
                rec.prepare()
                rec.start()

                recorder = rec
                isRecording = true
                isActive = true
                Log.i(TAG, "Recording started: ${outFile.absolutePath} (source=$source)")
                return
            } catch (e: Exception) {
                Log.e(TAG, "Audio source $source failed, trying next", e)
                cleanupRecorder()
            }
        }

        // Every source failed -> give up cleanly so we don't leave a dead service.
        Log.e(TAG, "Could not start recording with any audio source")
        outFile.delete()
        stopForegroundCompat()
        stopSelf()
    }

    private fun stopRecording() {
        if (isRecording) {
            try {
                recorder?.stop()
                Log.i(TAG, "Recording saved: ${currentFile?.absolutePath}")
            } catch (e: Exception) {
                // stop() can throw if it ran for a very short time -> drop the file.
                Log.e(TAG, "Error stopping recorder", e)
                currentFile?.delete()
            }
        }
        cleanupRecorder()
        stopForegroundCompat()
        stopSelf()
    }

    private fun cleanupRecorder() {
        try {
            recorder?.reset()
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        isRecording = false
        isActive = false
    }

    private fun startInForeground() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompatBuilder()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @Suppress("DEPRECATION")
    private fun NotificationCompatBuilder(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }
        return builder
            .setContentTitle(getString(R.string.recording_title))
            .setContentText(getString(R.string.recording_text))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        cleanupRecorder()
        super.onDestroy()
    }
}
