package com.example.guru2_3

data class TodoItem(
    var text: String,
    var tagName: String,
    var date: String,
    var isDone : Boolean = false
)
