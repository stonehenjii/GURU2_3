import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    data class User(val id: Long, val email: String, val nickname: String)
    data class Tag(val id: Long, val userId: Long, val tag_name: String)
    data class Task(val id: Long, val tagId: Long, val title: String, val isCompleted: Boolean)
    data class FocusTimer(
        val id: Long, 
        val studyTime: Int, 
        val shortBreak: Int, 
        val longBreak: Int, 
        val session: Int
    )

    companion object {
        private const val DATABASE_VERSION = 3
        private const val DATABASE_NAME = "PomodoroStudy.db"

        private const val TABLE_USERS = "users"
        private const val TABLE_TAGS = "tags"
        private const val TABLE_TASKS = "tasks"
        private const val TABLE_FOCUS_TIMERS = "focus_timers"
        private const val TABLE_EXAM_INFOS = "exam_infos"
        private const val TABLE_MOCK_EXAMS = "mock_exams"
        private const val TABLE_SETTINGS = "settings"
        private const val TABLE_SCORE_RECORDS = "score_records"
        private const val TABLE_TIME_RECORDS = "time_records"

        private const val KEY_ID = "id"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_TAG_ID = "tag_id"
        private const val KEY_CREATED_AT = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val CREATE_USERS_TABLE = """
            CREATE TABLE $TABLE_USERS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                nickname TEXT NOT NULL,
                $KEY_CREATED_AT TEXT,
                total_focus_minutes INTEGER DEFAULT 0,
                total_sessions INTEGER DEFAULT 0
            )
        """.trimIndent()

        val CREATE_TAGS_TABLE = """
            CREATE TABLE $TABLE_TAGS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_USER_ID INTEGER NOT NULL,
                tag_name TEXT NOT NULL,
                d_day TEXT,
                created_date INTEGER,
                $KEY_CREATED_AT TEXT,
                FOREIGN KEY($KEY_USER_ID) REFERENCES $TABLE_USERS($KEY_ID)
            )
        """.trimIndent()

        val CREATE_TASKS_TABLE = """
            CREATE TABLE $TABLE_TASKS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_USER_ID INTEGER NOT NULL,
                $KEY_TAG_ID INTEGER NOT NULL,
                title TEXT NOT NULL,
                scheduled_date TEXT,
                is_completed INTEGER DEFAULT 0,
                completed_at TEXT,
                FOREIGN KEY($KEY_USER_ID) REFERENCES $TABLE_USERS($KEY_ID),
                FOREIGN KEY($KEY_TAG_ID) REFERENCES $TABLE_TAGS($KEY_ID)
            )
        """.trimIndent()

        val CREATE_FOCUS_TIMERS_TABLE = """
            CREATE TABLE $TABLE_FOCUS_TIMERS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                studyTime INTEGER NOT NULL,
                shortBreak INTEGER NOT NULL,
                longBreak INTEGER NOT NULL,
                session INTEGER NOT NULL
            )
        """.trimIndent()

        val CREATE_EXAM_INFOS_TABLE = """
            CREATE TABLE $TABLE_EXAM_INFOS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_USER_ID INTEGER NOT NULL,
                $KEY_TAG_ID INTEGER NOT NULL,
                title TEXT NOT NULL,
                date TEXT,
                exam_time INTEGER,
                note TEXT,
                FOREIGN KEY($KEY_USER_ID) REFERENCES $TABLE_USERS($KEY_ID),
                FOREIGN KEY($KEY_TAG_ID) REFERENCES $TABLE_TAGS($KEY_ID)
            )
        """.trimIndent()

        val CREATE_MOCK_EXAMS_TABLE = """
            CREATE TABLE $TABLE_MOCK_EXAMS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_USER_ID INTEGER NOT NULL,
                $KEY_TAG_ID INTEGER NOT NULL,
                title TEXT NOT NULL,
                date TEXT,
                score INTEGER,
                exam_time INTEGER,
                started_at TEXT,
                ended_at TEXT,
                memo TEXT,
                asmr_used INTEGER DEFAULT 0,
                FOREIGN KEY($KEY_USER_ID) REFERENCES $TABLE_USERS($KEY_ID),
                FOREIGN KEY($KEY_TAG_ID) REFERENCES $TABLE_TAGS($KEY_ID)
            )
        """.trimIndent()

        val CREATE_SETTINGS_TABLE = """
            CREATE TABLE $TABLE_SETTINGS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_USER_ID INTEGER UNIQUE NOT NULL,
                studyTime INTEGER,
                shortBreak INTEGER,
                longBreak INTEGER,
                sessionsBeforeLongBreak INTEGER,
                FOREIGN KEY($KEY_USER_ID) REFERENCES $TABLE_USERS($KEY_ID)
            )
        """.trimIndent()

        val CREATE_SCORE_RECORDS_TABLE = """
            CREATE TABLE IF NOT EXISTS $TABLE_SCORE_RECORDS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tag_id INTEGER NOT NULL,
                score REAL NOT NULL,
                date TEXT NOT NULL,
                FOREIGN KEY(tag_id) REFERENCES $TABLE_TAGS($KEY_ID)
            )
        """.trimIndent()

        val CREATE_TIME_RECORDS_TABLE = """
            CREATE TABLE IF NOT EXISTS $TABLE_TIME_RECORDS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tag_id INTEGER NOT NULL,
                studyTime REAL NOT NULL,
                date TEXT NOT NULL,
                FOREIGN KEY(tag_id) REFERENCES $TABLE_TAGS($KEY_ID)
            )
        """.trimIndent()


        db?.execSQL(CREATE_USERS_TABLE)
        db?.execSQL(CREATE_TAGS_TABLE)
        db?.execSQL(CREATE_TASKS_TABLE)
        db?.execSQL(CREATE_FOCUS_TIMERS_TABLE)
        db?.execSQL(CREATE_EXAM_INFOS_TABLE)
        db?.execSQL(CREATE_MOCK_EXAMS_TABLE)
        db?.execSQL(CREATE_SETTINGS_TABLE)
        db?.execSQL(CREATE_SCORE_RECORDS_TABLE)
        db?.execSQL(CREATE_TIME_RECORDS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // 기존 테이블들 삭제
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_FOCUS_TIMERS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_SETTINGS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_TIME_RECORDS")
        
        // 새로운 구조로 테이블들 생성
        val CREATE_FOCUS_TIMERS_TABLE = """
            CREATE TABLE $TABLE_FOCUS_TIMERS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                studyTime INTEGER NOT NULL,
                shortBreak INTEGER NOT NULL,
                longBreak INTEGER NOT NULL,
                session INTEGER NOT NULL
            )
        """.trimIndent()
        
        val CREATE_SETTINGS_TABLE = """
            CREATE TABLE $TABLE_SETTINGS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_USER_ID INTEGER UNIQUE NOT NULL,
                studyTime INTEGER,
                shortBreak INTEGER,
                longBreak INTEGER,
                sessionsBeforeLongBreak INTEGER,
                FOREIGN KEY($KEY_USER_ID) REFERENCES $TABLE_USERS($KEY_ID)
            )
        """.trimIndent()
        
        val CREATE_TIME_RECORDS_TABLE = """
            CREATE TABLE IF NOT EXISTS $TABLE_TIME_RECORDS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tag_id INTEGER NOT NULL,
                studyTime REAL NOT NULL,
                date TEXT NOT NULL,
                FOREIGN KEY(tag_id) REFERENCES $TABLE_TAGS($KEY_ID)
            )
        """.trimIndent()
        
        db?.execSQL(CREATE_FOCUS_TIMERS_TABLE)
        db?.execSQL(CREATE_SETTINGS_TABLE)
        db?.execSQL(CREATE_TIME_RECORDS_TABLE)
    }

    // --- 데이터 조작 함수 (CRUD) ---

    fun addUser(id: String, password: String, nickname: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("user_id", id)
            put("password", password)
            put("nickname", nickname)
        }
        return db.insert("users", null, values)
    }

    fun validateUser(userid: String, password: String): Boolean {
        val db = readableDatabase
        val cursor = db.query("users", null, "user_id=? AND password=?", arrayOf(userid, password), null, null, null)
        val isValid = cursor.moveToFirst()
        cursor.close()
        return isValid
    }


    fun addTag(userId: Long, name: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_USER_ID, userId)
            put("tag_name", name)
        }
        return db.insert(TABLE_TAGS, null, values)
    }

    fun addTask(userId: Long, tagId: Long, title: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_USER_ID, userId)
            put(KEY_TAG_ID, tagId)
            put("title", title)
        }
        return db.insert(TABLE_TASKS, null, values)
    }

    fun getTasksForTag(tagId: Long): List<Task> {
        val taskList = ArrayList<Task>()
        val db = this.readableDatabase
        val cursor = db.query(TABLE_TASKS, null, "$KEY_TAG_ID = ?", arrayOf(tagId.toString()), null, null, null)

        if (cursor.moveToFirst()) {
            do {
                val idIndex = cursor.getColumnIndex(KEY_ID)
                val tagIdIndex = cursor.getColumnIndex(KEY_TAG_ID)
                val titleIndex = cursor.getColumnIndex("title")
                val isCompletedIndex = cursor.getColumnIndex("is_completed")

                if (idIndex != -1 && titleIndex != -1) {
                    taskList.add(Task(
                        id = cursor.getLong(idIndex),
                        tagId = cursor.getLong(tagIdIndex),
                        title = cursor.getString(titleIndex),
                        isCompleted = cursor.getInt(isCompletedIndex) == 1
                    ))
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        return taskList
    }
    // 새 태그 생성
    fun createTag(tagName: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            //put("name", tagName)
            put("tag_name", tagName)
            put("created_date", System.currentTimeMillis())
            put("user_id", 1)
        }
        return db.insert("tags", null, values)
    }

    // 태그 정보 가져오기
    fun getTag(tagId: Long): String? {
        val db = readableDatabase
        val cursor = db.query(
            "tags",
            arrayOf("tag_name"),
            "id = ?",
            arrayOf(tagId.toString()),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            val tagName = cursor.getString(cursor.getColumnIndexOrThrow("tag_name"))
            cursor.close()
            tagName
        } else {
            cursor.close()
            null
        }
    }

    // 모든 태그 가져오기
    fun getAllTags(userId: Long): List<Pair<Long, String>> {
        val tags = mutableListOf<Pair<Long, String>>()
        val db = readableDatabase
        val cursor = db.query("tags", arrayOf("id", "tag_name"), null, null, null, null, "id ASC")

        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
            val name = cursor.getString(cursor.getColumnIndexOrThrow("tag_name"))
            tags.add(Pair(id, name))
        }
        cursor.close()
        return tags
    }




    //성적 데이터 추가
    fun addScoreData(tagId: Long, score: Float, date: String = getCurrentDate()): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("tag_id", tagId)
            put("score", score)
            put("date", date)
        }
        return db.insert("score_records", null, values)
    }

    //시간데이터추가
    fun addTimeData(tagId: Long, time: Float, date: String = getCurrentDate()): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("tag_id", tagId)
            put("studyTime", time)
            put("date", date)
        }
        return db.insert("time_records", null, values)
    }
    // 성적 데이터 조회
    fun getScoreData(tagId: Long): List<Pair<Float, Float>> {
        val scoreList = mutableListOf<Pair<Float, Float>>()
        val db = this.readableDatabase
        val cursor = db.query("score_records", null, "tag_id = ?", arrayOf(tagId.toString()), null, null, "date ASC")

        var index = 0f
        if (cursor.moveToFirst()) {
            do {
                val scoreIndex = cursor.getColumnIndex("score")
                if (scoreIndex != -1) {
                    val score = cursor.getFloat(scoreIndex)
                    scoreList.add(Pair(index, score))
                    index++
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        return scoreList
    }

    // 시간 데이터 조회
    fun getTimeData(tagId: Long): List<Pair<Float, Float>> {
        val timeList = mutableListOf<Pair<Float, Float>>()
        val db = this.readableDatabase
        val cursor = db.query("time_records", null, "tag_id = ?", arrayOf(tagId.toString()), null, null, "date ASC")

        var index = 0f
        if (cursor.moveToFirst()) {
            do {
                val timeIndex = cursor.getColumnIndex("studyTime")
                if (timeIndex != -1) {
                    val time = cursor.getFloat(timeIndex)
                    timeList.add(Pair(index, time))
                    index++
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        return timeList
    }

    //taginfo -> 태그 이름 저장
    fun updateTagName(tagId: Long, newName: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("tag_name", newName)
            put("tag_name", newName)
        }
        return db.update("tags", values, "id = ?", arrayOf(tagId.toString()))
    }

    private fun getCurrentDate(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    // 시험날짜 업데이트
    fun updateExamDate(tagId: Long, examDate: String?): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("d_day", examDate)
        }
        return db.update("tags", values, "id = ?", arrayOf(tagId.toString()))
    }

    // 시험날짜 조회
    fun getExamDate(tagId: Long): String? {
        val db = readableDatabase
        val cursor = db.query(
            "tags",
            arrayOf("d_day"),
            "id = ?",
            arrayOf(tagId.toString()),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            val examDate = cursor.getString(cursor.getColumnIndexOrThrow("d_day"))
            cursor.close()
            examDate
        } else {
            cursor.close()
            null
        }
    }

    // 타이머 설정 저장
    fun saveTimerSettings(studyTime: Int, shortBreak: Int, longBreak: Int, session: Int): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("studyTime", studyTime)
            put("shortBreak", shortBreak)
            put("longBreak", longBreak)
            put("session", session)
        }
        return db.insert(TABLE_FOCUS_TIMERS, null, values)
    }

    // 타이머 설정 업데이트
    fun updateTimerSettings(id: Long, studyTime: Int, shortBreak: Int, longBreak: Int, session: Int): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("studyTime", studyTime)
            put("shortBreak", shortBreak)
            put("longBreak", longBreak)
            put("session", session)
        }
        return db.update(TABLE_FOCUS_TIMERS, values, "$KEY_ID = ?", arrayOf(id.toString()))
    }

    // 타이머 설정 조회 (가장 최근 설정)
    fun getLatestTimerSettings(): FocusTimer? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_FOCUS_TIMERS,
            null,
            null,
            null,
            null,
            null,
            "$KEY_ID DESC",
            "1"
        )

        return if (cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID))
            val studyTime = cursor.getInt(cursor.getColumnIndexOrThrow("studyTime"))
            val shortBreak = cursor.getInt(cursor.getColumnIndexOrThrow("shortBreak"))
            val longBreak = cursor.getInt(cursor.getColumnIndexOrThrow("longBreak"))
            val session = cursor.getInt(cursor.getColumnIndexOrThrow("session"))
            cursor.close()
            FocusTimer(id, studyTime, shortBreak, longBreak, session)
        } else {
            cursor.close()
            null
        }
    }

    // 타이머 설정 조회 (기본값 반환)
    fun getDefaultTimerSettings(): FocusTimer {
        val settings = getLatestTimerSettings()
        return settings ?: FocusTimer(0, 25, 5, 15, 8) // 기본값: 25분, 5분, 15분, 8세션
    }

    // 모든 타이머 설정 조회
    fun getAllTimerSettings(): List<FocusTimer> {
        val settingsList = mutableListOf<FocusTimer>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_FOCUS_TIMERS,
            null,
            null,
            null,
            null,
            null,
            "$KEY_ID DESC"
        )

        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID))
            val studyTime = cursor.getInt(cursor.getColumnIndexOrThrow("studyTime"))
            val shortBreak = cursor.getInt(cursor.getColumnIndexOrThrow("shortBreak"))
            val longBreak = cursor.getInt(cursor.getColumnIndexOrThrow("longBreak"))
            val session = cursor.getInt(cursor.getColumnIndexOrThrow("session"))
            settingsList.add(FocusTimer(id, studyTime, shortBreak, longBreak, session))
        }
        cursor.close()
        return settingsList
    }

    // 타이머 설정 삭제
    fun deleteTimerSettings(id: Long): Int {
        val db = this.writableDatabase
        return db.delete(TABLE_FOCUS_TIMERS, "$KEY_ID = ?", arrayOf(id.toString()))
    }


}