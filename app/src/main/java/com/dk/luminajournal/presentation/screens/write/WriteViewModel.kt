package com.dk.luminajournal.presentation.screens.write

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.dk.luminajournal.model.Mood
import com.dk.luminajournal.util.Constants.WRITE_SCREEN_ARGUMENT_KEY

class WriteViewModel(
    private val savedStateHandle: SavedStateHandle
): ViewModel() {

    var uiState by mutableStateOf(UiState())
        private set

    init {
        getDiaryIdArgument()
    }

     private fun getDiaryIdArgument(){
         uiState = uiState.copy(
             selectedDiaryId = savedStateHandle.get<String>(
                 key = WRITE_SCREEN_ARGUMENT_KEY
             )
         )
     }

}

data class UiState(
    val selectedDiaryId: String? = null,
    val title: String = "",
    val description: String = "",
    val mood: Mood = Mood.Neutral
)