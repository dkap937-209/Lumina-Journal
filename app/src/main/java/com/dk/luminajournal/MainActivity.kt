package com.dk.luminajournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.dk.luminajournal.data.repository.MongoDB
import com.dk.luminajournal.navigation.Screen
import com.dk.luminajournal.navigation.SetupNavGraph
import com.dk.luminajournal.ui.theme.LuminaJournalTheme
import com.dk.luminajournal.util.Constants.APP_ID
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClients
import io.realm.kotlin.mongodb.App

class MainActivity : ComponentActivity() {

    var keepSplashOpened = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().setKeepOnScreenCondition { keepSplashOpened }
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            LuminaJournalTheme {
                val navController = rememberNavController()
                SetupNavGraph(
                    startDestination = getStartDestination(),
                    navController = navController,
                    onDataLoaded = {
                        println("onDataLoaded Called")
                        keepSplashOpened = false
                    }
                )
            }
        }
    }
}

private fun getStartDestination(): String{
    val user = App.create(appId = APP_ID).currentUser
    return if(user != null && user.loggedIn) Screen.Home.route
    else Screen.Authentication.route
}