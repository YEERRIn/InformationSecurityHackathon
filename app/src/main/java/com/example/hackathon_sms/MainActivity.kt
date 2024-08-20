package com.example.hackathon_sms

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {

    //수신 받은 메시지에 대한 변수
    private lateinit var textViewMessage: TextView
    private lateinit var textViewMessageContent: TextView
    private lateinit var smsReceiver: BroadcastReceiver
    private val SMS_PERMISSION_CODE = 101

    private lateinit var buttonSendMessage : Button //전송 버튼
    private val sendGetRequest = sendGetRequest() //sendGetRequest.kt 불러오기..


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textViewMessage = findViewById(R.id.textViewMessage)
        textViewMessageContent = findViewById(R.id.textViewMessageContent)

        buttonSendMessage = findViewById(R.id.buttonSendMessage)

        // Retrofit 초기화
        sendGetRequest.setRetrofit()

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

        //버튼 클릭시 get 요청
        buttonSendMessage.setOnClickListener {
            //get요청에 대한 결과를 가져옴
            sendGetRequest.callAppAnalysis(object : sendGetRequest.ResponseCallback{
                //팝업 메시지 띄우기
                override fun onSuccess(message: String) {
                    showPopUP(message)
                }

                override fun onFailure(errorMessage: String) {
                    showPopUP(errorMessage)
                }
            })
        }

    }

    private fun showPopUP(message: String){
        AlertDialog.Builder(this)
            .setTitle("dataAnalysis")
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(smsReceiver)
    }


}