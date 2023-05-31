package com.dk.luminajournal.presentation.screens.home

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dk.luminajournal.data.repository.Diaries
import com.dk.luminajournal.data.repository.MongoDB
import com.dk.luminajournal.util.RequestState
import kotlinx.coroutines.launch

class HomeViewModel: ViewModel() {

    val TAG = "HomeViewModel"
    var diaries: MutableState<Diaries> = mutableStateOf(RequestState.Idle)

    init {
        observeAllDiaries()
    }

    private fun observeAllDiaries(){
        viewModelScope.launch {
            MongoDB.getAllDiaries().collect{ diary ->
                Log.i(TAG, "Diary observed: $diary")
                diaries.value = diary
            }
        }
    }

}