package com.example.guru2_3

import DatabaseHelper
import TodoAdapter
import android.content.Intent

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Timer
import kotlin.concurrent.timer

class MainActivity : AppCompatActivity() {
    //타이머 DB 연동 변수
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
    private var userId: Long = 0
    private var selectedDate: String =""
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
    lateinit var spinner: Spinner
    lateinit var spinnerAdapter: ArrayAdapter<String>
    lateinit var selectedDateText: TextView
    //lateinit var calendarView : CalendarView

    // 캘린더 관련 변수들
    private lateinit var sundayDate: TextView
    private lateinit var mondayDate: TextView
    private lateinit var tuesdayDate: TextView
    private lateinit var wednesdayDate: TextView
    private lateinit var thursdayDate: TextView
    private lateinit var fridayDate: TextView
    private lateinit var saturdayDate: TextView

    private lateinit var sundayStatus: ImageView
    private lateinit var mondayStatus: ImageView
    private lateinit var tuesdayStatus: ImageView
    private lateinit var wednesdayStatus: ImageView
    private lateinit var thursdayStatus: ImageView
    private lateinit var fridayStatus: ImageView
    private lateinit var saturdayStatus: ImageView

    private lateinit var dbHelper: DatabaseHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() // 이 부분은 최신 UI 기능으로, 필요 없다면 주석 처리하거나 삭제해도 됩니다.
        setContentView(R.layout.activity_test)

        dbHelper = DatabaseHelper(this)

        //데이터베이스에서 저장된 설정 불러오기
        val savedSettings = dbHelper.getDefaultTimerSettings()
        studyTime = intent.getIntExtra("집중시간", savedSettings.studyTime)
        shortBreak = intent.getIntExtra("짧은휴식", savedSettings.shortBreak)
        longBreak = intent.getIntExtra("긴휴식", savedSettings.longBreak)
        session = intent.getIntExtra("세션수", savedSettings.session)
        userId = intent.getLongExtra("USER_ID", 0)

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
        spinner = findViewById(R.id.spinner)
        selectedDateText = findViewById(R.id.selectedDateText)


