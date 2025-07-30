package com.example.guru2_3

import DatabaseHelper
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.highlight.Highlight
import android.view.MotionEvent
import android.view.View
import android.widget.Switch
import java.util.Calendar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


class TagInfoActivity : AppCompatActivity(), OnChartValueSelectedListener {

    private lateinit var scoreChart: LineChart
    private lateinit var timeChart: LineChart
    private lateinit var createTagFinishRate: TextView
    private lateinit var createTagDdayText: TextView
    private lateinit var dateSettingButton: android.widget.Button
    private lateinit var taskRecyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var tagMoveicon: ImageView
    private lateinit var tagMoveText: TextView
    private lateinit var scoreInput: EditText
    private lateinit var createTagNameTextView: TextView
    private var tagName: String = ""
    private var examDate: String? = null

    // 데이터 저장용 리스트
    private val scoreEntries = mutableListOf<Entry>()
    private val timeEntries = mutableListOf<Entry>()
    private var currentScoreIndex = 0f
    private var currentTimeIndex = 0f
    private lateinit var dbHelper: DatabaseHelper
    private var currentTagId: Long = 1 // Intent로 받아올 태그 ID (임시로 1 설정)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_tag_info)

            tagName = intent.getStringExtra("TAG_NAME") ?: "기본태그"
            currentTagId = intent.getLongExtra("TAG_ID", -1)
            
            // 디버깅 로그
            Toast.makeText(this, "TagInfoActivity 시작: $tagName (ID: $currentTagId)", Toast.LENGTH_SHORT).show()
            
