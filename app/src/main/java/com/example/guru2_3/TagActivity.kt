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

class TagActivity : AppCompatActivity() {
    private lateinit var scrollContainer: LinearLayout
    private lateinit var tagaddicon: ImageView
    private lateinit var tagaddText: TextView
    private lateinit var dbHelper: DatabaseHelper
    private var tagCounter = 0 // 태그 고유 ID 생성용
    private var userId: Long = 0


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
     * 새로운 태그 뷰를 동적으로 생성하는 함수 (사용자 흐름 개선 버전)
     * 
     * @param tagName 생성할 태그의 이름
     * @param tagId 태그의 고유 ID (데이터베이스에서 자동 생성)
     * @param isEditMode 편집 모드 여부 (현재 미사용)
     * @return 생성된 태그 컨테이너 (LinearLayout)
     * 
     * UI 개선 사항:
     * - 기존: 완수율 → D-day → 시험일정 스위치 순서
     * - 개선: D-day → 완수율 → 성적/시간 요약 → 수정하기 버튼 순서
     * - 스위치 제거하고 명확한 "수정하기" 버튼으로 TagInfoActivity 진입
     * 
     * 생성되는 UI 구조:
     * LinearLayout (태그 컨테이너)
     * ├── EditText (태그 이름 입력/표시)
     * ├── TextView (D-day 표시) - 첫 번째 줄
     * ├── TextView (완수율 표시) - 두 번째 줄  
     * ├── TextView (성적/시간 요약) - 세 번째 줄
     * └── Button (수정하기 버튼) - TagInfoActivity로 이동
     * 
     * 사용자 경험 개선:
     * - 미리보기 정보를 더 많이 제공 (성적 최고점수, 총 공부시간)
     * - 더 직관적인 "수정하기" 버튼으로 상세 화면 진입
     * - 정보 표시 순서를 중요도에 따라 재배치
     */
    private fun createTagView(tagName: String, tagId: Long, isEditMode: Boolean): LinearLayout {
        val dpToPx = resources.displayMetrics.density

        // 태그 컨테이너 생성
        val tagContainer = LinearLayout(this)
        tagContainer.orientation = LinearLayout.VERTICAL
        tagContainer.setBackgroundColor("#F44336".toColorInt())
        tagContainer.tag = tagId // 태그 ID를 View의 tag로 저장

        // 레이아웃 파라미터 설정 (높이 증가 - 태스크 목록과 그래프 미리보기 공간)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (350 * dpToPx).toInt() // 높이를 늘려서 태스크 목록과 그래프 미리보기 공간 확보
        )

        layoutParams.topMargin = (20 * dpToPx).toInt()

        tagContainer.layoutParams = layoutParams
        tagContainer.id = android.view.View.generateViewId()

        // 1. EditText 추가 (태그 이름)
        val nameEditText = EditText(this)
        nameEditText.setText(tagName) // hint 대신 text로 설정
        nameEditText.setTypeface(null, android.graphics.Typeface.BOLD)
        nameEditText.textSize = 18f
        nameEditText.setTextColor(Color.BLACK)
        nameEditText.setBackgroundColor(Color.WHITE)
        nameEditText.setPadding(
            (16 * dpToPx).toInt(),
            (12 * dpToPx).toInt(),
            (16 * dpToPx).toInt(),
            (12 * dpToPx).toInt()
        )
        
