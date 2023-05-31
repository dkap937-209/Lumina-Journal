package com.dk.luminajournal.data.repository

import android.app.DownloadManager.Request
import com.dk.luminajournal.model.Diary
import com.dk.luminajournal.util.RequestState
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

typealias Diaries = RequestState<Map<LocalDate, List<Diary>>>
interface MongoRepository {

    fun configureRealm()
    fun getAllDiaries(): Flow<Diaries>
}