package com.dk.luminajournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.dk.luminajournal.data.database.ImageToDeleteDao
import com.dk.luminajournal.data.database.ImageToUploadDao
import com.dk.luminajournal.navigation.Screen
import com.dk.luminajournal.navigation.SetupNavGraph
import com.dk.luminajournal.ui.theme.LuminaJournalTheme
import com.dk.luminajournal.util.Constants.APP_ID
import com.dk.luminajournal.util.retryDeletingImageFromFirebase
import com.dk.luminajournal.util.retryUploadingImageToFirebase
import com.google.firebase.FirebaseApp
import dagger.hilt.android.AndroidEntryPoint
import io.realm.kotlin.mongodb.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var imageToUploadDao: ImageToUploadDao

    @Inject
    lateinit var imageToDeleteDao: ImageToDeleteDao

    private var keepSplashOpened = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().setKeepOnScreenCondition { keepSplashOpened }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        FirebaseApp.initializeApp(applicationContext)
        setContent {
            LuminaJournalTheme {
                val navController = rememberNavController()
                SetupNavGraph(
                    startDestination = getStartDestination(),
                    navController = navController,
                    onDataLoaded = {
                        keepSplashOpened = false
                    }
                )
            }
        }

        cleanupCheck(
            scope = lifecycleScope,
            imageToUploadDao = imageToUploadDao,
            imageToDeleteDao = imageToDeleteDao
        )
    }
}

private fun cleanupCheck(
    scope: CoroutineScope,
    imageToUploadDao: ImageToUploadDao,
    imageToDeleteDao: ImageToDeleteDao
){
    scope.launch(Dispatchers.IO) {
        val imagesToUpload = imageToUploadDao.getAllImages()
        imagesToUpload.forEach { imageToUpload ->
            retryUploadingImageToFirebase(
                imageToUpload = imageToUpload,
                onSuccess = {
                    scope.launch(Dispatchers.IO){
                        imageToUploadDao.cleanupImage(imageId = imageToUpload.id)
                    }
                }
            )
        }

        val imagesToDelete = imageToDeleteDao.getAllImages()
        imagesToDelete.forEach { imageToDelete ->
            retryDeletingImageFromFirebase(
                imageToDelete = imageToDelete,
                onSuccess = {
                    scope.launch(Dispatchers.IO){
                        imageToDeleteDao.cleanupImage(imageId = imageToDelete.id)
                    }
                }
            )
        }
    }

}

private fun getStartDestination(): String{
    val user = App.create(appId = APP_ID).currentUser
    return if(user != null && user.loggedIn) Screen.Home.route
    else Screen.Authentication.route
}