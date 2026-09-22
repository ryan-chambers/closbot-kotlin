package com.ryanthink.closbotkt

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ryanthink.closbotkt.app.ClosBotApp
import com.ryanthink.closbotkt.core.di.Greeter
import com.ryanthink.closbotkt.ui.theme.ClosBotTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var greeter: Greeter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", greeter.greet())
        enableEdgeToEdge()
        setContent {
            ClosBotTheme {
                ClosBotApp()
            }
        }
    }
}
