package com.example.guru2_3

import DatabaseHelper
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.LinearLayout
import android.widget.EditText
import android.widget.ScrollView
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import android.graphics.Color
import androidx.core.graphics.toColorInt
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date
import android.app.DatePickerDialog
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.highlight.Highlight
import android.view.MotionEvent
import android.app.AlertDialog

class TagActivity : AppCompatActivity(), OnChartValueSelectedListener {
    private lateinit var scrollContainer: LinearLayout
    private lateinit var tagaddicon: ImageView
    private lateinit var tagaddText: TextView
    private lateinit var dbHelper: DatabaseHelper
    private var tagCounter = 0 // 태그 고유 ID 생성용
    private var userId: Long = 0
    
    // 차트 데이터 저장용
    private val scoreEntriesMap = mutableMapOf<Long, MutableList<Entry>>()
    private val timeEntriesMap = mutableMapOf<Long, MutableList<Entry>>()
    private val scoreIndexMap = mutableMapOf<Long, Float>()
    private val timeIndexMap = mutableMapOf<Long, Float>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = intent.getLongExtra("USER_ID", 1) // 기본값을 1로 설정

        enableEdgeToEdge()
        setContentView(R.layout.activity_tag)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 데이터베이스 헬퍼 초기화
        dbHelper = DatabaseHelper(this)

        // 뷰 초기화
        val mainLayout = findViewById<ConstraintLayout>(R.id.main)
        tagaddicon = findViewById(R.id.tagaddicon)
        tagaddText = findViewById(R.id.tagaddText)

        // 스크롤 가능한 컨테이너 생성
        val scrollView = ScrollView(this)
        scrollContainer = LinearLayout(this)
        scrollContainer.orientation = LinearLayout.VERTICAL
        scrollContainer.setPadding(50, 0, 50, 50)

        scrollView.addView(scrollContainer)

