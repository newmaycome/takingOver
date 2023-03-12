package com.example.asazna

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.asazna.databinding.ActivitySignUpBinding
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class SignUp : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private lateinit var binding: ActivitySignUpBinding
    private var REQUEST_GALLERY_TAKE = 2
    private var RECORD_REQUEST_CODE = 1000
    private var selectedPhotoUri: Uri? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_sign_up)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.iv2.setOnClickListener{

            openGallery()
            setGalleryPermission()

        }
        binding.btregister.setOnClickListener {

            signupActivity()

        }
        binding.tx2.setOnClickListener {

            val intent = Intent(this, LogIn::class.java)
            startActivity(intent)
            finish()

        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun signupActivity() {
        val emailtext: String = binding.etemail.text.toString()
        val passtext: String = binding.etpass.text.toString()

        auth.createUserWithEmailAndPassword(emailtext, passtext)
            .addOnCompleteListener(this) { Task ->
                if (Task.isSuccessful) {
                    Log.d("AsaznaLog", "Create Account Success!")

                    val intent = Intent(this, LogIn::class.java)
                    startActivity(intent)
                    addStorageActivity()
                    finish()

                } else {
                    Log.d("AsaznaLog","登録失敗")
                    Toast.makeText(this, "エラーが発生しました", Toast.LENGTH_SHORT).show()
                    binding.etpass.text.clear()
                }
            }
    }
    private fun sendEmail(){
        val emailtext: String = binding.etemail.text.toString()
        val emaillink = intent.data.toString()
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://logintestapp.page.link")//Asazna仕様に
            .setHandleCodeInApp(true)
            .build()

        auth.sendSignInLinkToEmail(emailtext, actionCodeSettings)
            .addOnCompleteListener {
                if (it.isSuccessful){
                    Log.d("AsaznaLog", "Success to send!")
                    Toast.makeText(this, "認証メールを送信しました", Toast.LENGTH_SHORT).show()
                    //confirmEmail(emailtext,emaillink)
                }else{
                    Log.d("AsaznaLog", "Fail to Send!")
                }
            }
    }

    private fun confirmEmail(email:String,emaillink:String){

        if(auth.isSignInWithEmailLink(emaillink)) {
            val credential = EmailAuthProvider.getCredentialWithLink(email,emaillink)
            auth.currentUser!!.reauthenticateAndRetrieveData(credential)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        Log.d("AsaznaLog", "Confirm E-mail Success!")
                        val intent = Intent(this, LogIn::class.java)
                        startActivity(intent)

                    } else {
                        Log.d("AsaznaLog", "Confirm E-mail Error!")
                    }
                }
        }
    }

    private fun openGallery() {

        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent,REQUEST_GALLERY_TAKE) //取り消し線は非推奨

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, photodata: Intent?) {
        super.onActivityResult(requestCode, resultCode, photodata)

        if (requestCode == REQUEST_GALLERY_TAKE && resultCode == Activity.RESULT_OK && photodata != null) {
            selectedPhotoUri = photodata.data
            try {
                selectedPhotoUri.also { selectedPhotoUri ->
                    val inputStream = contentResolver.openInputStream(selectedPhotoUri!!)
                    val image = BitmapFactory.decodeStream(inputStream)
                    binding.iv2.setImageBitmap(image)
                }
            } catch (e: java.lang.Exception) {
                Log.d("AsaznaLog","画像の取得に失敗しました。")
            }
        }
    }

    private fun setGalleryPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), RECORD_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            RECORD_REQUEST_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("AsaznaLog", "Permit!")
                } else {
                    Log.d("AsaznaLog", "Not Permit!")
                }
                return
            }
        }
    }
    //コルーチン使うべき？
    @RequiresApi(Build.VERSION_CODES.O)
    private fun addStorageActivity(){
        if(selectedPhotoUri == null) {
            return
        }else{
            val filename = UUID.randomUUID().toString()
            val refimages = storage.reference.child("/users/profile_image/$filename")

            refimages.putFile(selectedPhotoUri!!)
                .addOnSuccessListener {
                    Log.d("AsaznaLog", "PutFile Success! Filename:$filename")

                    val getImageUrl = refimages.downloadUrl

                    getImageUrl.addOnSuccessListener {
                        Log.d("AsaznaLog", "download Success! URL:$it")

                        addFirestoreActivity(it.toString())

                    }
                        .addOnFailureListener {
                            Log.d("AsaznaLog", "dawnload Fail!")
                        }
                }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addFirestoreActivity(imagesUri:String){
        val untext: String = binding.etusername.text.toString()
        val emailtext: String = binding.etemail.text.toString()
        val passtext: String = binding.etpass.text.toString()
        val user = auth.currentUser?.uid.toString()
        val createdat = ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("Asia/Tokyo"))

        if(user!=null) {
            val userinfo = mapOf(
                "username" to untext,
                "email" to emailtext,
                "password" to passtext,
                "uid" to user,
                "profileImageUrl" to imagesUri,
                "followingCount" to 0,
                "beingFollowedCount" to 0,
                "createdAt" to "$createdat"
            )

            db.collection("users")
                .document(user)
                .set(userinfo)
                .addOnSuccessListener {
                    Log.d("AsaznaLog", "Document Written!")
                }
                .addOnFailureListener { e ->
                    Log.w("AsaznaLog", "Document NOT Written!",e)
                }
        }
    }

}