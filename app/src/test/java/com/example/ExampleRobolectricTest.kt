package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.bridge.AndroidActionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Arushi", appName)
  }

  @Test
  fun `action manager handles make call formatting`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val actionManager = AndroidActionManager(context)
    val result = actionManager.makeCall("9876543210")
    assertNotNull(result)
    assertTrue(result.message.contains("9876543210"))
  }

  @Test
  fun `action manager handles empty contact name safely`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val actionManager = AndroidActionManager(context)
    val result = actionManager.callContact("")
    assertEquals(false, result.isSuccess)
  }

  @Test
  fun `speech recognizer state combines partial and final text correctly`() {
    val state = com.example.speech.SpeechRecognizerState(
      partialText = "kholo",
      finalText = "WhatsApp"
    )
    assertEquals("WhatsApp kholo", state.displayLiveText)
  }

  @Test
  fun `speech recognizer manager initializes and supports languages`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.speech.AndroidSpeechRecognizerManager(context)
    manager.setLanguage("hi-IN")
    assertEquals("hi-IN", manager.state.value.selectedLanguage)
    manager.setContinuousMode(true)
    assertTrue(manager.state.value.continuousMode)
    manager.destroy()
  }

  @Test
  fun `android bridge returns native bridge formatted json`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val actionManager = AndroidActionManager(context)
    val bridge = com.example.bridge.AndroidBridge(actionManager) {}

    assertTrue(bridge.isNativeBridgeAvailable())

    val callJson = org.json.JSONObject(bridge.makeCall("9876543210"))
    assertEquals("success", callJson.getString("status"))
    assertTrue(callJson.has("data"))

    val contactJson = org.json.JSONObject(bridge.callContact(""))
    assertEquals("not_found", contactJson.getString("status"))
    assertTrue(contactJson.has("data"))
  }
}
