import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // --- 데이터 모델 클래스들 ---
    data class User(val id: Long, val email: String, val nickname: String)
    data class Tag(val id: Long, val userId: Long, val name: String)
    data class Task(val id: Long, val tagId: Long, val title: String, val isCompleted: Boolean)
    data class FocusTimer(val id: Long, val userId: Long, val totalDuration: Int)

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "PomodoroStudy.db"

        // 테이블 이름 정의
        private const val TABLE_USERS = "users"
        private const val TABLE_TAGS = "tags"
        private const val TABLE_TASKS = "tasks"
        private const val TABLE_FOCUS_TIMERS = "focus_timers"
        private const val TABLE_EXAM_INFOS = "exam_infos"
        private const val TABLE_MOCK_EXAMS = "mock_exams"
        private const val TABLE_SETTINGS = "settings"

        // 공통 컬럼
        private const val KEY_ID = "id"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_TAG_ID = "tag_id"
        private const val KEY_CREATED_AT = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // --- 테이블 생성 SQL 문 ---

        val CREATE_USERS_TABLE = """
            CREATE TABLE $TABLE_USERS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                email TEXT UNIQUE NOT NULL,
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
                name TEXT NOT NULL,
                d_day TEXT,
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
                $KEY_USER_ID INTEGER NOT NULL,
                start_time TEXT,
                end_time TEXT,
                focus_time INTEGER,
                short_break INTEGER,
                long_break INTEGER,
                session_count INTEGER,
                total_duration INTEGER,
                asmr_used INTEGER DEFAULT 0,
                FOREIGN KEY($KEY_USER_ID) REFERENCES $TABLE_USERS($KEY_ID)
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
                focus_time INTEGER,
                short_break INTEGER,
                long_break INTEGER,
                sessions_before_long_break INTEGER,
                FOREIGN KEY($KEY_USER_ID) REFERENCES $TABLE_USERS($KEY_ID)
            )
        """.trimIndent()

        // --- 모든 테이블 실행 ---
        db?.execSQL(CREATE_USERS_TABLE)
        db?.execSQL(CREATE_TAGS_TABLE)
        db?.execSQL(CREATE_TASKS_TABLE)
        db?.execSQL(CREATE_FOCUS_TIMERS_TABLE)
        db?.execSQL(CREATE_EXAM_INFOS_TABLE)
        db?.execSQL(CREATE_MOCK_EXAMS_TABLE)
        db?.execSQL(CREATE_SETTINGS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // 개발 중에는 간단하게 기존 테이블을 삭제하고 새로 만드는 방식을 사용합니다.
        // 실제 배포된 앱에서는 사용자 데이터를 보존하기 위해 다른 마이그레이션 방법을 사용해야 합니다.
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_SETTINGS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_MOCK_EXAMS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_EXAM_INFOS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_FOCUS_TIMERS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_TASKS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_TAGS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    // --- 데이터 조작 함수 (CRUD) ---

    fun addUser(email: String, nickname: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("email", email)
            put("password", "temp_password") // 임시 비밀번호
            put("nickname", nickname)
        }
        return db.insert(TABLE_USERS, null, values)
    }

    fun addTag(userId: Long, name: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_USER_ID, userId)
            put("name", name)
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
}