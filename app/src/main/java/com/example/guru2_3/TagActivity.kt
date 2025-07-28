package com.example.guru2_3

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tag)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 뷰 초기화
        val mainLayout = findViewById<ConstraintLayout>(R.id.main)
        tagaddicon = findViewById(R.id.tagaddicon)
        tagaddText = findViewById(R.id.tagaddText)

        // 스크롤 가능한 컨테이너 생성
        val scrollView = ScrollView(this)
        scrollContainer = LinearLayout(this)
        scrollContainer.orientation = LinearLayout.VERTICAL
        scrollContainer.setPadding(50, 0, 50, 50) // 좌우 50dp 패딩, 하단 50dp 패딩

        scrollView.addView(scrollContainer)

        // ScrollView를 ConstraintLayout에 추가
        val scrollParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            0 // height를 0으로 하고 constraint로 조절
        )

        // 토마토 아이콘 아래부터 시작하도록 설정
        scrollParams.topToBottom = R.id.tagaddicon // 토마토 아이콘 아래에서 시작
        scrollParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        scrollParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        scrollParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        scrollParams.setMargins(0, (20 * resources.displayMetrics.density).toInt(), 0, 0) // 토마토 아이콘과 20dp 간격

        scrollView.layoutParams = scrollParams
        scrollView.id = android.view.View.generateViewId()

        mainLayout.addView(scrollView)

        // 기존 태그를 새 컨테이너로 이동
        moveExistingTagToScrollContainer()

        // 기존 태그의 Switch 클릭 리스너 설정
        val existingSwitch = findViewById<SwitchCompat>(R.id.createTagDateSwitch)
        existingSwitch?.setOnClickListener {
            performTagInfo()
        }

        // 토마토 아이콘 클릭 리스너
        tagaddicon.setOnClickListener {
            addNewTag()
        }

        // + 텍스트 클릭 리스너
        tagaddText.setOnClickListener {
            addNewTag()
        }
    }

    private fun moveExistingTagToScrollContainer() {
        // 기존 tagContainer를 찾아서 내용 복사
        val originalTagContainer = findViewById<LinearLayout>(R.id.tagContainer)
        originalTagContainer?.let { original ->
            // 기존 태그의 내용을 가져와서 새 태그 생성
            val nameEditText = original.findViewById<EditText>(R.id.createTagNameEditText)
            //val existingSwitch = original.findViewById<SwitchCompat>(R.id.createTagDateSwitch)

            val tagName = nameEditText?.hint?.toString() ?: "태그 이름"

            // 기존 태그와 동일한 새 태그 생성
            val firstTag = createTagView(tagName, isFirst = true)
            scrollContainer.addView(firstTag)

            // 기존 태그 완전히 제거
            original.visibility = android.view.View.GONE
        }
    }

    private fun addNewTag() {
        val newTag = createTagView("태그 이름", isFirst = false)
        scrollContainer.addView(newTag)

        Toast.makeText(this, "새 태그가 추가되었습니다!", Toast.LENGTH_SHORT).show()
    }

    private fun createTagView(tagName: String, isFirst: Boolean): LinearLayout {
        val dpToPx = resources.displayMetrics.density

        // 태그 컨테이너 생성
        val tagContainer = LinearLayout(this)
        tagContainer.orientation = LinearLayout.VERTICAL
        tagContainer.setBackgroundColor("#F44336".toColorInt())

        // 레이아웃 파라미터 설정
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, // 가로는 match_parent로 설정
            (173 * dpToPx).toInt() // 세로는 173dp
        )

        // 첫 번째 태그가 아니면 위쪽 마진 추가
        if (!isFirst) {
            layoutParams.topMargin = (20 * dpToPx).toInt()
        }

        tagContainer.layoutParams = layoutParams
        tagContainer.id = android.view.View.generateViewId()

        // 1. EditText 추가 (태그 이름)
        val nameEditText = EditText(this)
        nameEditText.hint = tagName
        nameEditText.setTypeface(null, android.graphics.Typeface.BOLD)
        nameEditText.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // 2. TextView 추가 (완수율)
        val finishRateText = TextView(this)
        finishRateText.text = "완수율"
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

        // Switch 클릭 리스너 설정
        dateSwitch.setOnClickListener {
            performTagInfo()
        }

        // 모든 뷰를 컨테이너에 추가
        tagContainer.addView(nameEditText)
        tagContainer.addView(finishRateText)
        tagContainer.addView(dateSwitch)

        return tagContainer
    }

    private fun performTagInfo() {
        val intent = Intent(this, TagInfoActivity::class.java)
        startActivity(intent)
    }
}