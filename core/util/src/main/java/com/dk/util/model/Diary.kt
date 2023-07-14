package com.dk.util.model

import android.os.Build
import androidx.annotation.RequiresApi
import com.dk.util.toRealmInstant
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId
import java.time.Instant

open class Diary: RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId.invoke()
    var owner_id: String = ""
    var title: String = ""
    var mood: String = Mood.Neutral.name
    var description: String = ""
    var images: RealmList<String> = realmListOf()
    @RequiresApi(Build.VERSION_CODES.O)
    var date: RealmInstant = Instant.now().toRealmInstant()
}