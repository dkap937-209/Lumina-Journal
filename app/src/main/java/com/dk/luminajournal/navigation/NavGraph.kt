package com.dk.luminajournal.navigation

import android.widget.Toast
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dk.luminajournal.data.repository.MongoDB
import com.dk.luminajournal.model.GalleryImage
import com.dk.luminajournal.model.Mood
import com.dk.luminajournal.presentation.components.DisplayAlertDialog
import com.dk.luminajournal.presentation.screens.auth.AuthenticationScreen
import com.dk.luminajournal.presentation.screens.auth.AuthenticationViewModel
import com.dk.luminajournal.presentation.screens.home.HomeScreen
import com.dk.luminajournal.presentation.screens.home.HomeViewModel
import com.dk.luminajournal.presentation.screens.write.WriteScreen
import com.dk.luminajournal.presentation.screens.write.WriteViewModel
import com.dk.luminajournal.util.Constants.APP_ID
import com.dk.luminajournal.util.Constants.WRITE_SCREEN_ARGUMENT_KEY
import com.dk.luminajournal.model.RequestState
import com.dk.luminajournal.model.rememberGalleryState
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState
import com.stevdzasan.messagebar.rememberMessageBarState
import com.stevdzasan.onetap.rememberOneTapSignInState
import io.realm.kotlin.mongodb.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SetupNavGraph(
    startDestination: String,
    navController: NavHostController,
    onDataLoaded: () -> Unit
) {
    NavHost(
        startDestination = startDestination,
        navController = navController
    ){
        authenticationRoute(
            navigateToHome = {
                navController.popBackStack()
                navController.navigate(Screen.Home.route)
            },
            onDataLoaded = onDataLoaded
        )
        homeRoute(
            navigateToWrite = {
                navController.navigate(Screen.Write.route)
            },
            navigateToAuth = {
                navController.popBackStack()
                navController.navigate(Screen.Authentication.route)
            },
            onDataLoaded = onDataLoaded,
            navigateToWriteWithArgs = { id ->
                navController.navigate(Screen.Write.passDiaryId(diaryId = id))
            }
        )
        writeRoute(
            onBackPressed = {
                navController.popBackStack()
            }
        )
    }
    
}

fun NavGraphBuilder.authenticationRoute(
    navigateToHome: () -> Unit,
    onDataLoaded: () -> Unit
){
    composable(route = Screen.Authentication.route){
        val viewModel: AuthenticationViewModel = viewModel()
        val authenticated = viewModel.authenticated
        val loadingState by viewModel.loadingState
        val oneTapState = rememberOneTapSignInState()
        val messageBarState = rememberMessageBarState()

        LaunchedEffect(key1 = Unit){
            onDataLoaded()
        }

        AuthenticationScreen(
            authenticated = authenticated.value,
            loadingState = loadingState,
            oneTapState = oneTapState,
            onButtonClicked = {
                oneTapState.open()
                viewModel.setLoading(
                    loading = true
                )
            },
            messageBarState = messageBarState,
            onTokenIdReceived = {tokenId ->
                viewModel.signInWithMongoAtlas(
                    tokenId = tokenId,
                    onSuccess = {
                        messageBarState.addSuccess("Successfully signed in")
                        viewModel.setLoading(false)
                    },
                    onError = {
                        messageBarState.addError(Exception(it))
                        viewModel.setLoading(false)
                    }
                )
            },
            onDialogDismissed = { message ->
                messageBarState.addError(Exception(message))
                viewModel.setLoading(false)
            },
            navigateToHome = navigateToHome
        )
    }
}

fun NavGraphBuilder.homeRoute(
    navigateToWrite: () -> Unit,
    navigateToWriteWithArgs: (String) -> Unit,
    navigateToAuth: () -> Unit,
    onDataLoaded: () -> Unit
){
    composable(route = Screen.Home.route){
        val viewModel: HomeViewModel = viewModel()
        val diaries by viewModel.diaries
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var signOutDialogOpened by remember{
            mutableStateOf(false)
        }

        LaunchedEffect(key1 = diaries){
            if(diaries !is RequestState.Loading){
                onDataLoaded()
            }
        }

        HomeScreen(
            diaries = diaries,
            drawerState = drawerState,
            onMenuClicked = {
                scope.launch {
                    drawerState.open()
                }
            },
            navigateToWrite = navigateToWrite,
            onSignOutClicked = {
                signOutDialogOpened = true
            },
            navigateToWriteWithArgs = navigateToWriteWithArgs
        )
        LaunchedEffect(key1 = Unit){
            MongoDB.configureRealm()
        }
        
        DisplayAlertDialog(
            title = "Sign Out",
            message = "Are you sure you want to sign out?",
            dialogOpened = signOutDialogOpened,
            onDialogClosed = { signOutDialogOpened = false },
            onYesClicked = {
                scope.launch(Dispatchers.IO) {
                    val user = App.create(APP_ID).currentUser
                    user?.logOut()
                    withContext(Dispatchers.Main){
                        navigateToAuth()
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalPagerApi::class)
fun NavGraphBuilder.writeRoute(
    onBackPressed: () -> Unit,
){
    composable(
        route = Screen.Write.route,
        arguments = listOf(
            navArgument(name = WRITE_SCREEN_ARGUMENT_KEY){
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ){
        val viewModel: WriteViewModel = viewModel()
        val context = LocalContext.current
        val uiState = viewModel.uiState
        val pagerState = rememberPagerState()
        val galleryState = rememberGalleryState()
        val pageNumber by remember {
            derivedStateOf{ pagerState.currentPage }
        }

        WriteScreen(
            uiState = uiState,
            onBackPressed = onBackPressed,
            pagerState = pagerState,
            onTitleChanged = { viewModel.setTitle(title = it) },
            onDescriptionChanged = { viewModel.setDescription(description = it)},
            galleryState = galleryState,
            onDeleteConfirmed = { viewModel.deleteDiary(
                onSuccess = {
                    Toast.makeText(
                        context,
                        "Diary Successfully Delete",
                        Toast.LENGTH_SHORT
                    ).show()
                    onBackPressed()
                },
                onError = { message ->
                    Toast.makeText(
                        context,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ) },
            onDateTimeUpdated = { viewModel.updateDateTime(zonedDateTime = it) },
            moodName = {
                Mood.values()[pageNumber].name
            },
            onSaveClicked = { diary ->
                viewModel.upsertDiary(
                    diary = diary.apply { mood = Mood.values()[pageNumber].name },
                    onSuccess = { onBackPressed () },
                    onError = { message ->
                        Toast.makeText(
                            context,
                            message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            },
            onImageSelect = { uri ->
                galleryState.addImage(
                    GalleryImage(
                        image = uri,
                        remoteImagePath = ""
                    )
                )
            }
        )
    }
}