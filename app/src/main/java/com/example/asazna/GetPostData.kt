package com.example.asazna

import java.util.*

data class GetPostData(
    val username:String = "",
    val postContent:String = "",
    val createdAt: Date? = null
)
