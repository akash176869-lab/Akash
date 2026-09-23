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
}
