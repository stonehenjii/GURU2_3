package com.example.guru2_3
import DatabaseHelper
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class LoginActivity : AppCompatActivity() {

    private lateinit var idEditText: EditText
    private lateinit var pwEditText: EditText
    private lateinit var timerLogo: ImageView
    private lateinit var idIcon: ImageView
    private lateinit var pwIcon: ImageView
    private lateinit var registerIcon: ImageView
    private lateinit var loginIcon: ImageView
    private lateinit var idText: TextView
    private lateinit var pwText: TextView
    private lateinit var loginText: TextView
    private lateinit var registerText: TextView
    private lateinit var sharedPreferences: SharedPreferences

    private val dbHelper = DatabaseHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)
        //ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
        //val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        //v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
        //insets
        //}

        initViews()
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        setClickListeners()




    }

    private fun initViews() {
        idEditText = findViewById(R.id.IDEditText)
        pwEditText = findViewById(R.id.pwEditText)
        registerIcon = findViewById(R.id.registerIcon)
        idIcon = findViewById(R.id.IDIcon)
        pwIcon = findViewById(R.id.pwIcon)
        loginIcon = findViewById(R.id.loginIcon)
        timerLogo = findViewById(R.id.TimerLogo)
        registerText = findViewById(R.id.registerText)
        idText = findViewById(R.id.IDText)
        pwText = findViewById(R.id.pwText)
        loginText = findViewById(R.id.loginText)
    }

    private fun setClickListeners() {
        // 회원가입 클릭 (아이콘 + 텍스트)
        registerIcon.setOnClickListener { performRegister() }
        registerText.setOnClickListener { performRegister() }

        // 로그인 클릭 (아이콘 + 텍스트)
        loginIcon.setOnClickListener { performLogin() }
        loginText.setOnClickListener { performLogin() }
    }

    private fun performLogin() {
        val username = idEditText.text.toString().trim()
        val password = pwEditText.text.toString().trim()

        // 입력값 검증
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "아이디와 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val dbHelper = DatabaseHelper(this)

        // 🔥 먼저 기존 사용자인지 확인
        if (dbHelper.validateUser(username, password)) {
            // 기존 사용자 - 로그인 성공
            Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
            movetotimer()
        } else {
            // 🔥 새로운 사용자 - 자동 회원가입
            try {
                // 닉네임을 아이디와 동일하게 설정 (또는 다른 로직 사용)
                val nickname = username // 또는 "${username}_user" 등

                val success = dbHelper.addUser(username, password, nickname)
                if (success != -1L) {
                    Toast.makeText(this, "자동 회원가입 완료! 로그인됩니다.", Toast.LENGTH_SHORT).show()
                    movetotimer()
                } else {
                    Toast.makeText(this, "회원가입 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "로그인 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun performRegister() {
        // 회원가입 화면으로 이동
        Toast.makeText(this, "회원가입 함수 진입!", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
    private fun movetotimer() {
        // 타이머 화면으로 이동(현재는 임시로 tagactivity 화면으로 이동합니다)
        val intent = Intent(this, TagActivity::class.java)
        startActivity(intent)
    }
}


