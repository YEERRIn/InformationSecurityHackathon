package com.example.hackathon_sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import android.util.Patterns


class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent?.action =="android.provider.Telephony.SMS_RECEIVED"){ // 문자가 왔을 때 발생하는 action
            Log.v("test", "있어요")
            val bundle = intent.extras
            val messages = smsMessageParse(bundle)

            if (messages?.isNotEmpty() == true) {
                val content = messages[0]?.messageBody.toString()
                Log.d("SMS Content", content)

                // URL 추출
                val urls = extractURL(content)
                Log.d("추출된 URL:", urls.joinToString(", "))

                val broadIntent = Intent("sms_Received")
                broadIntent.putExtra("Message", content)
                context?.sendBroadcast(broadIntent)

            }
        }


    }

    //문자 내용 파싱 함수
    fun smsMessageParse(bundle: Bundle?): Array<SmsMessage?>? {
        if (bundle == null) return null
        val objs = bundle["pdus"] as Array<Any>?

        if (objs == null) return null
        val message: Array<SmsMessage?> = arrayOfNulls(objs.size)
        for (i in objs.indices) {
            message[i] = SmsMessage.createFromPdu(objs[i] as ByteArray)
        }
        return message
    }

    //url 패턴 탐지
    fun extractURL(message: String): List<String> {

        val urlPattern = "(https?://\\S+)".toRegex()
        return urlPattern.findAll(message)
            .map { it.value }
            .toList()
    }
}

