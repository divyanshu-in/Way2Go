package com.application.myapplication

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.application.myapplication.databinding.ActivitySplashBinding
import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber

class SplashActivity: AppCompatActivity() {

    val auth = FirebaseAuth.getInstance()
    private lateinit var binding: ActivitySplashBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(LayoutInflater.from(this))

        if(auth.currentUser?.phoneNumber != null){
            startActivity(Intent(this, MainActivity::class.java))
        }else{
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()

        setContentView(binding.root)
    }
}