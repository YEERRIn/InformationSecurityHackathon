package com.example.hackathon_sms

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var textViewMessage: TextView
    private lateinit var textViewMessageContent: TextView
    private lateinit var smsReceiver: BroadcastReceiver
    private val SMS_PERMISSION_CODE = 101

    private lateinit var buttonSendMessage: Button
    private val sendGetRequest = sendGetRequest()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textViewMessage = findViewById(R.id.textViewMessage)
        textViewMessageContent = findViewById(R.id.textViewMessageContent)
        buttonSendMessage = findViewById(R.id.buttonSendMessage)

        sendGetRequest.setRetrofit()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), SMS_PERMISSION_CODE)
        }

        smsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val message = intent?.getStringExtra("Message")
                textViewMessageContent.text = "$message"
            }
        }

        val filter = IntentFilter("sms_Received")
        registerReceiver(smsReceiver, filter)

        buttonSendMessage.setOnClickListener {
            sendGetRequest.callAppAnalysis(object : sendGetRequest.ResponseCallback {
                override fun onSuccess(message: String) {
                    showMalwareAlert(message)
                }

                override fun onFailure(errorMessage: String) {
                    showMalwareAlert(errorMessage)
                }
            })
        }
    }

    private fun showMalwareAlert(message: String) {
        val malwareReporter = MalwareReporter(this)
        AlertDialog.Builder(this)
            .setTitle("악성 앱 경고")
            .setMessage("$message\n이 앱은 악성으로 확인되었습니다. 신고하시겠습니까?")
            .setPositiveButton("경찰청 (112)") { dialog, _ ->
                malwareReporter.reportMalware(MalwareReporter.ReportType.POLICE)
                dialog.dismiss()
            }
            .setNeutralButton("금융감독원 (1332)") { dialog, _ ->
                malwareReporter.reportMalware(MalwareReporter.ReportType.FINANCIAL)
                dialog.dismiss()
            }
            .setNegativeButton("한국인터넷진흥원 (118)") { dialog, _ ->
                malwareReporter.reportMalware(MalwareReporter.ReportType.SPAM)
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(smsReceiver)
    }
}
