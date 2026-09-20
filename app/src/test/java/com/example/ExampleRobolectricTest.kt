package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.fitpal.R
import com.example.util.SecurityUtils
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("FitPal", appName)
  }

  @Test
  fun `test passcode security lifecycle`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    // 1. Verify passcode is initially disabled
    assertFalse(SecurityUtils.isPasscodeEnabled(context))

    // 2. Enable passcode with a secure PIN "1234"
    SecurityUtils.enablePasscode(context, "1234")
    assertTrue(SecurityUtils.isPasscodeEnabled(context))

    // 3. Verify PIN verification works correctly
    assertTrue(SecurityUtils.verifyPin(context, "1234"))
    assertFalse(SecurityUtils.verifyPin(context, "5555")) // Incorrect PIN

    // 4. Verify cryptographic hash is not stored in plaintext
    val sp = context.getSharedPreferences("fitpal_settings", Context.MODE_PRIVATE)
    val storedHash = sp.getString("passcode_hash", "") ?: ""
    assertNotEquals("1234", storedHash)
    assertTrue(storedHash.isNotEmpty())

    // 5. Disable passcode and verify state is cleared
    SecurityUtils.disablePasscode(context)
    assertFalse(SecurityUtils.isPasscodeEnabled(context))
    assertFalse(SecurityUtils.verifyPin(context, "1234"))
  }
}
