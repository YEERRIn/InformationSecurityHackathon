package com.example.hackathon_sms

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var textViewMessage: TextView
    private lateinit var textViewMessageContent: TextView
    private lateinit var smsReceiver: BroadcastReceiver

    private val SMS_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textViewMessage = findViewById(R.id.textViewMessage)
        textViewMessageContent = findViewById(R.id.textViewMessageContent)

        //sms 권한 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), SMS_PERMISSION_CODE)
        }

        smsReceiver = object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                val message = intent?.getStringExtra("Message")
                textViewMessageContent.text = "$message"
            }
        }

        val filter = IntentFilter("sms_Received")
        registerReceiver(smsReceiver, filter)

    }



    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(smsReceiver)
    }
}