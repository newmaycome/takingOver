package com.example.asazna

import android.widget.ImageView
import com.google.firebase.firestore.DocumentId
import java.util.*

data class PostData(
    val postContent:String = "",
    val likeCount:Int= 0,
    val geoCode:String = "",
    val postUser:String = "",
    val storeName:String = "",
    val createdAt:Date = Date(System.currentTimeMillis()),
    val postImageUrl:String = "",
    val postImage2Url:String = "",
    val postImage3Url:String = "",
    val postImage4Url:String = "",
    val postUuid:String = ""
        )
