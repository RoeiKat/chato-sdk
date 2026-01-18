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
            apiKey = "app_9c213aa786a83b41b8da03d0de93a358f073ba412d70c78f"
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
