package com.example.callrecorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/**
 * Starts recording when a call becomes active (OFFHOOK) and stops it when the
 * call ends (IDLE). Works for both incoming and outgoing calls.
 */
class CallStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)

        when (state) {
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Call is active -> try to start the recording service.
                // NOTE: On Android 12+ (API 31+) starting a microphone foreground
                // service from a background broadcast is disallowed and throws
                // ForegroundServiceStartNotAllowedException. We catch it so the
                // app doesn't crash; use the in-app "Start recording" button in
                // that case. On Android <= 11 this auto-start works.
                val serviceIntent = Intent(context, CallRecordingService::class.java).apply {
                    action = CallRecordingService.ACTION_START
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(context, serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("CallStateReceiver", "Auto-start blocked by OS", e)
                }
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                // Call ended -> stop recording.
                val serviceIntent = Intent(context, CallRecordingService::class.java).apply {
                    action = CallRecordingService.ACTION_STOP
                }
                try {
                    context.startService(serviceIntent)
                } catch (e: Exception) {
                    android.util.Log.w("CallStateReceiver", "Stop failed", e)
                }
            }
            // EXTRA_STATE_RINGING: ignored; we only record once the call connects.
        }
    }
}
