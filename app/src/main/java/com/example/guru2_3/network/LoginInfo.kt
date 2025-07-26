package com.example.guru2_3.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class Info(
    val id: String,
    val password: String
)

@Serializable
data class InfoResponse(
    @SerialName("id") val id: String = "",
    @SerialName("password") val password: String = "",
){
    fun toInfo(): Info{
        return Info(
            id = id,
            password = password
        )
    }
}

// 카멜케이스: serialName -> 실제 코드
// 스네이크케이스: serial_name -> 서버통신