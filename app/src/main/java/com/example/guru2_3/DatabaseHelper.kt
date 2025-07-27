package com.example.guru2_3

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // 데이터 모델 클래스 (헬퍼 클래스 내부에 선언해도 됨)
    data class Memo(val id: Int, val content: String)

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "LegacyGuru2.db" // DB 파일 이름
        private const val TABLE_MEMOS = "memos"

        // 테이블 컬럼 이름
        private const val KEY_ID = "id"
        private const val KEY_CONTENT = "content"
    }

    // 이 함수는 데이터베이스 파일이 없을 때, 딱 한 번만 호출됨
    // 즉, 앱을 처음 설치했을 때 테이블을 생성하는 곳
    override fun onCreate(db: SQLiteDatabase?) {
        val createTableSQL = "CREATE TABLE $TABLE_MEMOS ($KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT, $KEY_CONTENT TEXT)"
        db?.execSQL(createTableSQL)
    }

    // 데이터베이스 버전이 변경될 때 호출됨
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // 기존 테이블을 삭제하고 새로 만드는 간단한 정책
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_MEMOS")
        onCreate(db)
    }

    // --- 데이터 조작 함수 (CRUD) ---

    // CREATE (생성)
    fun addMemo(content: String) {
        val db = this.writableDatabase // 쓰기 가능한 DB를 가져옴
        val values = ContentValues()
        values.put(KEY_CONTENT, content)

        db.insert(TABLE_MEMOS, null, values)
        //db.close() // 사용 후에는 항상 닫아줘야 함
    }

    // READ (읽기)
    fun getAllMemos(): List<Memo> {
        val memoList = ArrayList<Memo>()
        val selectQuery = "SELECT * FROM $TABLE_MEMOS"
        val db = this.readableDatabase // 읽기 가능한 DB를 가져옴
        val cursor = db.rawQuery(selectQuery, null)

        // Cursor를 처음부터 끝까지 순회하면서 데이터를 리스트에 추가
        if (cursor.moveToFirst()) {
            do {
                val idIndex = cursor.getColumnIndex(KEY_ID)
                val contentIndex = cursor.getColumnIndex(KEY_CONTENT)

                // 유효한 인덱스인지 확인
                if (idIndex != -1 && contentIndex != -1) {
                    val memo = Memo(
                        id = cursor.getInt(idIndex),
                        content = cursor.getString(contentIndex)
                    )
                    memoList.add(memo)
                }
            } while (cursor.moveToNext())
        }
        cursor.close() // Cursor 사용 후 닫기
        //db.close() // DB 사용 후 닫기
        return memoList
    }
}