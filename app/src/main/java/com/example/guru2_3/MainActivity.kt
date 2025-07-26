package com.example.guru2_3

import DatabaseHelper
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- 데이터베이스 테스트 ---
        val dbHelper = DatabaseHelper(this)
        val logTag = "DB_TEST"

        // 1. 새로운 사용자 추가
        // addUser 함수의 반환값은 새로 생성된 사용자의 ID입니다.
        val userId = dbHelper.addUser("user@example.com", "stonehenjii")

        // userId가 -1이 아니라면 성공적으로 추가된 것입니다.
        if (userId != -1L) {
            Log.d(logTag, "✅ 사용자 추가 성공! ID: $userId")

            // 2. 위 사용자의 태그(과목) 추가
            val tagId = dbHelper.addTag(userId, "자료구조")
            if (tagId != -1L) {
                Log.d(logTag, "✅ 태그 추가 성공! ID: $tagId, 이름: 자료구조")

                // 3. 위 태그에 대한 할 일(Task)들 추가
                dbHelper.addTask(userId, tagId, "1장. 배열과 리스트")
                dbHelper.addTask(userId, tagId, "2장. 스택과 큐")
                Log.d(logTag, "✅ '자료구조' 태그에 할 일 2개 추가 완료")

                // 4. '자료구조' 태그에 속한 모든 할 일 조회
                val tasks = dbHelper.getTasksForTag(tagId)
                Log.d(logTag, "--- '자료구조' 태그의 할 일 목록 ---")
                for (task in tasks) {
                    Log.d(logTag, "  - ${task.title} (완료 여부: ${task.isCompleted})")
                }

            } else {
                Log.e(logTag, "❌ 태그 추가 실패")
            }

        } else {
            Log.e(logTag, "❌ 사용자 추가 실패")
        }
    }
}