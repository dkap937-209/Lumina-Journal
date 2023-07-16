package com.dk.util

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import io.realm.kotlin.types.RealmInstant
import java.time.Instant

private const val TAG = "UtilFunctions"

fun RealmInstant.toInstant(): Instant {
    val sec: Long = this.epochSeconds
    val nano: Int = this.nanosecondsOfSecond
    return if(sec >= 0){
        Instant.ofEpochSecond(sec, nano.toLong())
    }
    else{
        Instant.ofEpochSecond(sec-1, 1_000_000 + nano.toLong())
    }
}


fun Instant.toRealmInstant(): RealmInstant {
    val sec: Long = this.epochSecond
    val nano: Int = this.nano
    return if(sec >= 0){
        RealmInstant.from(sec, nano)
    }
    else{
        RealmInstant.from(
            epochSeconds = sec + 1,
            nanosecondAdjustment = -1_000_000 + nano
        )
    }
}

fun fetchImagesFromFirebase(
    remoteImagePaths: List<String>,
    onImageDownload: (Uri) -> Unit,
    onImageDownloadFailed: (Exception) -> Unit = {},
    onReadyToDisplay: () -> Unit = {}
){
    if(remoteImagePaths.isNotEmpty()){
        remoteImagePaths.forEachIndexed{ index, remoteImagePath ->
            if(remoteImagePath.trim().isNotEmpty()){
                FirebaseStorage.getInstance().reference.child(remoteImagePath.trim()).downloadUrl
                    .addOnSuccessListener { uri ->
                        Log.i(TAG, "fetchImagesFromFirebase || response = success || uri = $uri ||")
                        onImageDownload(uri)
                        if(remoteImagePaths.lastIndexOf(remoteImagePaths.last()) == index){
                            onReadyToDisplay()
                        }
                    }
                    .addOnFailureListener{
                        Log.i(TAG, "fetchImagesFromFirebase || response = failure || error = $it} ||")
                        onImageDownloadFailed(it)
                    }
            }
        }
    }
}

