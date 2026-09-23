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
        val status = when (result.actionType) {
            "callContact" -> when {
                result.matchedContacts.size == 1 && result.isSuccess -> "matched"
                result.matchedContacts.size > 1 -> "multiple"
                result.matchedContacts.isEmpty() -> "not_found"
                result.isSuccess -> "matched"
                else -> "not_found"
            }
            else -> if (result.isSuccess) "success" else "failed"
        }
        json.put("status", status)

        val dataArr = JSONArray()
        for (contact in result.matchedContacts) {
            val obj = JSONObject()
            obj.put("name", contact.name)
            obj.put("phoneNumber", contact.phoneNumber)
            dataArr.put(obj)
        }
        json.put("data", dataArr)
        json.put("matchedContacts", dataArr)

        json.put("actionType", result.actionType)
        json.put("isSuccess", result.isSuccess)
        json.put("message", result.message)
        if (!result.isSuccess) {
            json.put("reason", result.message)
        }
        if (result.details != null) {
            json.put("details", result.details)
        }
        return json.toString()
    }
}
