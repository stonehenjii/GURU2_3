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

class TagActivity : AppCompatActivity() {
    private lateinit var scrollContainer: LinearLayout
    private lateinit var tagaddicon: ImageView
    private lateinit var tagaddText: TextView
    private lateinit var dbHelper: DatabaseHelper
    private var tagCounter = 0 // 태그 고유 ID 생성용

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        // 3. SwitchCompat 추가 (시험일정)
        val dateSwitch = SwitchCompat(this)
        dateSwitch.text = "시험일정"
        dateSwitch.textSize = 20f
        val switchParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        switchParams.topMargin = (20 * dpToPx).toInt()
        dateSwitch.layoutParams = switchParams

        // Switch 클릭 리스너 설정 - 각 태그별 고유 데이터 전달
        dateSwitch.setOnClickListener {
            val currentTagName = nameEditText.text.toString()
            navigateToTagInfo(currentTagName, tagId)
        }

        // 모든 뷰를 컨테이너에 추가
        tagContainer.addView(nameEditText)
        tagContainer.addView(finishRateText)
        tagContainer.addView(dateSwitch)

        return tagContainer
    }

    private fun navigateToTagInfo(tagName: String, tagId: Long) {
        val intent = Intent(this, TagInfoActivity::class.java)
        intent.putExtra("TAG_NAME", tagName)
        intent.putExtra("TAG_ID", tagId)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadExistingTags()

    }
    // 기존 태그들을 데이터베이스에서 로드하는 메서드 추가
    private fun loadExistingTags() {
        scrollContainer.removeAllViews() // 기존 뷰들 제거

        val existingTags = dbHelper.getAllTags()
        for ((tagId, tagName) in existingTags) {
            val tagView = createTagView(tagName, tagId, isEditMode = true)
            scrollContainer.addView(tagView)
        }

        tagCounter = existingTags.size
    }
}