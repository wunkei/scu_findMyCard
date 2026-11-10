package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class PostDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        const val DATABASE_NAME = "find_my_card.db"
        const val DATABASE_VERSION = 5

        const val TABLE_POSTS = "user_posts"
        const val COLUMN_ID = "id"
        const val COLUMN_TYPE = "type"
        const val COLUMN_STATUS = "status"
        const val COLUMN_TITLE = "title"
        const val COLUMN_CAMPUS = "campus"
        const val COLUMN_LOCATION = "location"
        const val COLUMN_DATE = "date"
        const val COLUMN_NOTE = "note"
        const val COLUMN_IS_DEMO = "is_demo"
        const val COLUMN_PERSON_NAME = "person_name"
        const val COLUMN_DOCUMENT_NUMBER = "document_number"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_IMAGE_URI = "image_uri"
        const val COLUMN_CREATED_AT = "created_at"

        const val TABLE_USERS = "user_profiles"
        const val COLUMN_USER_STUDENT_ID = "student_id"
        const val COLUMN_USER_NAME = "name"
        const val COLUMN_USER_PASSWORD = "password"
        const val COLUMN_USER_ID_CARD = "id_card"
        const val COLUMN_USER_PHONE = "phone"
        const val COLUMN_USER_CAMPUS = "campus"
        const val COLUMN_USER_IS_VERIFIED = "is_verified"
        const val COLUMN_USER_CREATED_AT = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_POSTS (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_TYPE TEXT NOT NULL,
                $COLUMN_STATUS TEXT NOT NULL,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_CAMPUS TEXT NOT NULL,
                $COLUMN_LOCATION TEXT NOT NULL,
                $COLUMN_DATE TEXT NOT NULL,
                $COLUMN_NOTE TEXT NOT NULL,
                $COLUMN_IS_DEMO INTEGER NOT NULL DEFAULT 0,
                $COLUMN_PERSON_NAME TEXT DEFAULT '',
                $COLUMN_DOCUMENT_NUMBER TEXT DEFAULT '',
                $COLUMN_LATITUDE REAL,
                $COLUMN_LONGITUDE REAL,
                $COLUMN_IMAGE_URI TEXT DEFAULT '',
                $COLUMN_CREATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )
        createUserTable(db)
    }

    private fun createUserTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_USERS (
                $COLUMN_USER_STUDENT_ID TEXT PRIMARY KEY,
                $COLUMN_USER_NAME TEXT NOT NULL,
                $COLUMN_USER_PASSWORD TEXT DEFAULT '',
                $COLUMN_USER_ID_CARD TEXT DEFAULT '',
                $COLUMN_USER_PHONE TEXT DEFAULT '',
                $COLUMN_USER_CAMPUS TEXT DEFAULT '江安校区',
                $COLUMN_USER_IS_VERIFIED INTEGER NOT NULL DEFAULT 1,
                $COLUMN_USER_CREATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createUserTable(db)
        }
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_USER_PASSWORD TEXT DEFAULT ''")
            } catch (_: Exception) {
            }
        }
        if (oldVersion < 4) {
            try {
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_user_id_card ON $TABLE_USERS ($COLUMN_USER_ID_CARD)")
            } catch (_: Exception) {
            }
        }
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE $TABLE_POSTS ADD COLUMN $COLUMN_LATITUDE REAL")
                db.execSQL("ALTER TABLE $TABLE_POSTS ADD COLUMN $COLUMN_LONGITUDE REAL")
                db.execSQL("ALTER TABLE $TABLE_POSTS ADD COLUMN $COLUMN_IMAGE_URI TEXT DEFAULT ''")
            } catch (_: Exception) {
            }
        }
    }

    fun insertPost(post: LostFoundPost): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, post.id)
            put(COLUMN_TYPE, post.type)
            put(COLUMN_STATUS, post.status)
            put(COLUMN_TITLE, post.title)
            put(COLUMN_CAMPUS, post.campus)
            put(COLUMN_LOCATION, post.location)
            put(COLUMN_DATE, post.date)
            put(COLUMN_NOTE, post.note)
            put(COLUMN_IS_DEMO, if (post.isDemo) 1 else 0)
            put(COLUMN_PERSON_NAME, post.personName)
            put(COLUMN_DOCUMENT_NUMBER, post.documentNumber)
            if (post.latitude != null) put(COLUMN_LATITUDE, post.latitude) else putNull(COLUMN_LATITUDE)
            if (post.longitude != null) put(COLUMN_LONGITUDE, post.longitude) else putNull(COLUMN_LONGITUDE)
            put(COLUMN_IMAGE_URI, post.imageUri)
            put(COLUMN_CREATED_AT, post.createdAt)
        }
        return db.insertWithOnConflict(TABLE_POSTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllPosts(): List<LostFoundPost> {
        val posts = mutableListOf<LostFoundPost>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_POSTS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_CREATED_AT DESC"
        )
        cursor.use {
            val idIndex = it.getColumnIndexOrThrow(COLUMN_ID)
            val typeIndex = it.getColumnIndexOrThrow(COLUMN_TYPE)
            val statusIndex = it.getColumnIndexOrThrow(COLUMN_STATUS)
            val titleIndex = it.getColumnIndexOrThrow(COLUMN_TITLE)
            val campusIndex = it.getColumnIndexOrThrow(COLUMN_CAMPUS)
            val locationIndex = it.getColumnIndexOrThrow(COLUMN_LOCATION)
            val dateIndex = it.getColumnIndexOrThrow(COLUMN_DATE)
            val noteIndex = it.getColumnIndexOrThrow(COLUMN_NOTE)
            val isDemoIndex = it.getColumnIndexOrThrow(COLUMN_IS_DEMO)
            val personNameIndex = it.getColumnIndexOrThrow(COLUMN_PERSON_NAME)
            val documentNumberIndex = it.getColumnIndexOrThrow(COLUMN_DOCUMENT_NUMBER)
            val latIndex = it.getColumnIndex(COLUMN_LATITUDE)
            val lngIndex = it.getColumnIndex(COLUMN_LONGITUDE)
            val imgIndex = it.getColumnIndex(COLUMN_IMAGE_URI)
            val createdAtIndex = it.getColumnIndexOrThrow(COLUMN_CREATED_AT)

            while (it.moveToNext()) {
                val lat = if (latIndex != -1 && !it.isNull(latIndex)) it.getDouble(latIndex) else null
                val lng = if (lngIndex != -1 && !it.isNull(lngIndex)) it.getDouble(lngIndex) else null
                val img = if (imgIndex != -1) it.getString(imgIndex).orEmpty() else ""

                posts.add(
                    LostFoundPost(
                        id = it.getString(idIndex),
                        type = it.getString(typeIndex),
                        status = it.getString(statusIndex),
                        title = it.getString(titleIndex),
                        campus = it.getString(campusIndex),
                        location = it.getString(locationIndex),
                        date = it.getString(dateIndex),
                        note = it.getString(noteIndex),
                        isDemo = it.getInt(isDemoIndex) == 1,
                        personName = it.getString(personNameIndex).orEmpty(),
                        documentNumber = it.getString(documentNumberIndex).orEmpty(),
                        latitude = lat,
                        longitude = lng,
                        imageUri = img,
                        createdAt = it.getLong(createdAtIndex)
                    )
                )
            }
        }
        return posts
    }

    fun deletePost(id: String): Int {
        val db = writableDatabase
        return db.delete(TABLE_POSTS, "$COLUMN_ID = ?", arrayOf(id))
    }

    fun updateStatus(id: String, newStatus: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_STATUS, newStatus)
        }
        return db.update(TABLE_POSTS, values, "$COLUMN_ID = ?", arrayOf(id))
    }

    fun saveUserProfile(profile: UserProfile): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_STUDENT_ID, profile.studentId)
            put(COLUMN_USER_NAME, profile.name)
            put(COLUMN_USER_PASSWORD, profile.password)
            put(COLUMN_USER_ID_CARD, profile.idCard)
            put(COLUMN_USER_PHONE, profile.phone)
            put(COLUMN_USER_CAMPUS, profile.campus)
            put(COLUMN_USER_IS_VERIFIED, if (profile.isVerified) 1 else 0)
            put(COLUMN_USER_CREATED_AT, profile.createdAt)
        }
        return db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun updateUserPassword(studentId: String, newPassword: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_PASSWORD, newPassword)
        }
        return db.update(TABLE_USERS, values, "$COLUMN_USER_STUDENT_ID = ?", arrayOf(studentId))
    }

    fun updateUserContact(studentId: String, phone: String, campus: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_PHONE, phone)
            put(COLUMN_USER_CAMPUS, campus)
        }
        return db.update(TABLE_USERS, values, "$COLUMN_USER_STUDENT_ID = ?", arrayOf(studentId))
    }

    fun isIdCardBound(idCard: String, excludeStudentId: String = ""): Boolean {
        val cleanId = idCard.replace(" ", "").trim().uppercase()
        if (cleanId.isBlank()) return false
        val db = readableDatabase
        val cursor = if (excludeStudentId.isBlank()) {
            db.query(
                TABLE_USERS,
                arrayOf(COLUMN_USER_STUDENT_ID),
                "$COLUMN_USER_ID_CARD = ?",
                arrayOf(cleanId),
                null,
                null,
                null
            )
        } else {
            db.query(
                TABLE_USERS,
                arrayOf(COLUMN_USER_STUDENT_ID),
                "$COLUMN_USER_ID_CARD = ? AND $COLUMN_USER_STUDENT_ID != ?",
                arrayOf(cleanId, excludeStudentId),
                null,
                null,
                null
            )
        }
        return cursor.use { it.count > 0 }
    }

    fun isStudentIdRegistered(studentId: String): Boolean {
        val clean = studentId.replace(" ", "").trim().removeSuffix("@scu.edu.cn").removeSuffix("@SCU.EDU.CN")
        if (clean.isBlank()) return false
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_USER_STUDENT_ID),
            "$COLUMN_USER_STUDENT_ID = ?",
            arrayOf(clean),
            null,
            null,
            null
        )
        return cursor.use { it.count > 0 }
    }

    fun getUserProfile(studentId: String): UserProfile? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            null,
            "$COLUMN_USER_STUDENT_ID = ?",
            arrayOf(studentId),
            null,
            null,
            null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                val studentIdIdx = it.getColumnIndexOrThrow(COLUMN_USER_STUDENT_ID)
                val nameIdx = it.getColumnIndexOrThrow(COLUMN_USER_NAME)
                val passwordIdx = it.getColumnIndex(COLUMN_USER_PASSWORD)
                val idCardIdx = it.getColumnIndexOrThrow(COLUMN_USER_ID_CARD)
                val phoneIdx = it.getColumnIndexOrThrow(COLUMN_USER_PHONE)
                val campusIdx = it.getColumnIndexOrThrow(COLUMN_USER_CAMPUS)
                val isVerifiedIdx = it.getColumnIndexOrThrow(COLUMN_USER_IS_VERIFIED)
                val createdAtIdx = it.getColumnIndexOrThrow(COLUMN_USER_CREATED_AT)

                UserProfile(
                    studentId = it.getString(studentIdIdx),
                    name = it.getString(nameIdx),
                    password = if (passwordIdx != -1) it.getString(passwordIdx).orEmpty() else "",
                    idCard = it.getString(idCardIdx).orEmpty(),
                    phone = it.getString(phoneIdx).orEmpty(),
                    campus = it.getString(campusIdx).orEmpty(),
                    isVerified = it.getInt(isVerifiedIdx) == 1,
                    createdAt = it.getLong(createdAtIdx)
                )
            } else {
                null
            }
        }
    }

    fun getAllUserProfiles(): List<UserProfile> {
        val list = mutableListOf<UserProfile>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_USER_CREATED_AT DESC"
        )
        cursor.use {
            val studentIdIdx = it.getColumnIndexOrThrow(COLUMN_USER_STUDENT_ID)
            val nameIdx = it.getColumnIndexOrThrow(COLUMN_USER_NAME)
            val passwordIdx = it.getColumnIndex(COLUMN_USER_PASSWORD)
            val idCardIdx = it.getColumnIndexOrThrow(COLUMN_USER_ID_CARD)
            val phoneIdx = it.getColumnIndexOrThrow(COLUMN_USER_PHONE)
            val campusIdx = it.getColumnIndexOrThrow(COLUMN_USER_CAMPUS)
            val isVerifiedIdx = it.getColumnIndexOrThrow(COLUMN_USER_IS_VERIFIED)
            val createdAtIdx = it.getColumnIndexOrThrow(COLUMN_USER_CREATED_AT)

            while (it.moveToNext()) {
                list.add(
                    UserProfile(
                        studentId = it.getString(studentIdIdx),
                        name = it.getString(nameIdx),
                        password = if (passwordIdx != -1) it.getString(passwordIdx).orEmpty() else "",
                        idCard = it.getString(idCardIdx).orEmpty(),
                        phone = it.getString(phoneIdx).orEmpty(),
                        campus = it.getString(campusIdx).orEmpty(),
                        isVerified = it.getInt(isVerifiedIdx) == 1,
                        createdAt = it.getLong(createdAtIdx)
                    )
                )
            }
        }
        return list
    }

    fun deleteUserProfile(studentId: String): Int {
        val db = writableDatabase
        return db.delete(TABLE_USERS, "$COLUMN_USER_STUDENT_ID = ?", arrayOf(studentId))
    }
}
