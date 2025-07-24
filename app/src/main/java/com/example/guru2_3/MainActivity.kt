package com.example.guru2_3

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() // 이 부분은 최신 UI 기능으로, 필요 없다면 주석 처리하거나 삭제해도 됩니다.
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- 여기에 DatabaseHelper 관련 코드 추가 ---
        // 1. DatabaseHelper 인스턴스 생성
        val dbHelper = DatabaseHelper(this)

        // 2. 새로운 메모 추가
        dbHelper.addMemo("DB Helper 테스트 메모")
        Log.d("DatabaseTest", "새로운 메모를 추가했습니다.")

        // 3. 모든 메모를 불러와서 로그로 출력
        val memoList = dbHelper.getAllMemos()
        Log.d("DatabaseTest", "--- 전체 메모 목록 ---")
        for (memo in memoList) {
            Log.d("DatabaseTest", "ID: ${memo.id}, 내용: ${memo.content}")
        }
        // ------------------------------------
    }
}