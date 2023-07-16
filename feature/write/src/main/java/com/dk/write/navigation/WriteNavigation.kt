package com.dk.write.navigation

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dk.util.Constants
import com.dk.util.Screen
import com.dk.util.model.Mood
import com.dk.write.WriteScreen
import com.dk.write.WriteViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState

private const val TAG = "NavGraph:WriteRoute"

@OptIn(ExperimentalPagerApi::class)
fun NavGraphBuilder.writeRoute(
    onBackPressed: () -> Unit,
){
    composable(
        route = Screen.Write.route,
        arguments = listOf(
            navArgument(name = Constants.WRITE_SCREEN_ARGUMENT_KEY){
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ){
        val viewModel: WriteViewModel = hiltViewModel()
        val context = LocalContext.current
        val uiState = viewModel.uiState
        val pagerState = rememberPagerState()
        val galleryState = viewModel.galleryState
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
                val type = context.contentResolver.getType(uri)?.split("/")?.last()?:"jpg"
                Log.i(TAG, "WriteScreen | onImageSelect || URI = $uri")
                viewModel.addImage(
                    image = uri,
                    imageType = type
                )
            },
            onImageDeleteClicked = { galleryState.removeImage(it) }
        )
    }
}