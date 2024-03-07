package com.twa.example

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.browser.trusted.TrustedWebActivityIntentBuilder

private const val TAG = "TWAManager"
private const val HOME_URL = "YOUR_URL"
private const val URL_PAGE_2 = "$HOME_URL/nav-test"

class TWAManager(private val context: Context) {
    private var client: CustomTabsClient? = null
    private var session: CustomTabsSession? = null

    private val targetOrigin = Uri.parse(HOME_URL)

    private var validated = false

    private val customTabsCallback: CustomTabsCallback = object : CustomTabsCallback() {
        override fun onRelationshipValidationResult(
            relation: Int, requestedOrigin: Uri,
            result: Boolean, extras: Bundle?
        ) {
            Log.d(TAG, "Relationship result: $result")
            validated = result
        }

        override fun onNavigationEvent(navigationEvent: Int, extras: Bundle?) {
            if (!validated) {
                if (navigationEvent != NAVIGATION_FINISHED) {
                    return
                }

                val result =
                    session?.requestPostMessageChannel(targetOrigin)
                Log.d(TAG, "Requested Post Message Channel: $result")
            }
        }

        override fun onMessageChannelReady(extras: Bundle?) {
            Log.d(TAG, "Message channel ready.")
            val result = session?.postMessage("{\"message\":\"First message\"}", null)
            Log.d(TAG, "postMessage returned: $result")
        }

        override fun onPostMessage(message: String, extras: Bundle?) {
            super.onPostMessage(message, extras)

            MessageHandler(this@TWAManager).handle(message)
        }
    }

    fun launch(afterLaunch: (CustomTabsSession) -> Unit = {}) {
        val packageName = CustomTabsClient.getPackageName(context, null)

        CustomTabsClient.bindCustomTabsService(
            context,
            packageName,
            object : CustomTabsServiceConnection() {
                override fun onCustomTabsServiceConnected(
                    name: ComponentName,
                    client: CustomTabsClient
                ) {
                    this@TWAManager.client = client

                    client.warmup(0L)
                    session = this@TWAManager.client?.newSession(customTabsCallback)
                    session?.let {
                        val uri = Uri.parse(HOME_URL)

                        TrustedWebActivityIntentBuilder(uri)
                            .build(it)
                            .launchTrustedWebActivity(context)

                        afterLaunch(it)
                    }
                }

                override fun onServiceDisconnected(componentName: ComponentName) {
                    client = null
                }
            })
    }

    fun sendNavigationMessage() {
        val msg = "{\"navigateTo\":\"$URL_PAGE_2\",\"time\":\"${System.currentTimeMillis()}\"}"
        Log.d(TAG, "sending message: $msg")
        session?.postMessage(msg, null)
    }
}