        // ScrollView를 ConstraintLayout에 추가
        val scrollParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            0
        )

        scrollParams.topToBottom = R.id.tagaddicon
        scrollParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        scrollParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        scrollParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        scrollParams.setMargins(0, (20 * resources.displayMetrics.density).toInt(), 0, 0)

        scrollView.layoutParams = scrollParams
        scrollView.id = android.view.View.generateViewId()

        mainLayout.addView(scrollView)

        // 기존 태그를 새 컨테이너로 이동 (임시 비활성화)
        // moveExistingTagToScrollContainer()

        // 토마토 아이콘 클릭 리스너
        tagaddicon.setOnClickListener {
            addNewTag()
        }

        // + 텍스트 클릭 리스너
        tagaddText.setOnClickListener {
            addNewTag()
        }

        // 안전하게 기존 태그 로드
        try {
            loadExistingTags()
        } catch (e: Exception) {
            Toast.makeText(this, "태그 로딩 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

//    private fun moveExistingTagToScrollContainer() {
//        val originalTagContainer = findViewById<LinearLayout>(R.id.tagContainer)
//        originalTagContainer?.let { original ->
//            val nameEditText = original.findViewById<EditText>(R.id.createTagNameEditText)
//            val tagName = nameEditText?.hint?.toString() ?: "태그 이름"
//
//            // 데이터베이스에 첫 번째 태그 생성
//            val tagId = dbHelper.createTag(tagName)
//            val firstTag = createTagView(tagName, tagId,  isEditMode = true)
//            scrollContainer.addView(firstTag)
//
//            original.visibility = android.view.View.GONE
//        }
//    }

    private fun addNewTag() {
        tagCounter++
        val tagName = "태그 이름 $tagCounter"

        // 데이터베이스에 새 태그 생성
        val tagId = dbHelper.createTag(tagName)
        val newTag = createTagView(tagName, tagId, isEditMode = false)
        val tag = dbHelper.addTag(userId, tagName)
        scrollContainer.addView(newTag)

        Toast.makeText(this, "새 태그가 추가되었습니다!", Toast.LENGTH_SHORT).show()
    }

    /**
     * 새로운 태그 뷰를 동적으로 생성하는 함수 (토글 기능 추가 버전)
     * 
     * @param tagName 생성할 태그의 이름
     * @param tagId 태그의 고유 ID (데이터베이스에서 자동 생성)
     * @param isEditMode 편집 모드 여부 (현재 미사용)
     * @return 생성된 태그 컨테이너 (LinearLayout)
     * 
     * UI 구조 (토글 기능 포함):
     * LinearLayout (태그 컨테이너)
     * ├── LinearLayout (상단 기본 정보 - 항상 표시)
     * │   ├── EditText (태그 이름)
     * │   ├── TextView (D-day)
     * │   ├── TextView (완수율)
     * │   └── ImageView (토글 삼각형)
     * └── LinearLayout (하단 추가 정보 - 토글로 표시/숨김)
     *     ├── TextView (태스크 목록 제목)
     *     ├── LinearLayout (태스크 목록 컨테이너)
     *     ├── LinearLayout (날짜 설정 기능)
     *     ├── LinearLayout (성적 그래프)
     *     ├── LinearLayout (시간 그래프)
     *     └── Button (수정하기 버튼)
     * 
     * 토글 기능:
     * - 기본적으로 태그 이름, D-day, 완수율만 표시
     * - 삼각형 클릭 시 추가 정보 표시/숨김
     * - 추가 정보: 태스크 목록, 날짜 설정, 성적/시간 그래프
     */
    private fun createTagView(tagName: String, tagId: Long, isEditMode: Boolean): LinearLayout {
        val dpToPx = resources.displayMetrics.density

        // 메인 태그 컨테이너 생성
        val tagContainer = LinearLayout(this)
        tagContainer.orientation = LinearLayout.VERTICAL
        tagContainer.setBackgroundColor("#F44336".toColorInt())
        tagContainer.tag = tagId // 태그 ID를 View의 tag로 저장

        // 레이아웃 파라미터 설정 (높이를 가변적으로 변경)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT // 토글에 따라 높이가 변함
        )
        layoutParams.topMargin = (20 * dpToPx).toInt()
        tagContainer.layoutParams = layoutParams
        tagContainer.id = android.view.View.generateViewId()

        // === 상단 기본 정보 컨테이너 (항상 표시) ===
        val basicInfoContainer = LinearLayout(this)
        basicInfoContainer.orientation = LinearLayout.VERTICAL
        basicInfoContainer.setPadding(
            (16 * dpToPx).toInt(),
            (12 * dpToPx).toInt(),
            (16 * dpToPx).toInt(),
            (12 * dpToPx).toInt()
        )

        // 태그 이름과 토글 버튼을 가로로 배치하는 컨테이너
        val nameToggleContainer = LinearLayout(this)
        nameToggleContainer.orientation = LinearLayout.HORIZONTAL

        // 1. EditText 추가 (태그 이름)
        val nameEditText = EditText(this)
        nameEditText.setText(tagName)
        nameEditText.setTypeface(null, android.graphics.Typeface.BOLD)
        nameEditText.textSize = 18f
        nameEditText.setTextColor(Color.BLACK)
        nameEditText.setBackgroundColor(Color.WHITE)
        nameEditText.setPadding(
            (12 * dpToPx).toInt(),
            (8 * dpToPx).toInt(),
            (12 * dpToPx).toInt(),
            (8 * dpToPx).toInt()
        )
        
        val nameEditParams = LinearLayout.LayoutParams(
            0,
            (40 * dpToPx).toInt(),
            1f // weight 1로 설정하여 나머지 공간 차지
        )
        nameEditText.layoutParams = nameEditParams

        // EditText 변경 감지 리스너
        nameEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val newTagName = nameEditText.text.toString()
                if (newTagName.isNotEmpty()) {
                    dbHelper.updateTagName(tagId, newTagName)
                }
            }
        }

        // 2. 토글 삼각형 ImageView 추가
        val toggleButton = ImageView(this)
        toggleButton.setImageResource(android.R.drawable.arrow_down_float) // 기본 하향 화살표
        toggleButton.scaleType = ImageView.ScaleType.CENTER_INSIDE
        toggleButton.setBackgroundColor(Color.parseColor("#FF9800"))
        toggleButton.setPadding(
            (8 * dpToPx).toInt(),
            (8 * dpToPx).toInt(),
            (8 * dpToPx).toInt(),
            (8 * dpToPx).toInt()
        )
        
        val toggleParams = LinearLayout.LayoutParams(
            (40 * dpToPx).toInt(),
            (40 * dpToPx).toInt()
        )
        toggleParams.setMargins((8 * dpToPx).toInt(), 0, 0, 0)
        toggleButton.layoutParams = toggleParams

        // 3. D-day 텍스트
        val ddayText = TextView(this)
        ddayText.text = "D-day: 설정되지 않음"
        ddayText.setTextColor(Color.BLACK)
        ddayText.textSize = 16f
        val ddayParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        ddayParams.topMargin = (8 * dpToPx).toInt()
        ddayText.layoutParams = ddayParams

        // 4. 완수율 텍스트
        val finishRateText = TextView(this)
        finishRateText.text = "완수율: 0%"
        finishRateText.setTextColor(Color.BLACK)
        finishRateText.textSize = 16f
        val finishRateParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        finishRateParams.topMargin = (4 * dpToPx).toInt()
        finishRateText.layoutParams = finishRateParams

        // 이름+토글 컨테이너에 뷰들 추가
        nameToggleContainer.addView(nameEditText)
        nameToggleContainer.addView(toggleButton)

        // 기본 정보 컨테이너에 뷰들 추가
        basicInfoContainer.addView(nameToggleContainer)
        basicInfoContainer.addView(ddayText)
        basicInfoContainer.addView(finishRateText)

        // === 하단 추가 정보 컨테이너 (토글로 표시/숨김) ===
        val additionalInfoContainer = LinearLayout(this)
        additionalInfoContainer.orientation = LinearLayout.VERTICAL
        additionalInfoContainer.setPadding(
            (16 * dpToPx).toInt(),
            (12 * dpToPx).toInt(),
            (16 * dpToPx).toInt(),
            (12 * dpToPx).toInt()
        )
        additionalInfoContainer.visibility = android.view.View.GONE // 기본적으로 숨김

        // 5. 태스크 목록 섹션
        val taskListTitle = TextView(this)
        taskListTitle.text = "📋 태스크 목록"
        taskListTitle.setTextColor(Color.BLACK)
        taskListTitle.textSize = 16f
        taskListTitle.setTypeface(null, android.graphics.Typeface.BOLD)
        val taskTitleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        taskTitleParams.bottomMargin = (8 * dpToPx).toInt()
        taskListTitle.layoutParams = taskTitleParams

        // 태스크 목록 컨테이너
        val taskListContainer = LinearLayout(this)
        taskListContainer.orientation = LinearLayout.VERTICAL
        val taskContainerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        taskContainerParams.bottomMargin = (16 * dpToPx).toInt()
        taskListContainer.layoutParams = taskContainerParams

        // 6. 날짜 설정 기능
        val dateSettingContainer = LinearLayout(this)
        dateSettingContainer.orientation = LinearLayout.HORIZONTAL
        
        val dateSettingTitle = TextView(this)
        dateSettingTitle.text = "📅 시험/마감일 설정:"
        dateSettingTitle.setTextColor(Color.BLACK)
        dateSettingTitle.textSize = 14f
        dateSettingTitle.setTypeface(null, android.graphics.Typeface.BOLD)
        
        val dateButton = android.widget.Button(this)
        dateButton.text = "날짜 선택"
        dateButton.textSize = 12f
        dateButton.setBackgroundColor(Color.parseColor("#4CAF50"))
        dateButton.setTextColor(Color.WHITE)
        dateButton.setPadding(
            (12 * dpToPx).toInt(),
            (6 * dpToPx).toInt(),
            (12 * dpToPx).toInt(),
            (6 * dpToPx).toInt()
        )
        
        val dateBtnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            (32 * dpToPx).toInt()
        )
        dateBtnParams.setMargins((8 * dpToPx).toInt(), 0, 0, 0)
        dateButton.layoutParams = dateBtnParams
        
        // 날짜 선택 기능 구현
        dateButton.setOnClickListener {
            showDatePickerDialog(tagId)
        }
        
        dateSettingContainer.addView(dateSettingTitle)
        dateSettingContainer.addView(dateButton)
        
        val dateContainerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        dateContainerParams.bottomMargin = (16 * dpToPx).toInt()
        dateSettingContainer.layoutParams = dateContainerParams

        // 7. 성적 그래프 섹션 (실제 LineChart)
        val scoreGraphContainer = LinearLayout(this)
        scoreGraphContainer.orientation = LinearLayout.VERTICAL
        
        val scoreGraphTitle = TextView(this)
        scoreGraphTitle.text = "📊 모의고사 성적 그래프 (클릭해서 데이터 추가)"
        scoreGraphTitle.setTextColor(Color.BLACK)
        scoreGraphTitle.textSize = 14f
        scoreGraphTitle.setTypeface(null, android.graphics.Typeface.BOLD)
        
        // 성적 LineChart 생성
        val scoreChart = LineChart(this)
        setupScoreChart(scoreChart, tagId)
        
        val scoreChartParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (200 * dpToPx).toInt() // 높이를 200dp로 설정
        )
        scoreChartParams.topMargin = (8 * dpToPx).toInt()
        scoreChart.layoutParams = scoreChartParams
        
        scoreGraphContainer.addView(scoreGraphTitle)
        scoreGraphContainer.addView(scoreChart)
        
        val scoreContainerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        scoreContainerParams.bottomMargin = (16 * dpToPx).toInt()
        scoreGraphContainer.layoutParams = scoreContainerParams

        // 8. 시간 그래프 섹션 (실제 LineChart)
        val timeGraphContainer = LinearLayout(this)
        timeGraphContainer.orientation = LinearLayout.VERTICAL
        
        val timeGraphTitle = TextView(this)
        timeGraphTitle.text = "⏰ 모의고사 소요 시간 그래프 (클릭해서 데이터 추가)"
        timeGraphTitle.setTextColor(Color.BLACK)
        timeGraphTitle.textSize = 14f
        timeGraphTitle.setTypeface(null, android.graphics.Typeface.BOLD)
        
        // 시간 LineChart 생성
        val timeChart = LineChart(this)
        setupTimeChart(timeChart, tagId)
        
        val timeChartParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (200 * dpToPx).toInt() // 높이를 200dp로 설정
        )
        timeChartParams.topMargin = (8 * dpToPx).toInt()
        timeChart.layoutParams = timeChartParams
        
        timeGraphContainer.addView(timeGraphTitle)
        timeGraphContainer.addView(timeChart)
        
        val timeContainerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        timeContainerParams.bottomMargin = (16 * dpToPx).toInt()
        timeGraphContainer.layoutParams = timeContainerParams

        // 추가 정보 컨테이너에 모든 하위 뷰들 추가 (수정하기 버튼 제거)
        additionalInfoContainer.addView(taskListTitle)
        additionalInfoContainer.addView(taskListContainer)
        additionalInfoContainer.addView(dateSettingContainer)
        additionalInfoContainer.addView(scoreGraphContainer)
        additionalInfoContainer.addView(timeGraphContainer)

        // 토글 기능 구현
        var isExpanded = false
        toggleButton.setOnClickListener {
            isExpanded = !isExpanded
            if (isExpanded) {
                additionalInfoContainer.visibility = android.view.View.VISIBLE
                toggleButton.setImageResource(android.R.drawable.arrow_up_float)
            } else {
                additionalInfoContainer.visibility = android.view.View.GONE
                toggleButton.setImageResource(android.R.drawable.arrow_down_float)
            }
        }

        // 메인 컨테이너에 기본 정보와 추가 정보 컨테이너 추가
        tagContainer.addView(basicInfoContainer)
        tagContainer.addView(additionalInfoContainer)

        return tagContainer
    }

    /**
     * 성적 그래프 차트를 설정하는 함수
     * 
     * @param chart 설정할 LineChart
     * @param tagId 태그 ID
     */
    private fun setupScoreChart(chart: LineChart, tagId: Long) {
        chart.apply {
            setDragEnabled(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            description.text = ""
            setNoDataText("차트를 클릭해서 성적을 추가하세요")
            setNoDataTextColor(Color.BLACK)
            setTouchEnabled(true)

            // X축 설정
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.BLACK
                setDrawGridLines(true)
                granularity = 1f
            }

            // Y축 설정
            axisLeft.apply {
                textColor = Color.BLACK
                axisMinimum = 0f
                axisMaximum = 100f
                setDrawGridLines(true)
            }

            axisRight.isEnabled = false
            legend.textColor = Color.BLACK

            setOnChartValueSelectedListener(this@TagActivity)
            onChartGestureListener = object : OnChartGestureListener {
                override fun onChartGestureStart(me: MotionEvent?, lastPerformedGesture: com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture?) {}
                override fun onChartGestureEnd(me: MotionEvent?, lastPerformedGesture: com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture?) {}
                override fun onChartLongPressed(me: MotionEvent?) {}
                override fun onChartDoubleTapped(me: MotionEvent?) {}
                override fun onChartSingleTapped(me: MotionEvent?) {
                    showScoreInputDialog(tagId)
                }
                override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) {}
                override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {}
                override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {}
            }
        }
        
        // 데이터 로드
        loadScoreData(chart, tagId)
    }
    
    /**
     * 시간 그래프 차트를 설정하는 함수
     * 
     * @param chart 설정할 LineChart
     * @param tagId 태그 ID
     */
    private fun setupTimeChart(chart: LineChart, tagId: Long) {
        chart.apply {
            setDragEnabled(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            description.text = ""
            setNoDataText("차트를 클릭해서 소요 시간을 추가하세요")
            setNoDataTextColor(Color.BLACK)
            setTouchEnabled(true)

            // X축 설정
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.BLACK
                setDrawGridLines(true)
                granularity = 1f
            }

            // Y축 설정
            axisLeft.apply {
                textColor = Color.BLACK
                axisMinimum = 0f
                axisMaximum = 300f // 300분 = 5시간으로 설정
                setDrawGridLines(true)
            }

            axisRight.isEnabled = false
            legend.textColor = Color.BLACK

            setOnChartValueSelectedListener(this@TagActivity)
            onChartGestureListener = object : OnChartGestureListener {
                override fun onChartGestureStart(me: MotionEvent?, lastPerformedGesture: com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture?) {}
                override fun onChartGestureEnd(me: MotionEvent?, lastPerformedGesture: com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture?) {}
                override fun onChartLongPressed(me: MotionEvent?) {}
                override fun onChartDoubleTapped(me: MotionEvent?) {}
                override fun onChartSingleTapped(me: MotionEvent?) {
                    showTimeInputDialog(tagId)
                }
                override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) {}
                override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {}
                override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {}
            }
        }
        
        // 데이터 로드
        loadTimeData(chart, tagId)
    }
    
    /**
     * 성적 데이터를 로드하여 차트에 표시하는 함수
     */
    private fun loadScoreData(chart: LineChart, tagId: Long) {
        val scoreData = dbHelper.getScoreData(tagId)
        val entries = mutableListOf<Entry>()
        
        scoreData.forEachIndexed { index, (_, score) ->
            entries.add(Entry(index.toFloat(), score))
        }
        
        scoreEntriesMap[tagId] = entries
        scoreIndexMap[tagId] = scoreData.size.toFloat()
        
        updateScoreChart(chart, entries)
    }
    
    /**
     * 시간 데이터를 로드하여 차트에 표시하는 함수
     */
    private fun loadTimeData(chart: LineChart, tagId: Long) {
        val timeData = dbHelper.getTimeData(tagId)
        val entries = mutableListOf<Entry>()
        
        timeData.forEachIndexed { index, (_, time) ->
            entries.add(Entry(index.toFloat(), time))
        }
        
        timeEntriesMap[tagId] = entries
        timeIndexMap[tagId] = timeData.size.toFloat()
        
        updateTimeChart(chart, entries)
    }
    
    /**
     * 성적 차트 업데이트
     */
    private fun updateScoreChart(chart: LineChart, entries: List<Entry>) {
        if (entries.isEmpty()) {
            chart.clear()
            return
        }
        
        val dataSet = LineDataSet(entries, "성적").apply {
            color = Color.parseColor("#FF9800")
            setCircleColor(Color.parseColor("#FF9800"))
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(true)
            valueTextColor = Color.BLACK
            valueTextSize = 10f
        }
        
        val lineData = LineData(dataSet)
        chart.data = lineData
        chart.invalidate()
    }
    
    /**
     * 시간 차트 업데이트
     */
    private fun updateTimeChart(chart: LineChart, entries: List<Entry>) {
        if (entries.isEmpty()) {
            chart.clear()
            return
        }
        
        val dataSet = LineDataSet(entries, "소요시간(분)").apply {
            color = Color.parseColor("#4CAF50")
            setCircleColor(Color.parseColor("#4CAF50"))
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(true)
            valueTextColor = Color.BLACK
            valueTextSize = 10f
        }
        
        val lineData = LineData(dataSet)
        chart.data = lineData
        chart.invalidate()
    }
    
    /**
     * 성적 입력 다이얼로그 표시
     */
    private fun showScoreInputDialog(tagId: Long) {
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
                    if (score in 0f..100f) {
                        addScoreData(tagId, score)
                    } else {
                        Toast.makeText(this, "0-100 사이의 점수를 입력하세요", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "올바른 숫자를 입력하세요", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("취소") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }
    
    /**
     * 시간 입력 다이얼로그 표시 (분 단위)
     */
    private fun showTimeInputDialog(tagId: Long) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("모의고사 소요 시간 입력")
        
        val input = EditText(this)
        input.hint = "소요 시간을 입력하세요 (분 단위)"
        builder.setView(input)
        
        builder.setPositiveButton("추가") { _, _ ->
            val timeText = input.text.toString()
            if (timeText.isNotEmpty()) {
                try {
                    val time = timeText.toFloat()
                    if (time >= 0f && time <= 300f) {
                        addTimeData(tagId, time)
                    } else {
                        Toast.makeText(this, "0-300 사이의 분을 입력하세요", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "올바른 숫자를 입력하세요", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("취소") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }
    
    /**
     * 성적 데이터 추가
     */
    private fun addScoreData(tagId: Long, score: Float) {
        val result = dbHelper.addScoreData(tagId, score)
        if (result != -1L) {
            val currentIndex = scoreIndexMap[tagId] ?: 0f
            val entries = scoreEntriesMap.getOrPut(tagId) { mutableListOf() }
            entries.add(Entry(currentIndex, score))
            scoreIndexMap[tagId] = currentIndex + 1f
            
            // 해당 태그의 차트 찾아서 업데이트
            findAndUpdateScoreChart(tagId, entries)
            
            Toast.makeText(this, "성적이 추가되었습니다", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "데이터 저장 실패", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 시간 데이터 추가
     */
    private fun addTimeData(tagId: Long, time: Float) {
        val result = dbHelper.addTimeData(tagId, time)
        if (result != -1L) {
            val currentIndex = timeIndexMap[tagId] ?: 0f
            val entries = timeEntriesMap.getOrPut(tagId) { mutableListOf() }
            entries.add(Entry(currentIndex, time))
            timeIndexMap[tagId] = currentIndex + 1f
            
            // 해당 태그의 차트 찾아서 업데이트
            findAndUpdateTimeChart(tagId, entries)
            
            Toast.makeText(this, "소요 시간이 추가되었습니다", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "데이터 저장 실패", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 특정 태그의 성적 차트를 찾아서 업데이트
     */
    private fun findAndUpdateScoreChart(tagId: Long, entries: List<Entry>) {
        for (i in 0 until scrollContainer.childCount) {
            val tagContainer = scrollContainer.getChildAt(i) as LinearLayout
            if (tagContainer.tag == tagId) {
                val additionalInfoContainer = tagContainer.getChildAt(1) as LinearLayout
                val scoreGraphContainer = additionalInfoContainer.getChildAt(3) as LinearLayout
                val scoreChart = scoreGraphContainer.getChildAt(1) as LineChart
                updateScoreChart(scoreChart, entries)
                break
            }
        }
    }
    
    /**
     * 특정 태그의 시간 차트를 찾아서 업데이트
     */
    private fun findAndUpdateTimeChart(tagId: Long, entries: List<Entry>) {
        for (i in 0 until scrollContainer.childCount) {
            val tagContainer = scrollContainer.getChildAt(i) as LinearLayout
            if (tagContainer.tag == tagId) {
                val additionalInfoContainer = tagContainer.getChildAt(1) as LinearLayout
                val timeGraphContainer = additionalInfoContainer.getChildAt(4) as LinearLayout
                val timeChart = timeGraphContainer.getChildAt(1) as LineChart
                updateTimeChart(timeChart, entries)
                break
            }
        }
    }
    
    // OnChartValueSelectedListener 인터페이스 구현
    override fun onValueSelected(e: Entry?, h: Highlight?) {
        e?.let {
            Toast.makeText(this, "선택된 값: ${it.y}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onNothingSelected() {
        // 선택이 해제되었을 때의 동작
    }

    /**
     * 액티비티가 다시 활성화될 때 호출되는 함수
     * 
     * 다른 화면 (예: TagInfoActivity, MainActivity)에서 태스크 상태가 변경된 후
     * 이 화면으로 돌아올 때 최신 정보를 반영하기 위해 사용
     * 
     * 실행 순서:
     * 1. loadExistingTags(): 데이터베이스에서 최신 태그 목록 로드
     * 2. updateCompletionRates(): 각 태그의 완수율을 최신 상태로 업데이트
     * 3. updateDdays(): 각 태그의 D-day를 최신 상태로 업데이트
     * 4. updateSummaryInfo(): 각 태그의 성적/시간 요약 정보 업데이트
     */
    override fun onResume() {
        super.onResume()
        loadExistingTags()        // 최신 태그 목록 로드
        updateCompletionRates()   // 완수율 실시간 업데이트
        updateDdays()             // D-day 실시간 업데이트
        updateTaskLists()         // 태스크 목록 업데이트
        refreshAllCharts()        // 모든 차트 데이터 새로고침
    }
    
    /**
     * 모든 태그의 차트 데이터를 새로고침하는 함수
     */
    private fun refreshAllCharts() {
        for (i in 0 until scrollContainer.childCount) {
            val tagContainer = scrollContainer.getChildAt(i) as LinearLayout
            val tagId = tagContainer.tag as Long
            
            try {
                val additionalInfoContainer = tagContainer.getChildAt(1) as LinearLayout
                
                // 성적 차트 새로고침
                val scoreGraphContainer = additionalInfoContainer.getChildAt(3) as LinearLayout
                val scoreChart = scoreGraphContainer.getChildAt(1) as LineChart
                loadScoreData(scoreChart, tagId)
                
                // 시간 차트 새로고침  
                val timeGraphContainer = additionalInfoContainer.getChildAt(4) as LinearLayout
                val timeChart = timeGraphContainer.getChildAt(1) as LineChart
                loadTimeData(timeChart, tagId)
            } catch (e: Exception) {
                // 차트 새로고침 실패 시 무시 (토글이 접혀있을 수 있음)
            }
        }
    }
    // 기존 태그들을 데이터베이스에서 로드하는 메서드 추가
    private fun loadExistingTags() {
        try {
            scrollContainer.removeAllViews() // 기존 뷰들 제거

            val existingTags = dbHelper.getAllTags(userId)
            for ((tagId, tagName) in existingTags) {
                try {
                    val tagView = createTagView(tagName, tagId, isEditMode = true)
                    scrollContainer.addView(tagView)
                } catch (e: Exception) {
                    // 개별 태그 생성 실패 시 건너뛰기
                    continue
                }
            }

            tagCounter = existingTags.size
        } catch (e: Exception) {
            Toast.makeText(this, "태그 목록 로딩 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 화면에 표시된 모든 태그의 완수율을 실시간으로 업데이트하는 함수 (토글 UI 대응)
     * 
     * 새로운 UI 구조:
     * TagContainer (LinearLayout)
     * ├── BasicInfoContainer (LinearLayout) - index 0
     * │   ├── NameToggleContainer (LinearLayout) - index 0
     * │   ├── TextView (D-day) - index 1
     * │   └── TextView (완수율) - index 2  ← 여기를 업데이트
     * └── AdditionalInfoContainer (LinearLayout) - index 1
     */
    private fun updateCompletionRates() {
        // scrollContainer 내의 모든 태그 뷰를 순회하며 완수율 업데이트
        for (i in 0 until scrollContainer.childCount) {
            val tagContainer = scrollContainer.getChildAt(i) as LinearLayout
            val tagId = tagContainer.tag as Long // 태그 ID는 View의 tag 속성에 저장됨
            
            // 기본 정보 컨테이너 (index 0)
            val basicInfoContainer = tagContainer.getChildAt(0) as LinearLayout
            
            // 기본 정보 컨테이너의 세 번째 자식이 완수율을 표시하는 TextView (index 2)
            val finishRateText = basicInfoContainer.getChildAt(2) as TextView
            
            // 데이터베이스에서 해당 태그의 완수율을 계산하여 가져옴
            val completionRate = dbHelper.getTagCompletionRate(tagId)
            
            // "완수율: XX%" 형태로 텍스트 업데이트
            finishRateText.text = "완수율: ${completionRate}%"
        }
    }

    /**
     * 화면에 표시된 모든 태그의 D-day를 실시간으로 업데이트하는 함수 (토글 UI 대응)
     * 
     * 새로운 UI 구조:
     * TagContainer (LinearLayout)
     * ├── BasicInfoContainer (LinearLayout) - index 0
     * │   ├── NameToggleContainer (LinearLayout) - index 0
     * │   ├── TextView (D-day) - index 1  ← 여기를 업데이트
     * │   └── TextView (완수율) - index 2
     * └── AdditionalInfoContainer (LinearLayout) - index 1
     */
    private fun updateDdays() {
        // scrollContainer 내의 모든 태그 뷰를 순회하며 D-day 업데이트
        for (i in 0 until scrollContainer.childCount) {
            val tagContainer = scrollContainer.getChildAt(i) as LinearLayout
            val tagId = tagContainer.tag as Long // 태그 ID는 View의 tag 속성에 저장됨
            
            // 기본 정보 컨테이너 (index 0)
            val basicInfoContainer = tagContainer.getChildAt(0) as LinearLayout
            
            // 기본 정보 컨테이너의 두 번째 자식이 D-day를 표시하는 TextView (index 1)
            val ddayText = basicInfoContainer.getChildAt(1) as TextView
            
            // 데이터베이스에서 해당 태그의 시험 날짜 조회
            val examDate = dbHelper.getExamDate(tagId)
            
            if (examDate != null) {
                try {
                    // 시험 날짜 파싱 및 D-day 계산 (Calendar 사용)
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val examDateParsed = dateFormat.parse(examDate)
                    val today = Calendar.getInstance().time
                    
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
                        
                        ddayText.text = displayText
                    } else {
                        ddayText.text = "시험일: $examDate"
                    }
                } catch (e: Exception) {
                    // 날짜 파싱 실패 시 원본 날짜 표시
                    ddayText.text = "시험일: $examDate"
                }
            } else {
                // 시험 날짜가 설정되지 않은 경우
                ddayText.text = "D-day: 설정되지 않음"
            }
        }
    }



    /**
     * 화면에 표시된 모든 태그의 태스크 목록을 업데이트하는 함수 (토글 UI 대응)
     * 
     * 새로운 UI 구조:
     * TagContainer (LinearLayout)
     * ├── BasicInfoContainer (LinearLayout) - index 0
     * └── AdditionalInfoContainer (LinearLayout) - index 1
     *     ├── TextView (태스크 목록 제목) - index 0
     *     ├── LinearLayout (태스크 목록 컨테이너) - index 1  ← 여기를 업데이트
     *     ├── LinearLayout (날짜 설정 컨테이너) - index 2
     *     ├── LinearLayout (성적 그래프 컨테이너) - index 3
     *     ├── LinearLayout (시간 그래프 컨테이너) - index 4
     *     └── Button (수정하기 버튼) - index 5
     */
    private fun updateTaskLists() {
        val dpToPx = resources.displayMetrics.density
        
        for (i in 0 until scrollContainer.childCount) {
            val tagContainer = scrollContainer.getChildAt(i) as LinearLayout
            val tagId = tagContainer.tag as Long
            
            // 추가 정보 컨테이너 (index 1)
            val additionalInfoContainer = tagContainer.getChildAt(1) as LinearLayout
            
            // 태스크 목록 컨테이너 (index 1)
            val taskListContainer = additionalInfoContainer.getChildAt(1) as LinearLayout
            taskListContainer.removeAllViews() // 기존 태스크 뷰들 제거
            
            // 데이터베이스에서 태스크 목록 조회
            val tasks = dbHelper.getTasksForTag(tagId)
            
            if (tasks.isEmpty()) {
                val noTaskText = TextView(this)
                noTaskText.text = "태스크 없음"
                noTaskText.setTextColor(Color.GRAY)
                noTaskText.textSize = 12f
                taskListContainer.addView(noTaskText)
            } else {
                // 최대 3개까지만 표시
                val tasksToShow = tasks.take(3)
                
                tasksToShow.forEach { task ->
                    val taskItemContainer = LinearLayout(this)
                    taskItemContainer.orientation = LinearLayout.HORIZONTAL
                    taskItemContainer.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    
                    // 체크박스
                    val checkBox = android.widget.CheckBox(this)
                    checkBox.isChecked = task.isCompleted
                    checkBox.layoutParams = LinearLayout.LayoutParams(
                        (48 * dpToPx).toInt(), // 최소 터치 타겟 크기 (48dp)
                        (48 * dpToPx).toInt()  // 최소 터치 타겟 크기 (48dp)
                    )
                    checkBox.setPadding(
                        (8 * dpToPx).toInt(),
                        (8 * dpToPx).toInt(),
                        (8 * dpToPx).toInt(),
                        (8 * dpToPx).toInt()
                    )
                    
                    // 체크박스 클릭 리스너 추가 - 데이터베이스 업데이트
                    checkBox.setOnCheckedChangeListener { _, isChecked ->
                        try {
                            // 데이터베이스에서 태스크 완료 상태 업데이트
                            dbHelper.updateTaskCompletion(task.id, isChecked)
                            
                            // 완수율 실시간 업데이트
                            updateCompletionRates()
                            
                            // 성공 메시지 표시
                            val statusText = if (isChecked) "완료" else "미완료"
                            Toast.makeText(this@TagActivity, 
                                "태스크 '${task.title}' $statusText", 
                                Toast.LENGTH_SHORT).show()
                                
                        } catch (e: Exception) {
                            // 오류 발생 시 체크박스 상태 되돌리기
                            checkBox.isChecked = !isChecked
                            Toast.makeText(this@TagActivity, 
                                "태스크 상태 업데이트 실패: ${e.message}", 
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                    
                    // 태스크 텍스트
                    val taskText = TextView(this)
                    val scheduledDateText = task.scheduledDate ?: "미설정"
                    taskText.text = "${task.title} ($scheduledDateText)"
                    taskText.setTextColor(Color.BLACK)
                    taskText.textSize = 12f
                    taskText.layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                    
                    taskItemContainer.addView(checkBox)
                    taskItemContainer.addView(taskText)
                    taskListContainer.addView(taskItemContainer)
                }
                
                // 3개보다 많은 경우 "..." 표시
                if (tasks.size > 3) {
                    val moreText = TextView(this)
                    moreText.text = "...외 ${tasks.size - 3}개"
                    moreText.setTextColor(Color.GRAY)
                    moreText.textSize = 11f
                    taskListContainer.addView(moreText)
                }
            }
        }
    }

    /**
     * 화면에 표시된 모든 태그의 성적/시간 그래프를 업데이트하는 함수 (제거)
     * 그래프 기능을 제거하여 안정성 확보
     */
    // updateGraphs() 함수 제거

    /**
     * 날짜 선택 다이얼로그를 표시하는 함수
     * 
     * @param tagId 태그 ID
     * 
     * 동작 과정:
     * 1. DatePickerDialog를 생성하여 현재 날짜로 초기화
     * 2. 사용자가 날짜를 선택하면 데이터베이스에 저장
     * 3. UI의 D-day 정보를 즉시 업데이트
     */
    private fun showDatePickerDialog(tagId: Long) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                // 선택된 날짜를 문자열로 변환 (yyyy-MM-dd 형식)
                val selectedDate = String.format(
                    Locale.getDefault(),
                    "%04d-%02d-%02d",
                    selectedYear,
                    selectedMonth + 1, // Calendar.MONTH는 0부터 시작
                    selectedDay
                )
                
                // 데이터베이스에 시험 날짜 저장
                try {
                    dbHelper.updateExamDate(tagId, selectedDate)
                    Toast.makeText(this, "시험일이 설정되었습니다: $selectedDate", Toast.LENGTH_SHORT).show()
                    
                    // D-day 정보 즉시 업데이트
                    updateDdays()
                } catch (e: Exception) {
                    Toast.makeText(this, "날짜 설정 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            year,
            month,
            day
        )
        
        datePickerDialog.show()
    }
}