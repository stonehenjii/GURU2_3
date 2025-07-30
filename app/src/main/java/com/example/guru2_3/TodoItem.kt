package com.example.guru2_3

data class TodoItem(
    var id: Long = 0,
    var text: String,
    var tagName: String,
    var date: String,
    var isDone : Boolean = false
)
