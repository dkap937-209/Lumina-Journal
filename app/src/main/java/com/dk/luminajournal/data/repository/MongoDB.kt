package com.dk.luminajournal.data.repository

import android.util.Log
import com.dk.luminajournal.model.Diary
import com.dk.luminajournal.util.Constants.APP_ID
import com.dk.luminajournal.util.RequestState
import com.dk.luminajournal.util.toInstant
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.asFlow
import io.realm.kotlin.ext.query
import io.realm.kotlin.log.LogLevel
import io.realm.kotlin.mongodb.App
import io.realm.kotlin.mongodb.sync.SyncConfiguration
import io.realm.kotlin.query.Sort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.mongodb.kbson.ObjectId
import java.time.ZoneId

object MongoDB: MongoRepository {

    private val app = App.create(APP_ID)
    private val user = app.currentUser
    private lateinit var realm: Realm
    private val TAG = "MongoDB"

    init{
        configureRealm()
    }

    override fun configureRealm() {
        if(user != null){
            Log.i(TAG, "Started realm configuration")
            println("User id: ${user.id}")
            var config = SyncConfiguration.Builder(
                user = user,
                schema = setOf(Diary::class)
            ).initialSubscriptions{ sub ->
               add(
                   query  = sub.query(query = "owner_id == $0", user.id),
                   name = "User's Diaries"
               )
            }
            .log(LogLevel.ALL)
            .build()
            realm = Realm.open(configuration = config)
        }

    }

    override fun getAllDiaries(): Flow<Diaries> {
        return  if(user != null){
            try {
                realm.query<Diary>(query = "owner_id == $0", user.id)
                    .sort(property = "date", sortOrder = Sort.DESCENDING)
                    .asFlow()
                    .map{ result ->
                        RequestState.Success(
                                data = result.list.groupBy {
                                    it.date.toInstant()
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                }
                        )
                    }

            }
            catch (e: Exception) {
                flow{
                    emit(RequestState.Error(e))
                }
            }
        }
        else{
            flow{
                emit(RequestState.Error(UserNotAuthenticatedException()))
            }
        }
    }

    override fun getSelectedDiary(diaryId: ObjectId): Flow<RequestState<Diary>> {
        return if(user!= null){
            try {
                realm.query<Diary>(
                    query = "_id == $0", diaryId
                ).asFlow().map {
                    RequestState.Success(data = it.list.first())
                }
            }
            catch(e: Exception){
                flow {
                    emit(RequestState.Error(e))
                }
            }
        }
        else{
            flow {
                emit( RequestState.Error(UserNotAuthenticatedException()))
            }
        }
    }

    override suspend fun insertDiary(diary: Diary): RequestState<Diary> {
        return if (user != null){
            realm.write{
                try {
                    val addedDiary = copyToRealm(
                        instance = diary.apply {
                            owner_id = user.id
                        }
                    )
                    RequestState.Success(data = addedDiary)
                }
                catch (e: Exception){
                    RequestState.Error(e)
                }
            }
        }
        else{
            RequestState.Error(UserNotAuthenticatedException())
        }
    }
}

private class UserNotAuthenticatedException: Exception("User is not logged in")