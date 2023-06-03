package com.dk.luminajournal.data.repository

import com.dk.luminajournal.model.Diary
import com.dk.luminajournal.util.RequestState
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import org.mongodb.kbson.ObjectId

typealias Diaries = RequestState<Map<LocalDate, List<Diary>>>
interface MongoRepository {
    fun configureRealm()
    fun getAllDiaries(): Flow<Diaries>
    fun getSelectedDiary(diaryId: ObjectId): RequestState<Diary>
}