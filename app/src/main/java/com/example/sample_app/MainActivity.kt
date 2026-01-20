package com.example.sample_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.chato.sdk.Chato

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Chato.init(
            context = applicationContext,
            apiKey = "app_b84a5ec606bf5ee08b043a268040e5cf6cf7d596d2ec2769"
        )
    }

    override fun onResume() {
        super.onResume()
        Chato.attach(this)
    }

    override fun onPause() {
        super.onPause()
        Chato.detach()
    }
}
