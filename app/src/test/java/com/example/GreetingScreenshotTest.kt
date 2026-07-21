package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ZipDatabase
import com.example.data.ZipRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ZipVaultDashboard
import com.example.ui.ZipViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()
  
  private lateinit var database: ZipDatabase
  private lateinit var repository: ZipRepository
  private lateinit var viewModel: ZipViewModel

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    database = Room.inMemoryDatabaseBuilder(context, ZipDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = ZipRepository(database.zipDao())
    viewModel = ZipViewModel(repository)
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        ZipVaultDashboard(viewModel = viewModel)
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
