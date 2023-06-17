package com.dk.luminajournal.presentation.screens.write

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dk.luminajournal.data.database.ImageToUploadDao
import com.dk.luminajournal.data.database.entity.ImageToUpload
import com.dk.luminajournal.data.repository.MongoDB
import com.dk.luminajournal.model.Diary
import com.dk.luminajournal.model.GalleryImage
import com.dk.luminajournal.model.GalleryState
import com.dk.luminajournal.model.Mood
import com.dk.luminajournal.util.Constants.WRITE_SCREEN_ARGUMENT_KEY
import com.dk.luminajournal.model.RequestState
import com.dk.luminajournal.util.fetchImagesFromFirebase
import com.dk.luminajournal.util.toRealmInstant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.kotlin.types.RealmInstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mongodb.kbson.ObjectId
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class WriteViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val imageToUploadDao: ImageToUploadDao
): ViewModel() {

    private val TAG = "WriteViewModel"
    val galleryState = GalleryState()
    var uiState by mutableStateOf(UiState())
        private set

    init {
        getDiaryIdArgument()
        fetchSelectedDiary()
    }

     private fun getDiaryIdArgument(){
         uiState = uiState.copy(
             selectedDiaryId = savedStateHandle.get<String>(
                 key = WRITE_SCREEN_ARGUMENT_KEY
             )?.replace("BsonObjectId(", "")?.replace(")", "")
         )
     }

    private fun fetchSelectedDiary(){

        if(uiState.selectedDiaryId != null){
            viewModelScope.launch(Dispatchers.Main) {
                MongoDB.getSelectedDiary(
                    diaryId = ObjectId.invoke(uiState.selectedDiaryId!!)
                )
                .catch {
                    emit(RequestState.Error(Exception("Diary is already deleted.")))
                }
                .collect{ diary ->
                    if(diary is RequestState.Success){
                        setTitle(title = diary.data.title)
                        setDescription(description = diary.data.description)
                        setMood(mood = Mood.valueOf(diary.data.mood))
                        setSelectedDiary(diary = diary.data)

                        fetchImagesFromFirebase(
                            remoteImagePaths = diary.data.images,
                            onImageDownload = { downloadedImage ->
                                galleryState.addImage(
                                    GalleryImage(
                                        image = downloadedImage,
                                        remoteImagePath = extractImagePath(
                                            fullImageUrl = downloadedImage.toString()
                                        )
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    private fun deleteImagesFromFirebase(
        images: List<String>
    ) {
        val storage = FirebaseStorage.getInstance().reference
        images.forEach { remotePath ->
            Log.i(TAG, "deleteImagesFromFirebase || remotePath = $remotePath ||")
            storage.child(remotePath).delete()
        }
    }

    private fun extractImagePath(fullImageUrl: String): String {
        val chunks = fullImageUrl.split("%2F")
        val imageName = chunks[2].split("?").first()
        return "images/${Firebase.auth.currentUser?.uid}/$imageName"
    }

    fun setTitle(title: String){
        uiState = uiState.copy(
            title = title
        )
    }

    fun setDescription(description: String){
        uiState = uiState.copy(
            description = description
        )
    }

    private fun setMood(mood: Mood){
        uiState = uiState.copy(
            mood = mood
        )
    }

    private fun setSelectedDiary(diary: Diary){
        uiState = uiState.copy(
            selectedDiary = diary
        )
    }

    fun updateDateTime(zonedDateTime: ZonedDateTime){
        uiState = uiState.copy(
            updatedDateTime = zonedDateTime.toInstant().toRealmInstant()
        )
    }

    fun upsertDiary(
        diary: Diary,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
             if(uiState.selectedDiaryId != null){
                 updateDiary(diary = diary, onSuccess = onSuccess, onError = onError)
             }
            else {
                insertDiary(diary = diary, onSuccess = onSuccess, onError = onError)
            }
        }
    }

    private suspend fun insertDiary(
        diary: Diary,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ){
        val result = MongoDB.insertDiary(diary = diary.apply{
            if(uiState.updatedDateTime != null){
                date = uiState.updatedDateTime!!
            }
        })
        if(result  is RequestState.Success){
            uploadImagesToFirebase()
            withContext(Dispatchers.Main){
                onSuccess()
            }
        }
        else if (result is RequestState.Error){
            withContext(Dispatchers.Main){
                onError(result.error.message.toString())
            }
        }
    }

    private suspend fun updateDiary(
        diary: Diary,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ){
        val result = MongoDB.updateDiary(
            diary = diary.apply{
                _id = ObjectId.invoke(uiState.selectedDiaryId!!)
                date = if(uiState.updatedDateTime != null) {
                    uiState.updatedDateTime!!
                }
                else{
                    uiState.selectedDiary!!.date
                }
            }
        )

        if(result is RequestState.Success){
            uploadImagesToFirebase()
            withContext(Dispatchers.Main){
                onSuccess()
            }
        }
        else if(result is RequestState.Error){
            withContext(Dispatchers.Main){
                onError(result.error.message.toString())
            }
        }
    }

    fun deleteDiary(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ){
        viewModelScope.launch(Dispatchers.IO) {
            if(uiState.selectedDiaryId != null){
                val result = MongoDB.deleteDiary(id = ObjectId(uiState.selectedDiaryId!!))
                if(result is RequestState.Success){
                    Log.i(TAG, "deleteDiary || result = success || selectedDiaryId = ${uiState.selectedDiaryId} ||")
                    withContext(Dispatchers.Main){
                        uiState.selectedDiary?.let { deleteImagesFromFirebase(images = it.images) }
                        onSuccess()
                    }
                }
                else if(result is RequestState.Error){
                    Log.i(TAG, "deleteDiary || result = Error | Error Message: ${result.error.message.toString()} || selectedDiaryId = ${uiState.selectedDiaryId} ||")
                    withContext(Dispatchers.Main){
                        onError(result.error.message.toString())
                    }
                }
            }
        }
    }

    fun addImage(
        image: Uri,
        imageType: String
    ){
        val remoteImagePath = "images/${FirebaseAuth.getInstance().currentUser?.uid}/" +
                "${image.lastPathSegment}-${System.currentTimeMillis()}.$imageType"
        Log.i(TAG, "addImage || remoteImage = $remoteImagePath")
        galleryState.addImage(
            GalleryImage(
                image = image,
                remoteImagePath = remoteImagePath
            )
        )
    }

    private fun uploadImagesToFirebase(){
        val storage = FirebaseStorage.getInstance().reference
        galleryState.images.forEach { galleryImage->
            Log.i(TAG, "uploadImagesToFirebase || imagePath=${galleryImage.remoteImagePath} ||")
            val imagePath = storage.child(galleryImage.remoteImagePath)
            imagePath.putFile(galleryImage.image)
                .addOnProgressListener {
                    val sessionUri = it.uploadSessionUri
                    if(sessionUri != null){
                        viewModelScope.launch(Dispatchers.IO) {
                            imageToUploadDao.addImageToUpload(
                                ImageToUpload(
                                    remoteImagePath = galleryImage.remoteImagePath,
                                    imageUri = galleryImage.image.toString(),
                                    sessionUri = sessionUri.toString()
                                )
                            )
                        }
                    }
                }

        }
    }

}

data class UiState(
    val selectedDiaryId: String? = null,
    val selectedDiary: Diary? = null,
    val title: String = "",
    val description: String = "",
    val mood: Mood = Mood.Neutral,
    val updatedDateTime: RealmInstant? = null
)