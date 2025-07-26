package com.example.guru2_3

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
//import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
//import kotlinx.serialization.json.Json
//import okhttp3.MediaType.Companion.toMediaType
//import retrofit2.Retrofit


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

//    val json = Json {
//        ignoreUnknownKeys = true // 알 수 없는 JSON 키 무시
//        coerceInputValues = true // null 값을 기본값으로 변환
//    }

//    val baseUrl = "" // 서버에서 내려주는 값을 저장해

//    val retrofit = Retrofit.Builder()
//        .baseUrl(baseUrl)
//        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
//        .build()

    //val loginApiService = retrofit.create(LoginApiService::class.java)

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



        // 2. 새로운 메모 추가
        dbHelper.addMemo("DB Helper 테스트 메모")
        Log.d("DatabaseTest", "새로운 메모를 추가했습니다.")

        // 3. 모든 메모를 불러와서 로그로 출력
        val memoList = dbHelper.getAllMemos()
        Log.d("DatabaseTest", "--- 전체 메모 목록 ---")
        for (memo in memoList) {
            Log.d("DatabaseTest", "ID: ${memo.id}, 내용: ${memo.content}")
        }
        // ------------------------------------
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


        //retrofit.

        //infoResponse.onSuccess {
        //    if(infoResponse.id == username && infoResponse.password == password) moveToHome()
        //}
    }

    private fun performRegister() {
        // 회원가입 화면으로 이동
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
}