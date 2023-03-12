package com.example.asazna

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firestore.v1.FirestoreGrpc.FirestoreBlockingStub
import com.squareup.picasso.Picasso

class PostAdapter(private val postList:ArrayList<GetPostData>):RecyclerView.Adapter<PostAdapter.ViewHolderItem>(){

    inner class ViewHolderItem(v:View):RecyclerView.ViewHolder(v){
        val txusernameHolder:TextView = v.findViewById(R.id.txusername)
        val txposteHolder:TextView = v.findViewById(R.id.txpost)
        val txCreatedAt:TextView = v.findViewById(R.id.txCreatedAt)
        val userImage:ImageView = v.findViewById(R.id.userimage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderItem {
        val postXml =LayoutInflater.from(parent.context)
            .inflate(R.layout.postlayout,parent,false)
        return ViewHolderItem(postXml)
    }

    override fun onBindViewHolder(holder: ViewHolderItem, position: Int) {
                val myPost = postList[position]
                holder.txusernameHolder.text = myPost.username
                holder.txposteHolder.text = myPost.postContent
                holder.txCreatedAt.text = myPost.createdAt.toString()

        Log.d("AsaznaLog","${holder.txCreatedAt.text}を取得しました。")
               // Picasso.get().load(myPost.profileImage).into(holder.userImage)
    }

    override fun getItemCount(): Int {
        return postList.size
    }
}