        val nameEditParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (48 * dpToPx).toInt() // 최소 터치 타겟 크기 (48dp)
        )
        nameEditParams.topMargin = (8 * dpToPx).toInt()
        nameEditParams.bottomMargin = (8 * dpToPx).toInt()
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

        // 2. TextView 추가 (D-day 표시) - 첫 번째 줄
        val ddayText = TextView(this)
        ddayText.text = "D-day: 설정되지 않음"
        ddayText.setTextColor(Color.BLACK)
        ddayText.textSize = 18f
        val ddayParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        ddayParams.topMargin = (10 * dpToPx).toInt()
        ddayText.layoutParams = ddayParams

        // 3. TextView 추가 (완수율) - 두 번째 줄
        val finishRateText = TextView(this)
        finishRateText.text = "완수율: 0%"
        finishRateText.setTextColor(Color.BLACK)
        finishRateText.textSize = 18f
        val finishRateParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        finishRateParams.topMargin = (5 * dpToPx).toInt()
        finishRateText.layoutParams = finishRateParams

        // 4. 태스크 목록 섹션
        val taskListTitle = TextView(this)
        taskListTitle.text = "태스크 목록"
        taskListTitle.setTextColor(Color.BLACK)
        taskListTitle.textSize = 16f
        taskListTitle.setTypeface(null, android.graphics.Typeface.BOLD)
        val taskTitleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        taskTitleParams.topMargin = (10 * dpToPx).toInt()
        taskListTitle.layoutParams = taskTitleParams

        // 태스크 목록을 표시할 LinearLayout (세로 배치)
        val taskListContainer = LinearLayout(this)
        taskListContainer.orientation = LinearLayout.VERTICAL
        val taskContainerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        taskContainerParams.topMargin = (5 * dpToPx).toInt()
        taskListContainer.layoutParams = taskContainerParams

        // 5. 성적/시간 그래프 미리보기 (제거)
        // 그래프 기능을 제거하여 안정성 확보

        // 성적과 시간 데이터의 평균값을 계산하여 표시
        val scoreData = dbHelper.getScoreData(tagId)
        val averageScore = if (scoreData.isNotEmpty()) {
            val totalScore = scoreData.sumOf { it.second.toDouble() }
            (totalScore / scoreData.size).toInt()
        } else 0
        
        val timeData = dbHelper.getTimeData(tagId)
        val averageTime = if (timeData.isNotEmpty()) {
            val totalTime = timeData.sumOf { it.second.toDouble() }
            (totalTime / timeData.size).toInt()
        } else 0
        
        val summaryText = TextView(this)
        val scoreText = if (averageScore > 0) "평균 ${averageScore}점" else "데이터 없음"
        val timeText = if (averageTime > 0) "평균 ${averageTime}시간" else "데이터 없음"
        summaryText.text = "성적: $scoreText | 시간: $timeText"
        summaryText.setTextColor(Color.GRAY)
        summaryText.textSize = 14f
        val summaryParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        summaryParams.topMargin = (5 * dpToPx).toInt()
        summaryText.layoutParams = summaryParams

        // 6. Button 추가 (수정하기 버튼)
        val editButton = android.widget.Button(this)
        editButton.text = "수정하기"
        editButton.textSize = 16f
        editButton.setBackgroundColor(Color.parseColor("#FF9800"))
        editButton.setTextColor(Color.WHITE)
        editButton.setPadding(
            (16 * dpToPx).toInt(),
            (12 * dpToPx).toInt(),
            (16 * dpToPx).toInt(),
            (12 * dpToPx).toInt()
        )
        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (48 * dpToPx).toInt() // 최소 터치 타겟 크기 (48dp)
        )
        buttonParams.topMargin = (10 * dpToPx).toInt()
        editButton.layoutParams = buttonParams

        // 수정하기 버튼 클릭 리스너 설정
        editButton.setOnClickListener {
            // 버튼 텍스트를 확실히 "수정하기"로 유지
            editButton.text = "수정하기"
            val currentTagName = nameEditText.text.toString()
            navigateToTagInfo(currentTagName, tagId)
        }

        // 그래프 입력 기능 제거 (안정성 확보)

        // 모든 뷰를 컨테이너에 추가
        tagContainer.addView(nameEditText)
        tagContainer.addView(ddayText)
        tagContainer.addView(finishRateText)
        tagContainer.addView(taskListTitle)
        tagContainer.addView(taskListContainer)
        tagContainer.addView(summaryText)
        tagContainer.addView(editButton)

        return tagContainer
    }

    /**
     * TagInfoActivity로 이동하는 함수 (수정하기 버튼 클릭 시)
     * 
     * @param tagName 태그 이름
     * @param tagId 태그 ID
     * 
     * 동작 과정:
     * 1. Intent 생성하여 TagInfoActivity 지정
     * 2. 태그 이름과 ID를 Extra로 전달
     * 3. 액티비티 시작
     * 
     * 오류 방지:
     * - 유효한 tagId인지 확인
     * - 디버깅을 위한 Toast 메시지 추가
     */
    private fun navigateToTagInfo(tagName: String, tagId: Long) {
        // 유효성 검사
        if (tagId <= 0) {
            Toast.makeText(this, "유효하지 않은 태그입니다.", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 디버깅 메시지
        Toast.makeText(this, "태그 '$tagName' 정보 수정 화면으로 이동", Toast.LENGTH_SHORT).show()
        
        val intent = Intent(this, TagInfoActivity::class.java)
        intent.putExtra("TAG_NAME", tagName)
        intent.putExtra("TAG_ID", tagId)
        
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "페이지 이동 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_LONG).show()
        }
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
        updateSummaryInfo()       // 성적/시간 요약 업데이트
        // updateGraphs() 제거 (그래프 기능 제거)
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
     * 화면에 표시된 모든 태그의 완수율을 실시간으로 업데이트하는 함수
     * 
     * 동작 과정:
     * 1. scrollContainer 내의 모든 태그 뷰를 순회
     * 2. 각 태그 컨테이너에서 태그 ID를 추출 (View.tag 속성에 저장됨)
     * 3. 해당 태그 ID로 데이터베이스에서 완수율 계산
     * 4. 완수율 TextView에 "완수율: XX%" 형태로 표시
     * 
     * 호출 시점:
     * - onResume(): 다른 화면에서 돌아올 때
     * - 태스크 상태가 변경된 후
     * 
     * UI 구조 참고:
     * TagContainer (LinearLayout)
     * ├── EditText (태그 이름) - index 0
     * ├── TextView (D-day) - index 1
     * ├── TextView (완수율) - index 2  ← 여기를 업데이트
     * ├── TextView (태스크 목록 제목) - index 3
     * ├── LinearLayout (태스크 목록 컨테이너) - index 4
     * ├── TextView (그래프 제목) - index 5
     * ├── TextView (그래프 요약) - index 6
     * └── Button (수정하기) - index 7
     */
    private fun updateCompletionRates() {
        // scrollContainer 내의 모든 태그 뷰를 순회하며 완수율 업데이트
        for (i in 0 until scrollContainer.childCount) {
            val tagContainer = scrollContainer.getChildAt(i) as LinearLayout
            val tagId = tagContainer.tag as Long // 태그 ID는 View의 tag 속성에 저장됨
            
            // 태그 컨테이너의 세 번째 자식이 완수율을 표시하는 TextView
            val finishRateText = tagContainer.getChildAt(2) as TextView
            
            // 데이터베이스에서 해당 태그의 완수율을 계산하여 가져옴
            val completionRate = dbHelper.getTagCompletionRate(tagId)
            
            // "완수율: XX%" 형태로 텍스트 업데이트
            finishRateText.text = "완수율: ${completionRate}%"
        }
    }

    /**
     * 화면에 표시된 모든 태그의 D-day를 실시간으로 업데이트하는 함수
     * 
     * 동작 과정:
     * 1. scrollContainer 내의 모든 태그 뷰를 순회
     * 2. 각 태그 컨테이너에서 태그 ID를 추출
     * 3. 해당 태그 ID로 데이터베이스에서 시험 날짜 조회
     * 4. D-day 계산 후 TextView에 표시
     * 
     * D-day 표시 형식:
     * - 시험일 전: "D-5 (2024-01-15)"
     * - 시험일 당일: "D-Day! (2024-01-10)"
     * - 시험일 후: "D+3 (2024-01-07)"
     * - 날짜 미설정: "D-day: 설정되지 않음"
     */
    private fun updateDdays() {
        // scrollContainer 내의 모든 태그 뷰를 순회하며 D-day 업데이트
        for (i in 0 until scrollContainer.childCount) {
            val tagContainer = scrollContainer.getChildAt(i) as LinearLayout
            val tagId = tagContainer.tag as Long // 태그 ID는 View의 tag 속성에 저장됨
            
            // 태그 컨테이너의 두 번째 자식이 D-day를 표시하는 TextView
            val ddayText = tagContainer.getChildAt(1) as TextView
            
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
     * 화면에 표시된 모든 태그의 성적/시간 요약 정보를 업데이트하는 함수
     * 
     * 동작 과정:
     * 1. scrollContainer 내의 모든 태그 뷰를 순회
     * 2. 각 태그 ID로 데이터베이스에서 성적/시간 데이터 조회
     * 3. 성적과 시간의 평균값을 계산하여 표시
     * 
     * 표시 정보:
     * - 성적 평균: 모든 성적 데이터의 평균값
     * - 시간 평균: 모든 시간 데이터의 평균값
     * - 데이터가 없으면 "데이터 없음" 표시
     */
    private fun updateSummaryInfo() {
        // scrollContainer 내의 모든 태그 뷰를 순회하며 요약 정보 업데이트
        for (i in 0 until scrollContainer.childCount) {
            val tagContainer = scrollContainer.getChildAt(i) as LinearLayout
            val tagId = tagContainer.tag as Long
            
            // 태그 컨테이너의 다섯 번째 자식이 요약 정보를 표시하는 TextView
            val summaryText = tagContainer.getChildAt(5) as TextView
            
            // 데이터베이스에서 성적 데이터 조회 및 평균 계산
            val scoreData = dbHelper.getScoreData(tagId)
            val averageScore = if (scoreData.isNotEmpty()) {
                val totalScore = scoreData.sumOf { it.second.toDouble() }
                (totalScore / scoreData.size).toInt()
            } else 0
            
            // 데이터베이스에서 시간 데이터 조회 및 평균 계산
            val timeData = dbHelper.getTimeData(tagId)
            val averageTime = if (timeData.isNotEmpty()) {
                val totalTime = timeData.sumOf { it.second.toDouble() }
                (totalTime / timeData.size).toInt()
            } else 0
            
            // 요약 정보 텍스트 생성
            val scoreText = if (averageScore > 0) "평균 ${averageScore}점" else "데이터 없음"
            val timeText = if (averageTime > 0) "평균 ${averageTime}시간" else "데이터 없음"
            
            summaryText.text = "성적: $scoreText | 시간: $timeText"
        }
    }

    /**
     * 화면에 표시된 모든 태그의 태스크 목록을 업데이트하는 함수
     * 
     * 동작 과정:
     * 1. scrollContainer 내의 모든 태그 뷰를 순회
     * 2. 각 태그 ID로 데이터베이스에서 태스크 목록 조회
     * 3. 태스크 목록 컨테이너에 체크박스와 함께 표시
     * 4. 체크박스 클릭 시 데이터베이스 업데이트 및 모든 페이지에서 반영되도록 수정합니다.
     * 
     * UI 구조:
     * - 각 태스크를 체크박스 + 텍스트로 표시
     * - 계획 날짜와 완료 상태 포함
     * - 최대 3개까지만 표시 (공간 절약)
     * - 체크박스 클릭 시 실시간 데이터베이스 업데이트
     */
    private fun updateTaskLists() {
        val dpToPx = resources.displayMetrics.density
        
        for (i in 0 until scrollContainer.childCount) {
            val tagContainer = scrollContainer.getChildAt(i) as LinearLayout
            val tagId = tagContainer.tag as Long
            
            // 태그 컨테이너의 다섯 번째 자식이 태스크 목록 컨테이너
            val taskListContainer = tagContainer.getChildAt(4) as LinearLayout
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
     * 그래프 데이터 입력 다이얼로그 (제거)
     * 그래프 기능을 제거하여 안정성 확보
     */
    // showGraphInputDialog() 함수 제거

    /**
     * 성적 입력 다이얼로그 (제거)
     * 그래프 기능을 제거하여 안정성 확보
     */
    // showScoreInputDialog() 함수 제거

    /**
     * 시간 입력 다이얼로그 (제거)
     * 그래프 기능을 제거하여 안정성 확보
     */
    // showTimeInputDialog() 함수 제거
}