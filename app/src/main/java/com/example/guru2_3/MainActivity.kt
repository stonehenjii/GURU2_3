package com.example.guru2_3

import DatabaseHelper
import TodoAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar

import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Timer
import kotlin.concurrent.timer

class MainActivity : AppCompatActivity() {
    //타이머 DB 연동 변수
    private var userId = 1
    private var studyTime = 25
    private var shortBreak = 5
    private var longBreak = 15
    private var session = 8
    private var sessionCount = 0
    //타이머 추가 변수
    private var time = 0;
    private var timerTask : Timer? = null
    private var isRunning = false
    private var isTimeSet = false
    private var isBreak = false
    //투두리스트 변수

    lateinit var  minTextView : TextView
    lateinit var  secTextView: TextView
    lateinit var  setButton: ImageButton
    lateinit var  pauseButton: ImageButton
    lateinit var  resetButton: ImageButton
    lateinit var  listBtn : ImageButton
    lateinit var  todoListEdt : EditText
    lateinit var  todoAdapter: TodoAdapter
    lateinit var  recyclerView: RecyclerView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() // 이 부분은 최신 UI 기능으로, 필요 없다면 주석 처리하거나 삭제해도 됩니다.
        setContentView(R.layout.activity_test)

        //값을 가져옵니다.
        studyTime = intent.getIntExtra("집중시간", 25)
        shortBreak = intent.getIntExtra("짧은휴식", 5)
        longBreak = intent.getIntExtra("긴휴식", 15)
        session = intent.getIntExtra("세션수", 8)
        Log.d("MainActivity", "studyTime: $studyTime, shortBreak: $shortBreak, longBreak: $longBreak, session: $session")

        // 상태바 색 변경 코드 추가
        // window.statusBarColor = ContextCompat.getColor(this, R.color.mainRed)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR  // 상태바 아이콘을 검정색으로

        //툴바를 추가합니다
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        //타이머를 설정합니다
        secTextView = findViewById(R.id.secTextView)
        minTextView = findViewById(R.id.minTextView)
        setButton = findViewById(R.id.setButton)
        pauseButton = findViewById(R.id.pauseButton)
        resetButton = findViewById(R.id.resetButton)
        listBtn = findViewById(R.id.listBtn)
        todoListEdt = findViewById(R.id.todoListEdt)
        recyclerView = findViewById(R.id.recyclerView)

        todoAdapter = TodoAdapter(mutableListOf())
        recyclerView.adapter = todoAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        listBtn.setOnClickListener {
            val text = todoListEdt.text.toString()
            if (text.isNotBlank()) {
                todoAdapter.addItem(TodoItem(text))
                todoListEdt.text.clear()
            } else {
                Toast.makeText(this, "할 일을 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }

        setButton.setOnClickListener {
            if (!isTimeSet) {
                // ⏱️ 상태에 따라 시간 설정
                time = when {
                    isBreak && sessionCount  >= session -> longBreak * 60 * 100  // 긴 휴식
                    isBreak -> shortBreak * 60 * 100                           // 짧은 휴식
                    else -> studyTime * 60 * 100                               // 공부
                }
                isTimeSet = true
            }

            setButton.visibility = View.GONE
            resetButton.visibility = View.GONE
            pauseButton.visibility = View.VISIBLE
            start()

        }

        pauseButton.setOnClickListener {
            setButton.visibility = View.VISIBLE
            resetButton.visibility = View.VISIBLE
            pauseButton.visibility = View.GONE
            pause()
        }

        resetButton.setOnClickListener {
            reset()
        }

//        // --- 여기에 DatabaseHelper 관련 코드 추가 ---
//        // 1. DatabaseHelper 인스턴스 생성
//        val dbHelper = DatabaseHelper(this)
//
//        // 2. 새로운 메모 추가
//        dbHelper.addMemo("DB Helper 테스트 메모")
//        Log.d("DatabaseTest", "새로운 메모를 추가했습니다.")
//
//        // 3. 모든 메모를 불러와서 로그로 출력
//        val memoList = dbHelper.getAllMemos()
//        Log.d("DatabaseTest", "--- 전체 메모 목록 ---")
//        for (memo in memoList) {
//            Log.d("DatabaseTest", "ID: ${memo.id}, 내용: ${memo.content}")
//        }
//        // ------------------------------------
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_top, menu)
        return super.onCreateOptionsMenu(menu);
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.profile -> {
                // 사용자 설정 눌렀을 때 동작
                val intent = Intent(this, TagActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.timerSetting -> {
                // 타이머 설정 눌렀을 때 동작
                val intent = Intent(this, TimerSetting::class.java)
                intent.putExtra("집중시간", studyTime)
                intent.putExtra("짧은휴식", shortBreak)
                intent.putExtra("긴휴식", longBreak)
                intent.putExtra("세션수", session)
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun pause(){
        timerTask?.cancel()
        isRunning = false
    }

    private fun start(){
        timerTask = timer(period = 10) {
            time--

            val totalSec = time / 100
            val min = totalSec / 60
            val sec = totalSec % 60

            if (time <= 0) {
                timerTask?.cancel()
                isRunning = false

                runOnUiThread {
                    setButton.visibility = View.VISIBLE
                    resetButton.visibility = View.VISIBLE
                    pauseButton.visibility = View.GONE
                }

                if (!isBreak) {
                    sessionCount = 0
                    sessionCount++
                    if (sessionCount  >= session) {
                        time = longBreak * 60 * 100
                    } else {
                        time = shortBreak * 60 * 100
                    }
                    isBreak = true
                } else {
                    if (sessionCount >= session) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "모든 세션이 완료되었습니다!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        time = studyTime * 60 * 100
                        isBreak = false
                    }
                }

                // 👉 사용자가 다시 재생 버튼 누를 때까지 기다리기
                isTimeSet = false
            }

            runOnUiThread {
                minTextView.text = String.format("%02d", min)
                secTextView.text = String.format("%02d", sec)
            }
        }
        isRunning = true
    }

    private fun reset(){
        timerTask?.cancel()
        isRunning = false
        isTimeSet = false
        isBreak = false
        sessionCount = 0
        // 시간 초기화
        time = studyTime * 60 * 100
        val totalSec = time / 100
        val min = totalSec / 60
        val sec = totalSec % 60

        runOnUiThread {
            minTextView.text = String.format("%02d", min)
            secTextView.text = String.format("%02d", sec)
        }

    }

}