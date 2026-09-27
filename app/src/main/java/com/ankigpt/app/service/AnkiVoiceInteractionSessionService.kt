package com.ankigpt.app.service

import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

class AnkiVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return AnkiVoiceSession(this)
    }
}

class AnkiVoiceSession(context: VoiceInteractionSessionService) : VoiceInteractionSession(context) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("EXTRA_VOICE_ASSISTANT_INVOCATION", true)
        }
        if (intent != null) {
            context.startActivity(intent)
        }
        hide()
    }
}
