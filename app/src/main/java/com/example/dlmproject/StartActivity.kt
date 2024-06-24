package com.example.dlmproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_start)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val logInButton = findViewById<Button>(R.id.buttonLogIn)
        logInButton.setOnClickListener{
            val intent = Intent( this, LogInActivity::class.java)
            startActivity(intent)
        }
        val signInButton = findViewById<Button>(R.id.buttonRegister)
        signInButton.setOnClickListener{
            val intent = Intent( this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}