        // TodoAdapter 초기화 (DatabaseHelper와 캘린더 업데이트 콜백 포함)
        todoAdapter = TodoAdapter(mutableListOf(), dbHelper) { 
            // 태스크 상태 변경 시 캘린더 새로고침
            refreshCalendar()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = todoAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 구분선 추가
        val dividerItemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(dividerItemDecoration)

        // 캘린더 뷰 초기화
        sundayDate = findViewById(R.id.sundayDate)
        mondayDate = findViewById(R.id.mondayDate)
        tuesdayDate = findViewById(R.id.tuesdayDate)
        wednesdayDate = findViewById(R.id.wednesdayDate)
        thursdayDate = findViewById(R.id.thursdayDate)
        fridayDate = findViewById(R.id.fridayDate)
        saturdayDate = findViewById(R.id.saturdayDate)

        sundayStatus = findViewById(R.id.sundayStatus)
        mondayStatus = findViewById(R.id.mondayStatus)
        tuesdayStatus = findViewById(R.id.tuesdayStatus)
        wednesdayStatus = findViewById(R.id.wednesdayStatus)
        thursdayStatus = findViewById(R.id.thursdayStatus)
        fridayStatus = findViewById(R.id.fridayStatus)
        saturdayStatus = findViewById(R.id.saturdayStatus)

        // 캘린더 설정
        setupCalendar()

        // 현재 날짜 설정
        val calendar = Calendar.getInstance()
        selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        selectedDateText.text = selectedDate

        // 캘린더 클릭 리스너 설정
        setupCalendarClickListeners()

        // 기존 태스크들 로드
        loadExistingTasks()

        //태그
        spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        val tagPairs = dbHelper.getAllTags(userId)  // List<Pair<Long, String>>
        val tagNames = tagPairs.map { it.second }   // List<String>
        spinnerAdapter.clear()
        spinnerAdapter.addAll(tagNames)
        spinnerAdapter.notifyDataSetChanged()

        //캘린더
//        val calendar = Calendar.getInstance()
//        selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        listBtn.setOnClickListener {
            val text = todoListEdt.text.toString()
            val selectedTag = spinner.selectedItem as? String ?: "태그 없음"
            if (text.isNotBlank()) {
                // 날짜 선택 다이얼로그 표시
                showDatePickerDialog { selectedDateForTask ->
                    // 태그 ID 찾기
                    val tagPairs = dbHelper.getAllTags(userId)
                    val selectedTagPair = tagPairs.find { it.second == selectedTag }
                    val tagId = selectedTagPair?.first ?: 1L
                    
                    // 데이터베이스에 태스크 저장
                    val taskId = dbHelper.addTaskWithDate(userId, tagId, text, selectedDateForTask)
                    
                    // UI에 추가 (현재 선택된 날짜와 같은 경우에만)
                    if (selectedDateForTask == selectedDate) {
                        val todoItem = TodoItem(taskId, text, selectedTag, selectedDateForTask)
                        todoAdapter.addItem(todoItem)
                    }
                    
                    todoListEdt.text.clear()
                    
                    // 캘린더 업데이트
                    refreshCalendar()
                    
                    Toast.makeText(this, "태스크가 $selectedDateForTask 에 추가되었습니다", Toast.LENGTH_SHORT).show()
                }
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

        // 날짜 선택 리스너
        //calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->

        //}

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
                intent.putExtra("USER_ID", userId)
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
                            
                            // 타이머 완료 시 데이터베이스에 기록 저장
                            dbHelper.saveTimerSettings(studyTime, shortBreak, longBreak, session)
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

    override fun onResume() {
        super.onResume()
        loadExistingTags()  // ← 태그 목록 다시 불러옴 (DB에서)
        refreshCalendar() // 캘린더 상태 업데이트

    }

    private fun loadExistingTags() {
        val existingTags = dbHelper.getAllTags(userId)

        // Spinner에 연결된 어댑터 갱신
        spinnerAdapter.clear()
        spinnerAdapter.addAll(existingTags.map { it.second }) // 태그 이름만 추출해서 추가
        spinnerAdapter.notifyDataSetChanged()
    }

    // 캘린더 설정
    private fun setupCalendar() {
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        // 이번주 일요일 날짜 계산
        val daysToSunday = if (currentDayOfWeek == Calendar.SUNDAY) 0 else currentDayOfWeek - Calendar.SUNDAY
        calendar.add(Calendar.DAY_OF_YEAR, -daysToSunday)
        
        // 이번주 날짜들 설정
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val weekDates = mutableListOf<String>()
        
        for (i in 0..6) {
            val weekCalendar = Calendar.getInstance()
            weekCalendar.time = calendar.time
            weekCalendar.add(Calendar.DAY_OF_YEAR, i)
            weekDates.add(dateFormat.format(weekCalendar.time))
        }
        
        // 날짜 텍스트 설정
        sundayDate.text = calendar.get(Calendar.DAY_OF_MONTH).toString()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        mondayDate.text = calendar.get(Calendar.DAY_OF_MONTH).toString()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        tuesdayDate.text = calendar.get(Calendar.DAY_OF_MONTH).toString()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        wednesdayDate.text = calendar.get(Calendar.DAY_OF_MONTH).toString()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        thursdayDate.text = calendar.get(Calendar.DAY_OF_MONTH).toString()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        fridayDate.text = calendar.get(Calendar.DAY_OF_MONTH).toString()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        saturdayDate.text = calendar.get(Calendar.DAY_OF_MONTH).toString()
        
        // 각 날짜별 상태 업데이트
        updateCalendarStatus(weekDates)
    }
    
    // 캘린더 상태 업데이트
    private fun updateCalendarStatus(weekDates: List<String>) {
        val statusViews = listOf(sundayStatus, mondayStatus, tuesdayStatus, wednesdayStatus, 
                                thursdayStatus, fridayStatus, saturdayStatus)
        
        for (i in weekDates.indices) {
            val date = weekDates[i]
            val statusView = statusViews[i]
            
            // 투명도 초기화
            statusView.alpha = 1.0f
            
            if (dbHelper.hasTasksForDate(userId, date)) {
                if (dbHelper.areAllTasksCompletedForDate(userId, date)) {
                    // 태스크 있고 모두 완료됨 - 초록색 체크 표시
                    statusView.setImageResource(android.R.drawable.checkbox_on_background)
                    statusView.setColorFilter(getColor(R.color.task_completed))
                } else {
                    // 태스크 있지만 미완료 - 빨간색 원
                    statusView.setImageResource(android.R.drawable.radiobutton_off_background)
                    statusView.setColorFilter(getColor(R.color.task_incomplete))
                }
            } else {
                // 태스크 없음 - 연한 회색 원
                statusView.setImageResource(android.R.drawable.radiobutton_off_background)
                statusView.setColorFilter(getColor(R.color.light_gray))
            }
        }
    }
    
    // 캘린더 새로고침
    private fun refreshCalendar() {
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        // 이번주 일요일 날짜 계산
        val daysToSunday = if (currentDayOfWeek == Calendar.SUNDAY) 0 else currentDayOfWeek - Calendar.SUNDAY
        calendar.add(Calendar.DAY_OF_YEAR, -daysToSunday)
        
        // 이번주 날짜들 계산
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val weekDates = mutableListOf<String>()
        
        for (i in 0..6) {
            val weekCalendar = Calendar.getInstance()
            weekCalendar.time = calendar.time
            weekCalendar.add(Calendar.DAY_OF_YEAR, i)
            weekDates.add(dateFormat.format(weekCalendar.time))
        }
        
        updateCalendarStatus(weekDates)
    }

    // 캘린더 클릭 리스너 설정
    private fun setupCalendarClickListeners() {
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        // 이번주 일요일 날짜 계산
        val daysToSunday = if (currentDayOfWeek == Calendar.SUNDAY) 0 else currentDayOfWeek - Calendar.SUNDAY
        calendar.add(Calendar.DAY_OF_YEAR, -daysToSunday)
        
        // 이번주 날짜들 계산
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val weekDates = mutableListOf<String>()
        
        for (i in 0..6) {
            val weekCalendar = Calendar.getInstance()
            weekCalendar.time = calendar.time
            weekCalendar.add(Calendar.DAY_OF_YEAR, i)
            weekDates.add(dateFormat.format(weekCalendar.time))
        }
        
        // 각 날짜 컨테이너에 클릭 리스너 설정
        val containers = listOf(
            findViewById<LinearLayout>(R.id.sundayContainer),
            findViewById<LinearLayout>(R.id.mondayContainer),
            findViewById<LinearLayout>(R.id.tuesdayContainer),
            findViewById<LinearLayout>(R.id.wednesdayContainer),
            findViewById<LinearLayout>(R.id.thursdayContainer),
            findViewById<LinearLayout>(R.id.fridayContainer),
            findViewById<LinearLayout>(R.id.saturdayContainer)
        )
        
        containers.forEachIndexed { index, container ->
            container.setOnClickListener {
                selectedDate = weekDates[index]
                selectedDateText.text = selectedDate
                loadExistingTasks()
                
                // 선택된 날짜 하이라이트 표시
                highlightSelectedDate(index)
                
                Toast.makeText(this, "선택된 날짜: ${weekDates[index]}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // 선택된 날짜 하이라이트
    private fun highlightSelectedDate(selectedIndex: Int) {
        val containers = listOf(
            findViewById<LinearLayout>(R.id.sundayContainer),
            findViewById<LinearLayout>(R.id.mondayContainer),
            findViewById<LinearLayout>(R.id.tuesdayContainer),
            findViewById<LinearLayout>(R.id.wednesdayContainer),
            findViewById<LinearLayout>(R.id.thursdayContainer),
            findViewById<LinearLayout>(R.id.fridayContainer),
            findViewById<LinearLayout>(R.id.saturdayContainer)
        )
        
        containers.forEachIndexed { index, container ->
            if (index == selectedIndex) {
                container.setBackgroundColor(getColor(R.color.red))
                container.alpha = 0.3f
            } else {
                container.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                container.alpha = 1.0f
            }
        }
    }

    private fun loadExistingTasks() {
        val tasksForDate = dbHelper.getTasksByDate(userId, selectedDate)
        val todoItems = tasksForDate.map { task ->
            val tagName = dbHelper.getTag(task.tagId) ?: "태그 없음"
            TodoItem(
                id = task.id,
                text = task.title,
                tagName = tagName,
                date = selectedDate,
                isDone = task.isCompleted
            )
        }
        todoAdapter.setItems(todoItems)
    }

    private fun showDatePickerDialog(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = android.app.DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDayOfMonth ->
            calendar.set(selectedYear, selectedMonth, selectedDayOfMonth)
            val selectedDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            onDateSelected(selectedDateString)
        }, year, month, day)
        datePickerDialog.show()
    }
}