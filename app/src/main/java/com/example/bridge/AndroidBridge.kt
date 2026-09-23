package com.example.bridge

import android.webkit.JavascriptInterface
import com.example.model.ActionResult
import org.json.JSONArray
import org.json.JSONObject

class AndroidBridge(
    private val actionManager: AndroidActionManager,
    private val onActionResult: (ActionResult) -> Unit
) {

    @JavascriptInterface
    fun isNativeBridgeAvailable(): Boolean {
        return true
    }

    @JavascriptInterface
    fun openWhatsApp(): String {
        val result = actionManager.openWhatsApp()
        onActionResult(result)
        return resultToJson(result)
    }

    @JavascriptInterface
    fun openApp(appName: String): String {
        val result = actionManager.openApp(appName)
        onActionResult(result)
        return resultToJson(result)
    }

    @JavascriptInterface
    fun makeCall(phoneNumber: String): String {
        val result = actionManager.makeCall(phoneNumber)
        onActionResult(result)
        return resultToJson(result)
    }

    @JavascriptInterface
    fun callContact(contactName: String): String {
        val result = actionManager.callContact(contactName)
        onActionResult(result)
        return resultToJson(result)
    }

    @JavascriptInterface
    fun openUrl(url: String): String {
        val result = actionManager.openUrl(url)
        onActionResult(result)
        return resultToJson(result)
    }

    private fun resultToJson(result: ActionResult): String {
        val json = JSONObject()
        json.put("actionType", result.actionType)
        json.put("isSuccess", result.isSuccess)
        json.put("message", result.message)
        if (result.details != null) {
            json.put("details", result.details)
        }
        if (result.matchedContacts.isNotEmpty()) {
            val arr = JSONArray()
            for (contact in result.matchedContacts) {
                val obj = JSONObject()
                obj.put("name", contact.name)
                obj.put("phoneNumber", contact.phoneNumber)
                arr.put(obj)
            }
            json.put("matchedContacts", arr)
        }
        return json.toString()
    }
}
