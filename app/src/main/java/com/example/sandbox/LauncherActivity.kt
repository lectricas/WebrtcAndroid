package com.example.sandbox

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        findViewById<Button>(R.id.start).setOnClickListener {
            val username = findViewById<EditText>(R.id.username).text.toString()
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra(
                    Aux.USERNAME,
                    username
                )
            })
        }
    }

}