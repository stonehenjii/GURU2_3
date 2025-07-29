package com.example.guru2_3

import DatabaseHelper
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class TimerSetting : AppCompatActivity() {

    var studyTime = 25
    var shortBreak = 5
    var longBreak = 15
    var session = 8
    lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer_setting)

        // 데이터베이스 헬퍼 초기화
        dbHelper = DatabaseHelper(this)

        // 데이터베이스에서 저장된 설정 불러오기
        val savedSettings = dbHelper.getDefaultTimerSettings()
        studyTime = savedSettings.studyTime
        shortBreak = savedSettings.shortBreak
        longBreak = savedSettings.longBreak
        session = savedSettings.session

        if (savedSettings != null) {
            Log.d("DBCheck", "Saved timer settings: studyTime=${savedSettings.studyTime}, shortBreak=${savedSettings.shortBreak}, longBreak=${savedSettings.longBreak}, session=${savedSettings.session}")
        } else {
            Log.d("DBCheck", "No timer settings found in DB")
        }

        // 상태바 색 변경 코드 추가
        // window.statusBarColor = ContextCompat.getColor(this, R.color.mainRed)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR  // 상태바 아이콘을 검정색으로

        val studyTimeEt = findViewById<EditText>(R.id.studyTimeEt)
        val shortBreakEt = findViewById<EditText>(R.id.shortBreakEt)
        val longBreakEt = findViewById<EditText>(R.id.longBreakEt)
        val sessionEt = findViewById<EditText>(R.id.sessionEt)
        val saveBtn = findViewById<ImageButton>(R.id.SaveBtn)


        studyTimeEt.setText(studyTime.toString())
        shortBreakEt.setText(shortBreak.toString())
        longBreakEt.setText(longBreak.toString())
        sessionEt.setText(session.toString())

        saveBtn.setOnClickListener {
            studyTime = studyTimeEt.text.toString().toIntOrNull() ?: 25
            shortBreak = shortBreakEt.text.toString().toIntOrNull() ?: 5
            longBreak = longBreakEt.text.toString().toIntOrNull() ?: 15
            session = sessionEt.text.toString().toIntOrNull() ?: 8

            if (studyTime < 0 || studyTime > 60) {
                Toast.makeText(this, "집중시간: 0~60 사이 숫자만 입력 가능해요", Toast.LENGTH_SHORT).show()
                studyTimeEt.setText("25") // 기존 값으로 복구
            } else if (shortBreak < 0 || shortBreak > 60){
                Toast.makeText(this, "짧은 휴식: 0~60 사이 숫자만 입력 가능해요", Toast.LENGTH_SHORT).show()
                shortBreakEt.setText("5") // 기존 값으로 복구
            } else if (longBreak < 0 || longBreak > 60){
                Toast.makeText(this, "긴 휴식: 0~60 사이 숫자만 입력 가능해요", Toast.LENGTH_SHORT).show()
                longBreakEt.setText("15") // 기존 값으로 복구
            } else if (session < 1 || session > 10){
                Toast.makeText(this, "세션: 1~10 사이 숫자만 입력 가능해요", Toast.LENGTH_SHORT).show()
                sessionEt.setText("8") // 기존 값으로 복구
            } else {
                // 데이터베이스에 타이머 설정 저장
                val result = dbHelper.saveTimerSettings(studyTime, shortBreak, longBreak, session)
                
                Toast.makeText(this, "타이머 설정이 저장되었습니다!", Toast.LENGTH_SHORT).show()
                
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("집중시간", studyTime)
                intent.putExtra("짧은휴식", shortBreak)
                intent.putExtra("긴휴식", longBreak)
                intent.putExtra("세션수", session)
                startActivity(intent)
                Log.d(
                    "TimerSetting",
                    "studyTime: $studyTime, shortBreak: $shortBreak, longBreak: $longBreak, session: $session"
                )
            }

        }


    }
    override fun onResume() {
        super.onResume()

        val savedSettings = dbHelper.getLatestTimerSettings() ?: return

        studyTime = savedSettings.studyTime
        shortBreak = savedSettings.shortBreak
        longBreak = savedSettings.longBreak
        session = savedSettings.session

        findViewById<EditText>(R.id.studyTimeEt).setText(studyTime.toString())
        findViewById<EditText>(R.id.shortBreakEt).setText(shortBreak.toString())
        findViewById<EditText>(R.id.longBreakEt).setText(longBreak.toString())
        findViewById<EditText>(R.id.sessionEt).setText(session.toString())
    }


}