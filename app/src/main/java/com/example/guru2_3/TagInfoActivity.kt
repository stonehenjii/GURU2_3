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


class TagInfoActivity : AppCompatActivity(), OnChartValueSelectedListener {

    private lateinit var scoreChart: LineChart
    private lateinit var timeChart: LineChart
    //private lateinit var createTagNameEditText: EditText
    private lateinit var createTagFinishRate: TextView
    private lateinit var createTagDateText: TextView
    private lateinit var tagMoveicon: ImageView
    private lateinit var tagMoveText: TextView
    private lateinit var scoreInput: EditText
    private lateinit var createTagNameTextView: TextView
    private var tagName: String = ""
    private lateinit var createTagDateSwitch: Switch
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
        setContentView(R.layout.activity_tag_info)

        tagName = intent.getStringExtra("TAG_NAME") ?: "기본태그"
        currentTagId = intent.getLongExtra("TAG_ID", -1)

        initViews()
        setupTagName()
        setupCharts()
        setClickListeners()
        dbHelper = DatabaseHelper(this)
        loadDataFromDatabase()
    }

    private fun initViews() {
        scoreChart = findViewById(R.id.scoreChart)
        timeChart = findViewById(R.id.timeChart)
        createTagNameTextView = findViewById(R.id.createTagNameTextView)
        createTagFinishRate = findViewById(R.id.createTagFinishRate)
        createTagDateText = findViewById(R.id.createTagDateText)
        scoreInput = findViewById(R.id.scoreInput)
        tagMoveicon = findViewById(R.id.tagMoveicon)
        tagMoveText = findViewById(R.id.tagMoveText)
        createTagDateSwitch = findViewById(R.id.createTagDateSwitch)
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

    private fun loadDataFromDatabase() {
        val scoreData = dbHelper.getScoreData(currentTagId) // ← TAG별 불러오기
        scoreEntries.clear()
        scoreData.forEach { (index, score) ->
            scoreEntries.add(Entry(index, score))
        }
        currentScoreIndex = scoreData.size.toFloat()
        updateScoreChart()

        val timeData = dbHelper.getTimeData(currentTagId) // ← TAG별 불러오기
        timeEntries.clear()
        timeData.forEach { (index, time) ->
            timeEntries.add(Entry(index, time))
        }
        currentTimeIndex = timeData.size.toFloat()
        updateTimeChart()
        loadExamDateFromDatabase()
        setupDateSwitch()
    }



    private fun setupDateSwitch() {
        createTagDateSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                createTagDateText.visibility = View.VISIBLE
                if (examDate == null) {
                    showDatePicker()
                }
            } else {
                createTagDateText.visibility = View.GONE
                examDate = null
                dbHelper.updateExamDate(currentTagId, null)
                createTagDateText.text = "날짜를 선택하세요"
            }
        }

        createTagDateText.setOnClickListener {
            if (createTagDateSwitch.isChecked) {
                showDatePicker()
            }
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
                createTagDateText.text = examDate
                dbHelper.updateExamDate(currentTagId, examDate)
            },
            year, month, day
        )

        datePickerDialog.show()
    }

    private fun loadExamDateFromDatabase() {
        examDate = dbHelper.getExamDate(currentTagId)
        if (examDate != null) {
            createTagDateSwitch.isChecked = true
            createTagDateText.visibility = View.VISIBLE
            createTagDateText.text = examDate
        }
    }



}