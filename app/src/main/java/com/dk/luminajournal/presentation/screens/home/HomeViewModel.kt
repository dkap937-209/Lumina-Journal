package com.dk.luminajournal.presentation.screens.home

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dk.luminajournal.connectivity.ConnectivityObserver
import com.dk.luminajournal.connectivity.NetworkConnectivityObserver
import com.dk.luminajournal.data.database.ImageToDeleteDao
import com.dk.luminajournal.data.database.entity.ImageToDelete
import com.dk.luminajournal.data.repository.Diaries
import com.dk.luminajournal.data.repository.MongoDB
import com.dk.luminajournal.model.RequestState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val connectivity: NetworkConnectivityObserver,
    private val imageToDeleteDao: ImageToDeleteDao
): ViewModel() {

    private var network by mutableStateOf(ConnectivityObserver.Status.Unavailable)
    private val TAG = "HomeViewModel"

    init {
        observeAllDiaries()
        viewModelScope.launch {
            connectivity.observe().collect{ status ->
                network = status
            }
        }
    }

    var diaries: MutableState<Diaries> = mutableStateOf(RequestState.Idle)

    private fun observeAllDiaries() {
        viewModelScope.launch(Dispatchers.IO) {
            MongoDB.getAllDiaries().collect{ result ->
                diaries.value = result
                Log.i(TAG, "observeAllDiaries || Diary collected: $result ||")
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