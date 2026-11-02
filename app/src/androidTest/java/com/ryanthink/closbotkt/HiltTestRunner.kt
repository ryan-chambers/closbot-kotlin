package com.ryanthink.closbotkt

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Starts every instrumented test in [HiltTestApplication] instead of [ClosBotApplication]. Hilt builds a fresh
 * dependency graph for each test from that application, which is what lets a test replace a module (see
 * `TestNotesDatabaseModule`).
 */
class HiltTestRunner : AndroidJUnitRunner() {

    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
