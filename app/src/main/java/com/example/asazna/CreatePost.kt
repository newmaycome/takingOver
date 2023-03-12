package com.example.asazna

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import io.realm.Realm
import java.util.*

class CreatePost : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private  val auth = FirebaseAuth.getInstance()
    private val user = auth.currentUser?.uid.toString()
    private val realm : Realm = Realm.getDefaultInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        val txDoPost = findViewById<TextView>(R.id.txDoPost)
        val txCancel = findViewById<TextView>(R.id.txCansel)
        val etPost = findViewById<EditText>(R.id.etPost)
        val txSpot = findViewById<TextView>(R.id.txSpot)
        val userImage = findViewById<ImageView>(R.id.userImage)
        val uuid = UUID.randomUUID()

        val postUserName = intent.getStringExtra("username")

        //ポスト画面に自身のアイコンを表示する
        db.collection("users")
            .whereEqualTo("username",postUserName)
            .get()
            .addOnSuccessListener { result->
                for(document in result){
                    val userImageUrl = document.data["profileImageUrl"].toString()
                    Picasso.get().load(userImageUrl).into(userImage)
                }
            }

        txDoPost.setOnClickListener {

            db.collection("users")
                .whereEqualTo("username",postUserName)
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        val userImageUrl = document.data["profileImageUrl"].toString()

                        val postData = PostData(
                            postContent = etPost.text.toString(),
                            postUser = user,
                            storeName = txSpot.text.toString(),
                            likeCount = 0,
                            createdAt = Date(System.currentTimeMillis()),
                            postImageUrl = "",
                            postImage2Url = "",
                            postImage3Url = "",
                            postImage4Url = "",
                            postUuid = uuid.toString()
                        )

                        db.collection("users")
                            .document(user)
                            .collection("myPost")
                            .document(uuid.toString())
                            .set(postData)
                            .addOnSuccessListener {
                                Log.d("AsaznaLog", "投稿成功！でもまだ共有はできてないで")

                                db.collection("Post")
                                    .document(uuid.toString())
                                    .set(postData)
                                    .addOnSuccessListener {
                                        Log.d("AsaznaLog", "共有完了")
                                        Toast.makeText(this, "投稿しました", Toast.LENGTH_LONG).show()
                                        addPostCount()
                                        addTimeLinePost(postData, uuid.toString())
                                        finish()
                                    }
                                    .addOnFailureListener {
                                        Log.d("AsaznaLog", "共有失敗。投稿はマイページのみ")
                                        finish()
                                    }
                            }
                            .addOnFailureListener {
                                Log.d("AsaznaLog", "投稿失敗")
                                Toast.makeText(this, "投稿中にエラーが発生しました", Toast.LENGTH_LONG).show()
                            }
                    }
                }
        }
        txCancel.setOnClickListener {
            finish()
        }
    }

    private fun addPostCount(){
        db.collection("users")
            .document(user)
            .get()
            .addOnCompleteListener {
                val getFresult = it.result
                val postCount = getFresult.data?.get("postCount").toString().toInt()
                val newPostCount = postCount + 1

                db.collection("users")
                    .document(user).update("postCount",newPostCount)
            }
    }

    private fun addTimeLinePost(postData:PostData,uuid:String){

        db.collection("users")
            .document(user)
            .collection("timeLinePosts")
            .document(uuid)
            .set(postData)
            .addOnSuccessListener {
                Log.d("AsaznaLog","己のタイムラインには表示されてまっせ")
            }
            .addOnFailureListener {
                Log.d("AsaznaLog","己のタイムラインにも表示されてないで")
            }

        db.collection("users")
            .document(user)
            .collection("beingFollowedUsers")
            .get()
            .addOnSuccessListener { result ->
                for(documents in result){

                    Log.d("AsaznaLog","${documents.id}を取得するで")

                   db.collection("users")
                       .document(documents.id)
                       .collection("timeLinePosts")
                       .document(uuid)
                       .set(postData)
                       .addOnSuccessListener {
                           Log.d("AsaznaLog","タイムライン表示完了")
                       }
                       .addOnFailureListener {
                           Log.d("AsaznaLog","エラー！タイムラインには表示されず")
                       }
                }
            }
    }
}

