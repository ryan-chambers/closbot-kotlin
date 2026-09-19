package com.ryanthink.closbotkt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ryanthink.closbotkt.app.ClosBotApp
import com.ryanthink.closbotkt.ui.theme.ClosBotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClosBotTheme {
                ClosBotApp()
            }
        }
    }
}
