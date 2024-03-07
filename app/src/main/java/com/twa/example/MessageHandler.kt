package com.twa.example

import android.util.Log

private const val TAG = "MessageHandler"

class MessageHandler(private val twaManager: TWAManager) {
    fun handle(message: String) {
        Log.d(TAG, "message received: $message")
        if (message.contains("navigation command")) {
            twaManager.sendNavigationMessage()
        }
    }
}