            // 태그 ID 검증
            if (currentTagId == -1L) {
                Toast.makeText(this, "올바르지 않은 태그 정보입니다.", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            dbHelper = DatabaseHelper(this) // initViews 전에 dbHelper 초기화
            
            initViews()
            setupTagName()
            setupCharts()
            setClickListeners()
            
            // 데이터 로딩을 개별적으로 try-catch로 감싸기
            try {
                loadDataFromDatabase()
            } catch (e: Exception) {
                Toast.makeText(this, "데이터 로딩 중 오류: ${e.message}", Toast.LENGTH_LONG).show()
                // 기본 UI는 유지
                updateCompletionRate()
                updateDdayDisplay()
            }
            
            Toast.makeText(this, "TagInfoActivity 초기화 완료", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(this, "TagInfoActivity 오류: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    /**
     * UI 뷰들을 초기화하는 함수 (오류 처리 추가)
     */
    private fun initViews() {
        try {
            scoreChart = findViewById(R.id.scoreChart)
            timeChart = findViewById(R.id.timeChart)
            createTagNameTextView = findViewById(R.id.createTagNameTextView)
            createTagFinishRate = findViewById(R.id.createTagFinishRate)
            createTagDdayText = findViewById(R.id.createTagDdayText)
            dateSettingButton = findViewById(R.id.dateSettingButton)
            taskRecyclerView = findViewById(R.id.taskRecyclerView)
            scoreInput = findViewById(R.id.scoreInput)
            tagMoveicon = findViewById(R.id.tagMoveicon)
            tagMoveText = findViewById(R.id.tagMoveText)
        } catch (e: Exception) {
            Toast.makeText(this, "UI 초기화 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupTagName() {
        createTagNameTextView.setText(tagName)
        createTagNameTextView.isEnabled = false
        createTagNameTextView.setOnClickListener(null)

        createTagNameTextView.setOnClickListener {
            showEditTagNameDialog()
        }
    }

    private fun showEditTagNameDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("태그 이름 수정")

        val input = EditText(this)
        input.setText(tagName)
        builder.setView(input)

        builder.setPositiveButton("수정") { _, _ ->
            val newTagName = input.text.toString()
            if (newTagName.isNotEmpty() && newTagName != tagName) {
                val result = dbHelper.updateTagName(currentTagId, newTagName)
                if (result > 0) {
                    tagName = newTagName
                    createTagNameTextView.text = tagName
                    Toast.makeText(this, "태그 이름이 변경되었습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("취소") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun saveTagName() {
        val newTagName = createTagNameTextView.text.toString()
        if (newTagName.isNotEmpty() && newTagName != tagName) {
            val result = dbHelper.updateTagName(currentTagId, newTagName)
            if (result > 0) {
                tagName = newTagName
                Toast.makeText(this, "태그 이름이 변경되었습니다", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "태그 이름 변경에 실패했습니다", Toast.LENGTH_SHORT).show()
                // 실패 시 원래 이름으로 되돌리기
                createTagNameTextView.setText(tagName)
            }
        }
    }

    private fun setupCharts() {
        setupScoreChart()
        setupTimeChart()
    }

    // 뒤로 가기 시 태그 이름 저장
    override fun onPause() {
        super.onPause()
        saveTagName()
    }

    private fun setupScoreChart() {
        scoreChart.apply {
            setDragEnabled(false)  // 추가: 드래그 비활성화
            setScaleEnabled(false) // 추가: 스케일 비활성화
            setPinchZoom(false)
            description.text = "성적 그래프 (클릭해서 데이터 추가)"
            description.textSize = 12f
            description.textColor = Color.BLACK
            setNoDataText("차트를 클릭해서 성적을 추가하세요")
            setNoDataTextColor(Color.WHITE)
            setTouchEnabled(true)

            // X축 설정
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.WHITE
                setDrawGridLines(true)
                granularity = 1f
            }

            // Y축 설정
            axisLeft.apply {
                textColor = Color.WHITE
                axisMinimum = 0f
                //axisMaximum = 100f
                setDrawGridLines(true)
            }

            axisRight.isEnabled = false

            // 범례 설정
            legend.textColor = Color.WHITE

            setOnChartValueSelectedListener(this@TagInfoActivity)
            onChartGestureListener = object : OnChartGestureListener {
                override fun onChartGestureStart(me: MotionEvent?, lastPerformedGesture: com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture?) {}
                override fun onChartGestureEnd(me: MotionEvent?, lastPerformedGesture: com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture?) {}
                override fun onChartLongPressed(me: MotionEvent?) {}
                override fun onChartDoubleTapped(me: MotionEvent?) {}
                override fun onChartSingleTapped(me: MotionEvent?) {
                    showScoreInputDialog()
                }
                override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) {}
                override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {}
                override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {}
            }

            // 투명한 더미 데이터 추가 (클릭 감지를 위해)
            setupDummyScoreData()


        }
    }

    private fun setupTimeChart() {
        timeChart.apply {
            description.text = "시간 그래프 (클릭해서 데이터 추가)"
            description.textSize = 12f
            description.textColor = Color.BLACK

            setNoDataText("차트를 클릭해서 시간을 추가하세요")
            setNoDataTextColor(Color.WHITE)
            setTouchEnabled(true)

            // X축 설정
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.WHITE
                setDrawGridLines(true)
                granularity = 1f
            }

            // Y축 설정
            axisLeft.apply {
                textColor = Color.WHITE
                axisMinimum = 0f
                axisMaximum = 24f
                setDrawGridLines(true)
            }

            axisRight.isEnabled = false

            // 범례 설정
            legend.textColor = Color.WHITE


            // 터치 제스처 리스너 설정
            setOnChartValueSelectedListener(this@TagInfoActivity)
            onChartGestureListener = object : OnChartGestureListener {
                override fun onChartGestureStart(me: MotionEvent?, lastPerformedGesture: com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture?) {}
                override fun onChartGestureEnd(me: MotionEvent?, lastPerformedGesture: com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture?) {}
                override fun onChartLongPressed(me: MotionEvent?) {}
                override fun onChartDoubleTapped(me: MotionEvent?) {}
                override fun onChartSingleTapped(me: MotionEvent?) {
                    showTimeInputDialog()
                }
                override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) {}
                override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {}
                override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {}
            }

            // 투명한 더미 데이터 추가
            setupDummyTimeData()


        }
    }

    private fun showScoreInputDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("성적 입력")

        val input = EditText(this)
        input.hint = "성적을 입력하세요 (0-100)"
        builder.setView(input)

        builder.setPositiveButton("추가") { _, _ ->
            val scoreText = input.text.toString()
            if (scoreText.isNotEmpty()) {
                try {
                    val score = scoreText.toFloat()
                    addScoreData(score)
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "올바른 숫자를 입력하세요", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("취소") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun showTimeInputDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("공부 시간 입력")

        val input = EditText(this)
        input.hint = "시간을 입력하세요 (시간 단위)"
        builder.setView(input)

        builder.setPositiveButton("추가") { _, _ ->
            val timeText = input.text.toString()
            if (timeText.isNotEmpty()) {
                try {
                    val time = timeText.toFloat()
                    addTimeData(time)
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "올바른 숫자를 입력하세요", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("취소") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun addScoreData(score: Float) {
        val result = dbHelper.addScoreData(currentTagId, score)
        if (result != -1L) {
            scoreEntries.add(Entry(currentScoreIndex, score))
            currentScoreIndex++
            updateScoreChart()
        } else {
            Toast.makeText(this, "데이터 저장 실패", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addTimeData(time: Float) {
        val result = dbHelper.addTimeData(currentTagId, time)
        if (result != -1L) {
            timeEntries.add(Entry(currentTimeIndex, time))
            currentTimeIndex++
            updateTimeChart()
        } else {
            Toast.makeText(this, "데이터 저장 실패", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateScoreChart() {
        if (scoreEntries.isEmpty()) {
            setupDummyScoreData() // 빈 데이터일 때는 더미 데이터 유지
            return
        }

        val dataSet = LineDataSet(scoreEntries, "성적").apply {
            color = Color.YELLOW
            setCircleColor(Color.YELLOW)
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(true)
            valueTextColor = Color.WHITE
            valueTextSize = 10f
        }

        val lineData = LineData(dataSet)
        scoreChart.data = lineData
        scoreChart.invalidate()
    }

    private fun updateTimeChart() {
        if (timeEntries.isEmpty()) {
            setupDummyTimeData() // 빈 데이터일 때는 더미 데이터 유지
            return
        }

        val dataSet = LineDataSet(timeEntries, "공부시간").apply {
            color = Color.CYAN
            setCircleColor(Color.CYAN)
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(true)
            valueTextColor = Color.WHITE
            valueTextSize = 10f
        }

        val lineData = LineData(dataSet)
        timeChart.data = lineData
        timeChart.invalidate()
    }

    // 투명한 더미 데이터 설정 (클릭 감지를 위해)
    private fun setupDummyScoreData() {
        val dummyEntries = listOf(Entry(0f, 50f)) // 중간값으로 설정
        val dataSet = LineDataSet(dummyEntries, "").apply {
            color = Color.TRANSPARENT // 투명하게 설정
            setCircleColor(Color.TRANSPARENT)
            setDrawValues(false)
            setDrawCircles(false)
            lineWidth = 0f
            isHighlightEnabled = false
        }

        val lineData = LineData(dataSet)
        scoreChart.data = lineData
        scoreChart.invalidate()
    }

    private fun setupDummyTimeData() {
        val dummyEntries = listOf(Entry(0f, 12f)) // 중간값으로 설정
        val dataSet = LineDataSet(dummyEntries, "").apply {
            color = Color.TRANSPARENT
            setCircleColor(Color.TRANSPARENT)
            setDrawValues(false)
            setDrawCircles(false)
            lineWidth = 0f
            isHighlightEnabled = false
        }

        val lineData = LineData(dataSet)
        timeChart.data = lineData
        timeChart.invalidate()
    }

    private fun setClickListeners() {
        tagMoveicon.setOnClickListener {
            finish()
        }

        tagMoveText.setOnClickListener {
            finish()
        }
    }
    override fun finish(){
        saveTagName()
        super.finish()
    }

    // OnChartValueSelectedListener 구현
    override fun onValueSelected(e: Entry?, h: Highlight?) {
        // 차트의 특정 값이 선택되었을 때의 동작
        e?.let {
            Toast.makeText(this, "선택된 값: ${it.y}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onNothingSelected() {
        // 선택이 해제되었을 때의 동작
    }

    /**
     * 데이터베이스에서 태그 관련 데이터를 로드하는 함수 (오류 처리 개선)
     * 
     * 로드하는 데이터:
     * - 성적 그래프 데이터
     * - 시간 그래프 데이터
     * - 시험 날짜 정보
     * - 완수율 정보
     * - D-day 정보
     * - 태스크 목록
     * 
     * 오류 처리:
     * - 각 단계별 try-catch로 오류 격리
     * - 사용자에게 오류 메시지 표시
     */
    private fun loadDataFromDatabase() {
        try {
            // 성적 데이터 로드
            val scoreData = dbHelper.getScoreData(currentTagId)
            scoreEntries.clear()
            scoreData.forEach { (index, score) ->
                scoreEntries.add(Entry(index, score))
            }
            currentScoreIndex = scoreData.size.toFloat()
            updateScoreChart()

            // 시간 데이터 로드
            val timeData = dbHelper.getTimeData(currentTagId)
            timeEntries.clear()
            timeData.forEach { (index, time) ->
                timeEntries.add(Entry(index, time))
            }
            currentTimeIndex = timeData.size.toFloat()
            updateTimeChart()
            
            // 기타 데이터 로드
            loadExamDateFromDatabase()
            setupDateButton()
            
            // 태스크 RecyclerView 설정을 개별적으로 try-catch로 감싸기
            try {
                setupTaskRecyclerView()
            } catch (e: Exception) {
                Toast.makeText(this, "태스크 목록 설정 중 오류: ${e.message}", Toast.LENGTH_LONG).show()
            }
            
            updateCompletionRate()
            updateDdayDisplay()
            
        } catch (e: Exception) {
            Toast.makeText(this, "데이터 로딩 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_LONG).show()
            // 기본값으로 초기화
            scoreEntries.clear()
            timeEntries.clear()
            updateScoreChart()
            updateTimeChart()
        }
    }



    /**
     * 날짜 설정 버튼 클릭 리스너를 설정하는 함수 (UI 개선 버전)
     * 
     * 기존 UI 개선 사항:
     * - 기존: Switch 토글로 날짜 설정 활성화/비활성화
     * - 개선: 명확한 "날짜 설정" 버튼으로 즉시 DatePicker 호출
     * 
     * 사용자 경험 개선:
     * - 더 직관적인 인터페이스 (버튼 클릭 → 즉시 날짜 선택)
     * - D-day 옆에 위치하여 관련성 명확
     * - 별도의 토글 단계 없이 바로 날짜 설정 가능
     */
    private fun setupDateButton() {
        dateSettingButton.setOnClickListener {
            showDatePicker()
        }
    }

    /**
     * 태스크 목록을 표시하는 RecyclerView를 설정하는 함수 (새로운 기능)
     * 
     * 기능 설명:
     * - 현재 태그에 속한 모든 태스크를 리스트로 표시
     * - 각 태스크의 계획 날짜와 완료 상태를 한 눈에 확인 가능
     * - 체크박스를 통해 실시간으로 완료 상태 토글 가능
     * 
     * UI 구성:
     * - LinearLayoutManager로 세로 리스트 형태
     * - 각 항목: "태스크명 (계획일: yyyy-MM-dd)" + 체크박스
     * - 150dp 고정 높이로 스크롤 가능
     * 
     * 데이터 연동:
     * - loadTasksForTag()에서 데이터베이스 조회 및 어댑터 설정
     * - 체크박스 변경 시 DB 업데이트 및 완수율 새로고침
     */
    private fun setupTaskRecyclerView() {
        // RecyclerView 설정 (태스크 목록 표시용)
        taskRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        
        // 태스크 목록을 로드하여 표시
        loadTasksForTag()
    }

    /**
     * 현재 태그의 모든 태스크를 로드하여 RecyclerView에 표시하는 함수
     * 
     * 개선 사항:
     * - null 체크 강화
     * - 예외 처리 추가
     * - 올바른 레이아웃 사용
     * - 안전한 데이터 접근
     */
    private fun loadTasksForTag() {
        try {
            val tasks = dbHelper.getTasksForTag(currentTagId)
            
            // 간단한 텍스트 어댑터 생성 (개선된 버전)
            val adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
                
                inner class TaskViewHolder(itemView: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
                    val taskText: TextView = itemView.findViewById(R.id.textView)
                    val checkBox: android.widget.CheckBox = itemView.findViewById(R.id.checkBox)
                }
                
                override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
                    val view = android.view.LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_todo, parent, false)
                    return TaskViewHolder(view)
                }
                
                override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                    try {
                        val task = tasks.getOrNull(position) ?: return
                        val taskHolder = holder as TaskViewHolder
                        
                        // null-safe한 데이터 접근
                        val taskTitle = task.title
                        val scheduledDateText = task.scheduledDate ?: "미설정"
                        val isCompleted = task.isCompleted
                        
                        taskHolder.taskText.text = "$taskTitle (계획일: $scheduledDateText)"
                        taskHolder.checkBox.isChecked = isCompleted
                        
                        // 체크박스 리스너 설정 (null 체크 포함)
                        taskHolder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                            try {
                                dbHelper.updateTaskCompletion(task.id, isChecked)
                                updateCompletionRate()
                            } catch (e: Exception) {
                                Toast.makeText(this@TagInfoActivity, "태스크 상태 업데이트 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@TagInfoActivity, "태스크 표시 중 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun getItemCount(): Int = tasks.size
            }
            
            taskRecyclerView.adapter = adapter
            
        } catch (e: Exception) {
            Toast.makeText(this, "태스크 목록 로딩 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                examDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                updateDdayDisplay()       // D-day 표시 업데이트
                dbHelper.updateExamDate(currentTagId, examDate)
            },
            year, month, day
        )

        datePickerDialog.show()
    }

    private fun loadExamDateFromDatabase() {
        examDate = dbHelper.getExamDate(currentTagId)
        // D-day 표시는 updateDdayDisplay()에서 처리됨
    }

    /**
     * 현재 태그의 완수율을 계산하여 UI에 표시하는 함수
     * 
     * @see createTagFinishRate TextView에 완수율을 표시
     * 
     * 동작 과정:
     * 1. currentTagId를 사용하여 데이터베이스에서 완수율 계산
     * 2. "완수율: XX%" 형태로 TextView에 표시
     * 
     * 호출 시점:
     * - onCreate(): 화면 최초 로드 시
     * - onResume(): 다른 화면에서 돌아올 때 (예: MainActivity에서 태스크 완료 후)
     * 
     * UI 업데이트 대상:
     * activity_tag_info.xml의 createTagFinishRate TextView
     */
    private fun updateCompletionRate() {
        // DatabaseHelper를 통해 현재 태그의 완수율 계산
        val completionRate = dbHelper.getTagCompletionRate(currentTagId)
        
        // UI에 "완수율: XX%" 형태로 표시
        createTagFinishRate.text = "완수율: ${completionRate}%"
    }

    /**
     * 현재 태그의 D-day를 계산하여 완수율 아래에 표시하는 함수
     * 
     * @see createTagDdayText TextView에 D-day를 표시 (완수율 아래에 항상 표시)
     * 
     * 동작 과정:
     * 1. currentTagId를 사용하여 데이터베이스에서 시험 날짜 조회
     * 2. 현재 날짜와 비교하여 D-day 계산
     * 3. 계산된 D-day를 적절한 형식으로 표시
     * 
     * D-day 표시 형식:
     * - 시험일 전: "D-5 (2024-01-15)"
     * - 시험일 당일: "D-Day! (2024-01-10)"
     * - 시험일 후: "D+3 (2024-01-07)"
     * - 날짜 미설정: "D-day: 설정되지 않음"
     * 
     * 호출 시점:
     * - onCreate(): 화면 최초 로드 시
     * - onResume(): 다른 화면에서 돌아올 때
     * - 시험 날짜 변경 시 (DatePicker에서 날짜 선택 후)
     */
    private fun updateDdayDisplay() {
        // 데이터베이스에서 현재 태그의 시험 날짜 조회
        val examDate = dbHelper.getExamDate(currentTagId)
        
        if (examDate != null) {
            try {
                // 시험 날짜 파싱 및 D-day 계산 (Calendar 사용)
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val examDateParsed = dateFormat.parse(examDate)
                val today = java.util.Calendar.getInstance().time
                
                if (examDateParsed != null) {
                    // 두 날짜 간의 차이를 일 단위로 계산
                    val timeDiff = examDateParsed.time - today.time
                    val daysUntilExam = (timeDiff / (1000 * 60 * 60 * 24)).toInt()
                    
                    // D-day 형식에 따라 텍스트 설정
                    val displayText = when {
                        daysUntilExam > 0 -> "D-${daysUntilExam} ($examDate)"
                        daysUntilExam == 0 -> "D-Day! ($examDate)"
                        else -> "D+${-daysUntilExam} ($examDate)"
                    }
                    
                    createTagDdayText.text = displayText
                } else {
                    createTagDdayText.text = "시험일: $examDate"
                }
            } catch (e: Exception) {
                // 날짜 파싱 실패 시 원본 날짜 표시
                createTagDdayText.text = "시험일: $examDate"
            }
        } else {
            // 시험 날짜가 설정되지 않은 경우
            createTagDdayText.text = "D-day: 설정되지 않음"
        }
    }

    /**
     * TagInfoActivity가 다시 활성화될 때 호출되는 함수
     * 
     * 사용자가 MainActivity에서 태스크를 완료/미완료로 변경한 후
     * 이 화면으로 돌아올 때 최신 완수율과 D-day를 반영하기 위해 필요
     * 
     * 예시 시나리오:
     * 1. TagInfoActivity에서 완수율 50%, D-5 확인
     * 2. MainActivity로 이동하여 태스크 2개 더 완료
     * 3. 다시 TagInfoActivity로 돌아옴
     * 4. onResume()에서 업데이트 함수들 호출
     * 5. 업데이트된 완수율 70%, D-day 표시
     */
    override fun onResume() {
        super.onResume()
        updateCompletionRate() // 화면으로 돌아올 때마다 완수율 업데이트
        updateDdayDisplay()    // 화면으로 돌아올 때마다 D-day 업데이트
    }





}