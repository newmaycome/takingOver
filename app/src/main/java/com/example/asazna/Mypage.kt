package com.example.asazna

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.squareup.picasso.Picasso
import io.realm.Realm
import java.util.*
import kotlin.collections.ArrayList

class Mypage : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private  val auth = FirebaseAuth.getInstance()
    private val user = auth.currentUser?.uid.toString()
    private val realm : Realm = Realm.getDefaultInstance()


    private lateinit var recyclerView: RecyclerView
    private lateinit var postList :ArrayList<GetPostData>
    private lateinit var recyclerAdapter : PostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mypage)

        val arrawback = findViewById<ImageView>(R.id.arrawback)

        arrawback.setOnClickListener {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()

        val txname = findViewById<TextView>(R.id.txname)
        val userimage = findViewById<ImageView>(R.id.userimage)
        val txPostNum = findViewById<TextView>(R.id.txpostNum)
        val txFollowNum = findViewById<TextView>(R.id.txfollowNum)
        val txFollowerNum = findViewById<TextView>(R.id.txfollowerNum)
        val fbtNewPost = findViewById<FloatingActionButton>(R.id.fbtNewPost)
        val myname: String? = intent.getStringExtra("editorname")

        Log.d("AsaznaLog","${myname}のマイページを表示します")


//マイページのユーザー情報の取得等について
        txname.text = myname

        db.collection("users")
            .whereEqualTo("username",myname)  //同じ名前のユーザーが存在した場合これではNG→修正の必要あり
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val postNum = document.data["postCount"].toString()
                    val followNum = document.data["followingCount"].toString()
                    val followerNum = document.data["beingFollowedCount"].toString()
                    val userImageUrl = document.data["profileImageUrl"].toString()
                    val uid = document.data["uid"].toString()

                    if(uid != user){
                        Log.d("AsaznaLog","uid:${uid},currentuser:${user}→他人だね！")
                        fbtNewPost.visibility = View.INVISIBLE
                    }else{
                        Log.d("AsaznaLog","uid:${uid},currentuser:${user}→同一人物だね！")
                        fbtNewPost.visibility = View.VISIBLE
                    }

                    txPostNum.text  = postNum
                    txFollowNum.text = followNum
                    txFollowerNum.text = followerNum
                    Picasso.get().load(userImageUrl).into(userimage)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this,"ユーザー情報を取得できませんでした",Toast.LENGTH_LONG).show()
            }


        //ポスト情報の取得について
        recyclerView = findViewById(R.id.rv)
        postList = arrayListOf()
        recyclerView.layoutManager = LinearLayoutManager(this)

        Log.d("AsaznaLog","ほな${myname}のポスト情報取得していきましょ")

        db.collection("users")
            .whereEqualTo("username",myname)
            .get()
            .addOnSuccessListener {result ->
                for(document in result) {
                    val myPageUid = document.data["uid"].toString()
                    getPostInfo(myPageUid)
                }
            }

        fbtNewPost.setOnClickListener {
            val intent = Intent(this,CreatePost::class.java)
            intent.putExtra("username",myname)
            startActivity(intent)
        }
    }
    private fun getPostInfo(myPageUid:String){
        db.collection("users")
            .document(myPageUid)
            .collection("myPost")
            .orderBy("createdAt",Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener{result->
                for (document in result) {
                    Log.d("AsaznaLog","${document.id} →${document.data}")

                    val post:GetPostData = document.toObject(GetPostData::class.java)

                    recyclerView.adapter = PostAdapter(postList)
                    Log.d("AsaznaLog", "${recyclerView.adapter}を取得しましたぞよ～～～")

                    if (post != null) {
                        postList.add(post)
                    } else {
                        Log.d("AsaznaLog", "nullだよ～～～ん")
                    }
                }
               recyclerView.adapter = PostAdapter(postList)
                Log.d("AsaznaLog", "もう一息！")

            }
            .addOnFailureListener {
                Log.d("AsaznaLog", "${it}が発生しました！")
            }

    }
}