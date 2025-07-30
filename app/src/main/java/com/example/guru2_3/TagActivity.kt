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
        userId = intent.getLongExtra("USER_ID", 0)

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

        // 기존 태그를 새 컨테이너로 이동
        moveExistingTagToScrollContainer()

        // 토마토 아이콘 클릭 리스너
        tagaddicon.setOnClickListener {
            addNewTag()
        }

        // + 텍스트 클릭 리스너
        tagaddText.setOnClickListener {
            addNewTag()
        }

        loadExistingTags()
//        if (scrollContainer.childCount == 0) {
//            moveExistingTagToScrollContainer()
//        }
    }

    private fun moveExistingTagToScrollContainer() {
        val originalTagContainer = findViewById<LinearLayout>(R.id.tagContainer)
        originalTagContainer?.let { original ->
            val nameEditText = original.findViewById<EditText>(R.id.createTagNameEditText)
            val tagName = nameEditText?.hint?.toString() ?: "태그 이름"

            // 데이터베이스에 첫 번째 태그 생성
            val tagId = dbHelper.createTag(tagName)
            val firstTag = createTagView(tagName, tagId,  isEditMode = true)
            scrollContainer.addView(firstTag)

            original.visibility = android.view.View.GONE
        }
    }

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

    private fun createTagView(tagName: String, tagId: Long, isEditMode: Boolean): LinearLayout {
        val dpToPx = resources.displayMetrics.density

        // 태그 컨테이너 생성
        val tagContainer = LinearLayout(this)
        tagContainer.orientation = LinearLayout.VERTICAL
        tagContainer.setBackgroundColor("#F44336".toColorInt())
        tagContainer.tag = tagId // 태그 ID를 View의 tag로 저장

        // 레이아웃 파라미터 설정
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (173 * dpToPx).toInt()
        )

        layoutParams.topMargin = (20 * dpToPx).toInt()

        tagContainer.layoutParams = layoutParams
        tagContainer.id = android.view.View.generateViewId()

        // 1. EditText 추가 (태그 이름)
        val nameEditText = EditText(this)
        nameEditText.setText(tagName) // hint 대신 text로 설정
        nameEditText.setTypeface(null, android.graphics.Typeface.BOLD)
        nameEditText.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // EditText 변경 감지 리스너
        nameEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val newTagName = nameEditText.text.toString()
                if (newTagName.isNotEmpty()) {
                    dbHelper.updateTagName(tagId, newTagName)
                }
            }
        }

        // 2. TextView 추가 (완수율)
        val finishRateText = TextView(this)
        finishRateText.text = "완수율: 0%" // 초기값 설정
        finishRateText.setTextColor(Color.BLACK)
        finishRateText.textSize = 20f
        val finishRateParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        finishRateParams.topMargin = (20 * dpToPx).toInt()
        finishRateText.layoutParams = finishRateParams

        // 3. TextView 추가 (D-day 표시)
        val ddayText = TextView(this)
        ddayText.text = "D-day: 설정되지 않음" // 초기값 설정
        ddayText.setTextColor(Color.BLACK)
        ddayText.textSize = 18f
        val ddayParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        ddayParams.topMargin = (10 * dpToPx).toInt()
        ddayText.layoutParams = ddayParams

        // 4. SwitchCompat 추가 (시험일정)
        val dateSwitch = SwitchCompat(this)
        dateSwitch.text = "시험일정"
        dateSwitch.textSize = 20f
        val switchParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        switchParams.topMargin = (10 * dpToPx).toInt()
        dateSwitch.layoutParams = switchParams

        // Switch 클릭 리스너 설정 - 각 태그별 고유 데이터 전달
        dateSwitch.setOnClickListener {
            val currentTagName = nameEditText.text.toString()
            navigateToTagInfo(currentTagName, tagId)
        }

        // 모든 뷰를 컨테이너에 추가
        tagContainer.addView(nameEditText)
        tagContainer.addView(finishRateText)
        tagContainer.addView(ddayText)
        tagContainer.addView(dateSwitch)

        return tagContainer
    }

    private fun navigateToTagInfo(tagName: String, tagId: Long) {
        val intent = Intent(this, TagInfoActivity::class.java)
        intent.putExtra("TAG_NAME", tagName)
        intent.putExtra("TAG_ID", tagId)
        startActivity(intent)
    }

    /**
     * 액티비티가 다시 활성화될 때 호출되는 함수
     * 
     * 다른 화면 (예: TagInfoActivity, MainActivity)에서 태스크 상태가 변경된 후
     * 이 화면으로 돌아올 때 최신 완수율과 D-day를 반영하기 위해 사용
     * 
     * 실행 순서:
     * 1. loadExistingTags(): 데이터베이스에서 최신 태그 목록 로드
     * 2. updateCompletionRates(): 각 태그의 완수율을 최신 상태로 업데이트
     * 3. updateDdays(): 각 태그의 D-day를 최신 상태로 업데이트
     */
    override fun onResume() {
        super.onResume()
        loadExistingTags()        // 최신 태그 목록 로드
        updateCompletionRates()   // 완수율 실시간 업데이트
        updateDdays()             // D-day 실시간 업데이트
    }
    // 기존 태그들을 데이터베이스에서 로드하는 메서드 추가
    private fun loadExistingTags() {
        scrollContainer.removeAllViews() // 기존 뷰들 제거

        val existingTags = dbHelper.getAllTags(userId)
        for ((tagId, tagName) in existingTags) {
            val tagView = createTagView(tagName, tagId, isEditMode = true)
            scrollContainer.addView(tagView)
        }

        tagCounter = existingTags.size
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
     * ├── TextView (완수율) - index 1  ← 여기를 업데이트
     * ├── TextView (D-day) - index 2  ← 여기를 업데이트
     * └── Switch (시험일정) - index 3
     */
    private fun updateCompletionRates() {
        // scrollContainer 내의 모든 태그 뷰를 순회하며 완수율 업데이트
        for (i in 0 until scrollContainer.childCount) {
            val tagContainer = scrollContainer.getChildAt(i) as LinearLayout
            val tagId = tagContainer.tag as Long // 태그 ID는 View의 tag 속성에 저장됨
            
            // 태그 컨테이너의 두 번째 자식이 완수율을 표시하는 TextView
            val finishRateText = tagContainer.getChildAt(1) as TextView
            
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
            
            // 태그 컨테이너의 세 번째 자식이 D-day를 표시하는 TextView
            val ddayText = tagContainer.getChildAt(2) as TextView
            
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
}