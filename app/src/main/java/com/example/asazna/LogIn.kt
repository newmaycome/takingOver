package com.example.asazna

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.asazna.databinding.ActivityLogInBinding
import com.google.firebase.auth.FirebaseAuth

class LogIn : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private lateinit var binding: ActivityLogInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_log_in)
        binding = ActivityLogInBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.btlogin.setOnClickListener {
            val emailtext = binding.etemail.text.toString()
            val passtext = binding.etpass.text.toString()
            auth.signInWithEmailAndPassword(emailtext, passtext)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d("AsaznaLog", "Login Success!")
                        val intent = Intent(this, MapsActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "ログインに失敗しました。", Toast.LENGTH_SHORT).show()
                        binding.etpass.text.clear()
                    }
                }
        }
        binding.tx2.setOnClickListener {
            val intent = Intent(this,SignUp::class.java)
            startActivity(intent)
            finish()
        }
    }
}