package com.dk.luminajournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.dk.luminajournal.navigation.Screen
import com.dk.luminajournal.navigation.SetupNavGraph
import com.dk.luminajournal.ui.theme.LuminaJournalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContent {
            LuminaJournalTheme {
                val navController = rememberNavController()
                SetupNavGraph(
                    startDestination = Screen.Authentication.route,
                    navController = navController
                )
            }
        }
    }
}