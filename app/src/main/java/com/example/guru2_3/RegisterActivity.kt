package com.example.guru2_3

import DatabaseHelper
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

class RegisterActivity : AppCompatActivity() {

    private lateinit var regIDEditText: EditText
    private lateinit var regPwEditText: EditText
    private lateinit var regConfirmPwEditText: EditText
    private lateinit var regNicknameEditText: EditText

    private lateinit var regIDicon: ImageView
    private lateinit var regPwicon: ImageView
    private lateinit var regConfirmPwicon: ImageView
    private lateinit var regNicknameicon: ImageView
    private lateinit var regLoginicon: ImageView

    private lateinit var regTitleText: TextView
    private lateinit var regIDText: TextView
    private lateinit var regPWText: TextView
    private lateinit var regConfirmPwText: TextView
    private lateinit var regNicknameText: TextView
    private lateinit var regLoginText: TextView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initViews()
        setClickListeners()
    }
    private fun initViews() {
        // EditText 초기화
        regIDEditText = findViewById(R.id.regIDEditText)
        regPwEditText = findViewById(R.id.regPWEditText)
        regConfirmPwEditText = findViewById(R.id.regConfirmPwEditText)
        regNicknameEditText = findViewById(R.id.regNicknameEditText)

        // ImageView 초기화 (토마토 아이콘들)
        regIDicon = findViewById(R.id.regIDicon)
        regPwicon = findViewById(R.id.regPwicon)
        regConfirmPwicon = findViewById(R.id.regConfirmPwicon)
        regNicknameicon = findViewById(R.id.regNicknameicon)
        regLoginicon = findViewById(R.id.regLoginicon)

        // TextView 초기화
        regTitleText = findViewById(R.id.regTitleText)
        regIDText = findViewById(R.id.regIDText)
        regPWText = findViewById(R.id.regPWText)
        regConfirmPwText = findViewById(R.id.regConfimrPwText)
        regNicknameText = findViewById(R.id.regNicknameText)
        regLoginText = findViewById(R.id.regLoginText)
    }


    private fun performRegister() {

        val id = regIDEditText.text.toString().trim()
        val password = regPwEditText.text.toString().trim()
        val confirmPassword = regConfirmPwEditText.text.toString().trim()
        val nickname = regNicknameEditText.text.toString().trim()

        // 입력값 검증
        if (id.isEmpty()) {
            Toast.makeText(this, "아이디를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (confirmPassword.isEmpty()) {
            Toast.makeText(this, "비밀번호 확인을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (nickname.isEmpty()) {
            Toast.makeText(this, "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 비밀번호 확인
        if (password != confirmPassword) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 회원가입 성공 시
        Toast.makeText(this, "로그인 페이지로 이동합니다!", Toast.LENGTH_LONG).show()


        val dbHelper = DatabaseHelper(this)
        val success = dbHelper.addUser(id, password, nickname)
        if (success != -1L) {
            Toast.makeText(this, "회원가입 성공!", Toast.LENGTH_SHORT).show()
            movetoLoginPage()
        } else {
            Toast.makeText(this, "이미 존재하는 아이디입니다.", Toast.LENGTH_SHORT).show()
        }


    }

    private fun setClickListeners() {
        // 로그인 클릭 (아이콘 + 텍스트)
        regLoginicon.setOnClickListener { movetoLoginPage() }
        regLoginText.setOnClickListener { movetoLoginPage() }

    }

    private fun validateAndMoveToLogin() {
        val id = regIDEditText.text.toString().trim()
        val password = regPwEditText.text.toString().trim()
        val confirmPassword = regConfirmPwEditText.text.toString().trim()
        val nickname = regNicknameEditText.text.toString().trim()

        // 입력값 검증 - performRegister()와 동일한 검증 로직
        if (id.isEmpty()) {
            Toast.makeText(this, "아이디를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (confirmPassword.isEmpty()) {
            Toast.makeText(this, "비밀번호 확인을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (nickname.isEmpty()) {
            Toast.makeText(this, "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 비밀번호 확인
        if (password != confirmPassword) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 🔥 모든 검증 통과 시에만 로그인 페이지로 이동
        movetoLoginPage()
    }

    private fun movetoLoginPage() {
        // 로그인 화면으로 이동
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

}