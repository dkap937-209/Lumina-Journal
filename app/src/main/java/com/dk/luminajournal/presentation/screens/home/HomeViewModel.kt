package com.dk.luminajournal.presentation.screens.home

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dk.util.connectivity.ConnectivityObserver
import com.dk.util.connectivity.NetworkConnectivityObserver
import com.dk.luminajournal.data.database.ImageToDeleteDao
import com.dk.luminajournal.data.database.entity.ImageToDelete
import com.dk.luminajournal.data.repository.Diaries
import com.dk.luminajournal.data.repository.MongoDB
import com.dk.luminajournal.model.RequestState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val connectivity: NetworkConnectivityObserver,
    private val imageToDeleteDao: ImageToDeleteDao
): ViewModel() {

    private lateinit var allDiariesJob: Job
    private lateinit var filteredDiariesJob: Job

    private val TAG = "HomeViewModel"
    private var network by mutableStateOf(ConnectivityObserver.Status.Unavailable)
    var diaries: MutableState<Diaries> = mutableStateOf(RequestState.Idle)
    var dateIsSelected by mutableStateOf(false)
        private set

    init {
        getDiaries()
        viewModelScope.launch {
            connectivity.observe().collect{ status ->
                network = status
            }
        }
    }

    fun getDiaries(zonedDateTime: ZonedDateTime? = null){
        dateIsSelected = zonedDateTime != null
        diaries.value = RequestState.Loading
        if(dateIsSelected && zonedDateTime != null){
            Log.i(TAG, "getDiaries || Retrieving Filtered Diaries ||")
            observeFilteredDiaries(zonedDateTime)
        }
        else{
            Log.i(TAG, "getDiaries || Retrieving All Diaries ||")
            observeAllDiaries()
        }
    }

    private fun observeAllDiaries() {
        allDiariesJob = viewModelScope.launch(Dispatchers.IO) {
            if(::filteredDiariesJob.isInitialized){
                Log.i(TAG, "observeAllDiaries || Filtered Diaries Job Cancelled ||")
                filteredDiariesJob.cancelAndJoin()
            }
            MongoDB.getAllDiaries().collect{ result ->
                diaries.value = result
                Log.i(TAG, "observeAllDiaries || Diary collected: $result ||")
            }
        }
    }

    private fun observeFilteredDiaries(zonedDateTime: ZonedDateTime){
        filteredDiariesJob = viewModelScope.launch {
            if(::allDiariesJob.isInitialized){
                Log.i(TAG, "observeFilteredDiaries || All Diaries Job Cancelled ||")
                allDiariesJob.cancelAndJoin()
            }
            MongoDB.getFilteredDiaries(zonedDateTime).collect{ result ->
                Log.i(TAG, "observeFilteredDiaries || Diary collected: $result ||")
                diaries.value = result
            }
        }
    }

    fun deleteAllDiaries(
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ){

        if(network == ConnectivityObserver.Status.Available){
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val imagesDirectory = "images/${userId}"
            val storage = FirebaseStorage.getInstance().reference

            Log.i(TAG, "deleteAllDiaries || userId = $userId || imagesDirectory = $imagesDirectory")
            storage.child(imagesDirectory)
                .listAll()
                .addOnSuccessListener {
                    it.items.forEach { ref ->
                        val imagePath = "$imagesDirectory/${ref.name}"
                        Log.i(TAG, "deleteAllDiaries || userId = $userId || imagePath = $imagePath")
                        storage.child(imagePath).delete()
                            .addOnFailureListener {
                                Log.i(TAG, "deleteAllDiaries || userId = $userId || imagePath = $imagePath || Failed to beb deleted")
                                viewModelScope.launch(Dispatchers.IO) {
                                    imageToDeleteDao.addImageToDelete(
                                        ImageToDelete(
                                            remoteImagePath = imagePath
                                        )
                                    )
                                }
                            }
                    }
                    viewModelScope.launch(Dispatchers.IO) {
                        val result = MongoDB.deleteAllDiaries()
                        if(result is RequestState.Success){
                            withContext(Dispatchers.Main){
                                onSuccess()
                            }
                        }
                        else if(result is RequestState.Error){
                            withContext(Dispatchers.Main){
                                onError(result.error)
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    onError(it)
                }
        }
        else{
            onError(Exception("No Internet Connection"))
        }
    }
}