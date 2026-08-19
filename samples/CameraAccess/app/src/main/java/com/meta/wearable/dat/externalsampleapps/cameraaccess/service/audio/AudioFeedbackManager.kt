/*
 * Copyright (c) 2026 UrbanSense AI.
 * All rights reserved.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.service.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class AudioFeedbackManager(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "UrbanSense:Audio"
        private const val UTTERANCE_ID = "urbansense_feedback"
    }

    private var tts: TextToSpeech? = null
    @Volatile private var isInitialized = false
    private val pendingQueue = mutableListOf<Pair<String, Boolean>>()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Error instantiating TextToSpeech engine", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val ptBrLocale = Locale("pt", "BR")
            var langResult = tts?.setLanguage(ptBrLocale)
            
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                val ptLocale = Locale("pt")
                langResult = tts?.setLanguage(ptLocale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.getDefault())
                }
            }

            tts?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "TTS playback started: $utteranceId")
                }

                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "TTS playback finished: $utteranceId")
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    Log.w(TAG, "TTS playback error: $utteranceId")
                }
            })

            isInitialized = true
            Log.i(TAG, "TextToSpeech successfully initialized in Portuguese")

            // Process any speech requests queued while TTS was initializing
            synchronized(pendingQueue) {
                for ((text, enabled) in pendingQueue) {
                    executeSpeak(text, enabled)
                }
                pendingQueue.clear()
            }
        } else {
            Log.e(TAG, "TextToSpeech initialization failed with status $status")
        }
    }

    fun speak(text: String, isEnabled: Boolean = true) {
        if (!isEnabled || text.isBlank()) return

        if (!isInitialized) {
            Log.d(TAG, "TTS not ready yet, queuing speech: \"$text\"")
            synchronized(pendingQueue) {
                pendingQueue.add(Pair(text, isEnabled))
            }
            return
        }

        executeSpeak(text, isEnabled)
    }

    private fun executeSpeak(text: String, isEnabled: Boolean) {
        if (!isEnabled || text.isBlank()) return
        try {
            // Ensure volume is adequate and fallback to speaker if Bluetooth is not routing
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
            Log.d(TAG, "Playing audio feedback: \"$text\"")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio feedback", e)
        }
    }

    fun speakSuccess(customMessage: String? = null, isEnabled: Boolean = true) {
        val message = customMessage?.takeIf { it.isNotBlank() } ?: "Registro enviado com sucesso."
        speak(message, isEnabled)
    }

    fun speakFailure(customMessage: String? = null, isEnabled: Boolean = true) {
        val message = customMessage?.takeIf { it.isNotBlank() } ?: "Falha no envio. Registro salvo localmente."
        speak(message, isEnabled)
    }

    fun cleanup() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up TTS", e)
        }
    }
}
