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
    
    // 진행 상황 표시 관련 변수
    lateinit var progressStatusText: TextView
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
        
        // 진행 상황 표시 관련 초기화
        progressStatusText = findViewById(R.id.progressStatusText)


        /**
         * TodoAdapter 초기화 (캘린더 연동을 위한 콜백 설정)
         * 
         * 매개변수:
         * 1. mutableListOf<TodoItem>(): 빈 투두 아이템 리스트 (나중에 loadExistingTasks()에서 채움)
         * 2. dbHelper: 태스크 완료 상태를 데이터베이스에 저장하기 위한 헬퍼
         * 3. 람다 함수 { refreshCalendar() }: 태스크 상태 변경 시 캘린더 새로고침 콜백
         * 
         * 캘린더 연동 흐름:
         * 사용자가 체크박스 클릭 → TodoAdapter.onBindViewHolder의 리스너 실행
         * → dbHelper.updateTaskCompletion() → 이 람다 함수 호출 → refreshCalendar()
         * → updateCalendarStatus() → 캘린더 상태 아이콘 실시간 업데이트
         */
        todoAdapter = TodoAdapter(mutableListOf(), dbHelper) { 
            // 태스크 상태 변경 시 캘린더 새로고침 (빨간색 원 ↔ 초록색 체크)
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
            
            // 타이머 시작 시 진행 상황 업데이트
            updateProgressDisplay()
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
        
        // 초기 진행 상황 표시 설정
        updateProgressDisplay()

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
                    // 세션 완료 후 sessionCount 증가
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
                            updateProgressDisplay() // 완료 상태 표시
                        }
                    } else {
                        time = studyTime * 60 * 100
                        isBreak = false
                    }
                }
                
                // 세션/휴식 변경 시 진행 상황 업데이트
                runOnUiThread {
                    updateProgressDisplay()
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
            updateProgressDisplay() // 진행 상황 표시 업데이트
        }
    }

    /**
     * 현재 진행 상황만 간단하게 표시하는 함수
     * 
     * 표시 형식:
     * - 세션 중: "2/3"
     * - 짧은 휴식: "세션2 마무리 후 짧은휴식"
     * - 긴 휴식: "전체 세션 마무리 후 긴휴식"
     * - 완료: "🎉 모든 세션 완료!"
     */
    private fun updateProgressDisplay() {
        val currentStatus = when {
            sessionCount == 0 && !isBreak -> "1/$session"
            sessionCount >= session && isBreak -> "전체 세션 마무리 후 긴휴식"
            sessionCount >= session -> "🎉 모든 세션 완료!"
            isBreak && sessionCount > 0 -> "세션${sessionCount} 마무리 후 짧은휴식"
            !isBreak && sessionCount > 0 -> "${sessionCount + 1}/$session"
            else -> "1/$session"
        }
        
        progressStatusText.text = currentStatus
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

    /**
     * 주간 캘린더 UI를 초기화하고 설정하는 함수
     * 
     * 기능:
     * 1. 현재 주의 일요일부터 토요일까지 7일간의 날짜를 계산
     * 2. 각 요일의 TextView에 날짜(일) 표시 (예: 1, 2, 3...)
     * 3. 각 날짜별 태스크 완료 상태를 시각적으로 표시
     * 
     * 캘린더 상태 표시 규칙:
     * - 태스크 없음: 연한 회색 원
     * - 태스크 있지만 미완료: 빨간색 원
     * - 모든 태스크 완료: 초록색 체크 표시
     * 
     * 호출 시점: onCreate()에서 최초 1회
     */
    private fun setupCalendar() {
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        // 이번주 일요일 날짜 계산 (Calendar.SUNDAY = 1, MONDAY = 2, ...)
        val daysToSunday = if (currentDayOfWeek == Calendar.SUNDAY) 0 else currentDayOfWeek - Calendar.SUNDAY
        calendar.add(Calendar.DAY_OF_YEAR, -daysToSunday)
        
        // 일요일부터 토요일까지 7일간의 날짜 문자열 생성 (yyyy-MM-dd 형태)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val weekDates = mutableListOf<String>()
        
        for (i in 0..6) {
            val weekCalendar = Calendar.getInstance()
            weekCalendar.time = calendar.time
            weekCalendar.add(Calendar.DAY_OF_YEAR, i)
            weekDates.add(dateFormat.format(weekCalendar.time))
        }
        
        // 각 요일 TextView에 날짜(일) 표시 (예: 1, 15, 16, 17, 18, 19, 20)
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
        
        // 각 날짜별 태스크 완료 상태를 ImageView에 표시
        updateCalendarStatus(weekDates)
    }
    
    /**
     * 주간 캘린더의 각 날짜별 태스크 완료 상태를 시각적으로 업데이트하는 함수
     * 
     * @param weekDates 일요일부터 토요일까지 7일간의 날짜 리스트 (yyyy-MM-dd 형태)
     * 
     * 동작 과정:
     * 1. 각 날짜별로 데이터베이스에서 태스크 존재 여부 확인
     * 2. 태스크가 있는 경우 완료 상태 확인
     * 3. 상태에 따라 ImageView에 다른 아이콘과 색상 적용
     * 
     * 상태별 UI 표시:
     * - 태스크 없음: 연한 회색 빈 원 (light_gray + radiobutton_off)
     * - 태스크 있지만 미완료: 빨간색 빈 원 (task_incomplete + radiobutton_off)  
     * - 모든 태스크 완료: 초록색 체크 표시 (task_completed + checkbox_on)
     * 
     * 데이터베이스 연동:
     * - hasTasksForDate(): 특정 날짜에 태스크가 있는지 확인
     * - areAllTasksCompletedForDate(): 모든 태스크가 완료되었는지 확인
     * 
     * 호출 시점:
     * - setupCalendar(): 최초 캘린더 설정 시
     * - refreshCalendar(): 태스크 상태 변경 후
     */
    private fun updateCalendarStatus(weekDates: List<String>) {
        // 일요일부터 토요일까지 7개의 상태 표시 ImageView 배열
        val statusViews = listOf(sundayStatus, mondayStatus, tuesdayStatus, wednesdayStatus, 
                                thursdayStatus, fridayStatus, saturdayStatus)
        
        // 각 날짜별로 상태 확인 및 UI 업데이트
        for (i in weekDates.indices) {
            val date = weekDates[i]
            val statusView = statusViews[i]
            
            // 투명도 초기화 (선택 효과 제거)
            statusView.alpha = 1.0f
            
            // 데이터베이스에서 해당 날짜의 태스크 존재 여부 확인
            if (dbHelper.hasTasksForDate(userId, date)) {
                // 태스크가 존재하는 경우: 완료 상태에 따라 UI 구분
                if (dbHelper.areAllTasksCompletedForDate(userId, date)) {
                    // 모든 태스크 완료: 초록색 체크박스 표시
                    statusView.setImageResource(android.R.drawable.checkbox_on_background)
                    statusView.setColorFilter(getColor(R.color.task_completed))
                } else {
                    // 일부 태스크 미완료: 빨간색 빈 원 표시
                    statusView.setImageResource(android.R.drawable.radiobutton_off_background)
                    statusView.setColorFilter(getColor(R.color.task_incomplete))
                }
            } else {
                // 태스크 없음: 연한 회색 빈 원 표시
                statusView.setImageResource(android.R.drawable.radiobutton_off_background)
                statusView.setColorFilter(getColor(R.color.light_gray))
            }
        }
    }
    
    /**
     * 캘린더의 상태 표시를 실시간으로 새로고침하는 함수
     * 
     * 기능:
     * - 현재 주의 날짜들을 다시 계산
     * - 각 날짜별 태스크 완료 상태를 최신 정보로 업데이트
     * - 태스크 상태 변경 후 즉시 캘린더에 반영
     * 
     * setupCalendar()과의 차이점:
     * - setupCalendar(): 최초 1회 전체 캘린더 UI 설정 (날짜 텍스트 + 상태 표시)
     * - refreshCalendar(): 상태 표시만 업데이트 (날짜 텍스트는 변경 없음)
     * 
     * 호출 시점:
     * - TodoAdapter에서 체크박스 상태 변경 시
     * - 새로운 태스크 추가 시
     * - 태스크 완료/미완료 토글 시
     * - onResume()에서 다른 화면에서 돌아올 때
     * 
     * 사용 예시:
     * 사용자가 체크박스를 체크 → TodoAdapter가 DB 업데이트 → refreshCalendar() 호출
     * → 해당 날짜의 상태가 즉시 빨간색 원에서 초록색 체크로 변경
     */
    private fun refreshCalendar() {
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        // 이번주 일요일 날짜 계산 (setupCalendar()와 동일한 로직)
        val daysToSunday = if (currentDayOfWeek == Calendar.SUNDAY) 0 else currentDayOfWeek - Calendar.SUNDAY
        calendar.add(Calendar.DAY_OF_YEAR, -daysToSunday)
        
        // 이번주 날짜들 재계산 (yyyy-MM-dd 형태)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val weekDates = mutableListOf<String>()
        
        for (i in 0..6) {
            val weekCalendar = Calendar.getInstance()
            weekCalendar.time = calendar.time
            weekCalendar.add(Calendar.DAY_OF_YEAR, i)
            weekDates.add(dateFormat.format(weekCalendar.time))
        }
        
        // 최신 태스크 상태로 캘린더 상태 표시 업데이트
        updateCalendarStatus(weekDates)
    }

    /**
     * 캘린더의 각 날짜 클릭 이벤트를 설정하는 함수
     * 
     * 기능:
     * 1. 7개 날짜 컨테이너에 클릭 리스너 등록
     * 2. 클릭된 날짜를 selectedDate로 설정
     * 3. 해당 날짜의 태스크들을 투두리스트에 로드
     * 4. 선택된 날짜 시각적 하이라이트 표시
     * 
     * 클릭 시 동작 순서:
     * 1. selectedDate 변수 업데이트 (yyyy-MM-dd 형태)
     * 2. selectedDateText UI 업데이트 (화면 상단에 선택된 날짜 표시)
     * 3. loadExistingTasks() 호출 → 해당 날짜의 태스크들을 RecyclerView에 로드
     * 4. highlightSelectedDate() 호출 → 선택된 날짜 배경색 변경
     * 5. Toast 메시지로 사용자에게 선택 확인
     * 
     * UI 연동:
     * - activity_test.xml의 각 날짜 컨테이너 (sundayContainer ~ saturdayContainer)
     * - selectedDateText: 현재 선택된 날짜 표시
     * - RecyclerView: 선택된 날짜의 태스크 목록 표시
     * 
     * 사용자 경험:
     * - 월요일 클릭 → 월요일의 태스크만 아래 리스트에 표시
     * - 다른 날짜 클릭 → 즉시 해당 날짜의 태스크로 전환
     * - 날짜별 태스크 관리 가능
     */
    private fun setupCalendarClickListeners() {
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        // 이번주 일요일 날짜 계산 (날짜 배열과 클릭 인덱스 매칭을 위해)
        val daysToSunday = if (currentDayOfWeek == Calendar.SUNDAY) 0 else currentDayOfWeek - Calendar.SUNDAY
        calendar.add(Calendar.DAY_OF_YEAR, -daysToSunday)
        
        // 클릭 이벤트에서 사용할 이번주 날짜들 계산
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val weekDates = mutableListOf<String>()
        
        for (i in 0..6) {
            val weekCalendar = Calendar.getInstance()
            weekCalendar.time = calendar.time
            weekCalendar.add(Calendar.DAY_OF_YEAR, i)
            weekDates.add(dateFormat.format(weekCalendar.time))
        }
        
        // 각 날짜 컨테이너 (일요일~토요일) UI 요소 배열
        val containers = listOf(
            findViewById<LinearLayout>(R.id.sundayContainer),
            findViewById<LinearLayout>(R.id.mondayContainer),
            findViewById<LinearLayout>(R.id.tuesdayContainer),
            findViewById<LinearLayout>(R.id.wednesdayContainer),
            findViewById<LinearLayout>(R.id.thursdayContainer),
            findViewById<LinearLayout>(R.id.fridayContainer),
            findViewById<LinearLayout>(R.id.saturdayContainer)
        )
        
        // 각 날짜 컨테이너에 클릭 리스너 등록
        containers.forEachIndexed { index, container ->
            container.setOnClickListener {
                // 1. 선택된 날짜 업데이트
                selectedDate = weekDates[index]
                selectedDateText.text = selectedDate
                
                // 2. 해당 날짜의 태스크들을 투두리스트에 로드
                loadExistingTasks()
                
                // 3. 선택된 날짜 시각적 하이라이트 표시
                highlightSelectedDate(index)
                
                // 4. 사용자에게 선택 확인 Toast 표시
                Toast.makeText(this, "선택된 날짜: ${weekDates[index]}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 선택된 날짜를 시각적으로 하이라이트 표시하는 함수
     * 
     * @param selectedIndex 선택된 날짜의 인덱스 (0=일요일, 1=월요일, ..., 6=토요일)
     * 
     * 동작 과정:
     * 1. 모든 날짜 컨테이너를 순회
     * 2. 선택된 인덱스에 해당하는 컨테이너만 하이라이트 적용
     * 3. 나머지 컨테이너는 기본 상태로 복원
     * 
     * 하이라이트 효과:
     * - 선택된 날짜: 빨간색 배경 + 30% 투명도 (부드러운 강조 효과)
     * - 선택되지 않은 날짜: 투명 배경 + 100% 불투명 (기본 상태)
     * 
     * UI 피드백:
     * - 사용자가 어떤 날짜를 선택했는지 명확하게 표시
     * - 현재 투두리스트에 표시되는 태스크들이 어느 날짜인지 구분 가능
     * - 다른 날짜 클릭 시 하이라이트가 즉시 이동
     * 
     * 호출 시점:
     * - setupCalendarClickListeners()에서 날짜 클릭 시
     * - 초기 화면 로드 시 오늘 날짜 하이라이트 (필요한 경우)
     */
    private fun highlightSelectedDate(selectedIndex: Int) {
        // 모든 날짜 컨테이너 UI 요소 배열
        val containers = listOf(
            findViewById<LinearLayout>(R.id.sundayContainer),
            findViewById<LinearLayout>(R.id.mondayContainer),
            findViewById<LinearLayout>(R.id.tuesdayContainer),
            findViewById<LinearLayout>(R.id.wednesdayContainer),
            findViewById<LinearLayout>(R.id.thursdayContainer),
            findViewById<LinearLayout>(R.id.fridayContainer),
            findViewById<LinearLayout>(R.id.saturdayContainer)
        )
        
        // 모든 컨테이너를 순회하며 선택 상태에 따라 UI 적용
        containers.forEachIndexed { index, container ->
            if (index == selectedIndex) {
                // 선택된 날짜: 빨간색 배경 + 부드러운 투명도 효과
                container.setBackgroundColor(getColor(R.color.red))
                container.alpha = 0.3f
            } else {
                // 선택되지 않은 날짜: 기본 상태 (투명 배경, 완전 불투명)
                container.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                container.alpha = 1.0f
            }
        }
    }

    /**
     * 선택된 날짜의 기존 태스크들을 데이터베이스에서 로드하여 투두리스트에 표시하는 함수
     * 
     * 동작 과정:
     * 1. selectedDate에 해당하는 태스크들을 데이터베이스에서 조회
     * 2. 각 태스크의 태그 이름을 별도로 조회하여 추가
     * 3. Task 객체를 TodoItem 객체로 변환
     * 4. TodoAdapter에 변환된 데이터 전달하여 RecyclerView 업데이트
     * 
     * 데이터 변환 과정:
     * Task (DB 엔티티) → TodoItem (UI 데이터 클래스)
     * - task.id → todoItem.id
     * - task.title → todoItem.text
     * - task.tagId → tagName 조회 → todoItem.tagName
     * - selectedDate → todoItem.date
     * - task.isCompleted → todoItem.isDone
     * 
     * UI 연동:
     * - RecyclerView에 선택된 날짜의 태스크만 표시
     * - 체크박스로 완료/미완료 상태 표시
     * - "1. 태그명 : 태스크 제목" 형태로 표시
     * 
     * 호출 시점:
     * - onCreate(): 초기 화면 로드 시 (오늘 날짜 태스크들)
     * - setupCalendarClickListeners(): 다른 날짜 클릭 시
     * - onResume(): 다른 화면에서 돌아올 때
     * 
     * 사용자 경험:
     * - 월요일 클릭 → 월요일에 등록된 태스크들만 보임
     * - 화요일 클릭 → 즉시 화요일 태스크들로 전환
     * - 날짜별 태스크 관리 가능
     */
    private fun loadExistingTasks() {
        // 1. 선택된 날짜의 태스크들을 데이터베이스에서 조회
        val tasksForDate = dbHelper.getTasksByDate(userId, selectedDate)
        
        // 2. Task 객체를 TodoItem 객체로 변환 (UI 표시용)
        val todoItems = tasksForDate.map { task ->
            // 태그 ID로 태그 이름 조회 (표시용)
            val tagName = dbHelper.getTag(task.tagId) ?: "태그 없음"
            
            // UI에서 사용할 TodoItem 객체 생성
            TodoItem(
                id = task.id,                    // 태스크 고유 ID
                text = task.title,               // 태스크 제목
                tagName = tagName,               // 태그 이름 (예: "수학", "영어")
                date = selectedDate,             // 스케줄된 날짜
                isDone = task.isCompleted        // 완료 여부 (체크박스 상태)
            )
        }
        
        // 3. TodoAdapter에 변환된 데이터 전달하여 RecyclerView 업데이트
        todoAdapter.setItems(todoItems)
    }

    /**
     * 새 태스크 추가 시 날짜 선택을 위한 DatePicker 다이얼로그를 표시하는 함수
     * 
     * @param onDateSelected 날짜 선택 완료 시 호출되는 콜백 함수 (선택된 날짜 문자열을 매개변수로 받음)
     * 
     * 기능:
     * - Android 기본 DatePickerDialog 표시
     * - 사용자가 날짜 선택 후 "확인" 버튼 클릭 시 콜백 함수 호출
     * - 오늘 날짜를 기본값으로 설정
     * 
     * 콜백 함수 동작:
     * 1. 사용자가 선택한 연/월/일을 받음
     * 2. yyyy-MM-dd 형태의 문자열로 변환
     * 3. onDateSelected 콜백 함수에 전달
     * 
     * 호출 시점:
     * - listBtn 클릭 시 (새 태스크 추가 버튼)
     * - 태스크에 스케줄 날짜를 지정하기 위해
     * 
     * 사용 예시:
     * showDatePickerDialog { selectedDate ->
     *     // selectedDate = "2024-01-15"
     *     dbHelper.addTaskWithDate(userId, tagId, title, selectedDate)
     * }
     * 
     * UI 흐름:
     * 1. 사용자가 태스크 제목 입력
     * 2. 태그 선택
     * 3. "+" 버튼 클릭
     * 4. DatePicker 다이얼로그 표시 ← 이 함수
     * 5. 날짜 선택 후 태스크가 해당 날짜에 저장
     * 6. 캘린더 상태 업데이트
     */
    private fun showDatePickerDialog(onDateSelected: (String) -> Unit) {
        // 현재 날짜를 기본값으로 설정
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // DatePickerDialog 생성 및 표시
        val datePickerDialog = android.app.DatePickerDialog(
            this, 
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                // 사용자가 날짜 선택 시 실행되는 콜백
                calendar.set(selectedYear, selectedMonth, selectedDayOfMonth)
                
                // 선택된 날짜를 yyyy-MM-dd 형태 문자열로 변환
                val selectedDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                
                // 외부에서 전달받은 콜백 함수 실행 (선택된 날짜 전달)
                onDateSelected(selectedDateString)
            }, 
            year, month, day
        )
        
        // 다이얼로그 표시
        datePickerDialog.show()
    }
}