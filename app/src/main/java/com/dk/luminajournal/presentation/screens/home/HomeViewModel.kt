package com.dk.luminajournal.presentation.screens.home

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dk.luminajournal.data.repository.Diaries
import com.dk.luminajournal.data.repository.MongoDB
import com.dk.luminajournal.model.RequestState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel: ViewModel() {

    private val TAG = "HomeViewModel"

    init {
        observeAllDiaries()
    }

    var diaries: MutableState<Diaries> = mutableStateOf(RequestState.Idle)

    private fun observeAllDiaries() {
        viewModelScope.launch(Dispatchers.IO) {
            MongoDB.getAllDiaries().collect{ result ->
                diaries.value = result
                Log.i(TAG, "Diary collected: $result")
            }
        }
    }

}