package top.etta.aerie.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import top.etta.aerie.MainActivity

@RunWith(AndroidJUnit4::class)
class MainActivityComposeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appShellStartsAndDisplaysTheProductName() {
        composeRule.onNodeWithText("Aerie 云栖").assertIsDisplayed()
    }
}
