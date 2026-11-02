package com.ryanthink.closbotkt

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dagger.hilt.android.testing.HiltAndroidRule
import org.junit.Rule

/**
 * Base for tests that drive the whole app through [MainActivity]. Subclasses still need `@HiltAndroidTest`, which Hilt
 * reads from the concrete test class, and call `hiltRule.inject()` if they have `@Inject` fields.
 */
abstract class HiltComposeTest {

    // Hilt's rule must run first: it builds the dependency graph that the activity then starts with.
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()

    protected fun text(id: Int, vararg args: Any) = composeRule.activity.getString(id, *args)
}
