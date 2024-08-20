package com.example.hackathon_sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

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

                // 각 URL을 컨테이너로 전송
                for (url in urls) {
                    Thread {
                        sendUrlToContainer(url)
                    }.start()
                }

                val broadIntent = Intent("sms_Received")
                broadIntent.putExtra("Message", content)
                context?.sendBroadcast(broadIntent)

            }
        }
    }

    // 문자 내용 파싱 함수
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

    // url 패턴 탐지
    fun extractURL(message: String): List<String> {
        val urlPattern = "(https?://\\S+)".toRegex()
        return urlPattern.findAll(message)
            .map { it.value }
            .toList()
    }

    fun sendUrlToContainer(extractedUrl: String) {
        try {
            // 도커 컨테이너의 IP 주소와 포트를 여기에 입력하세요
            val url = URL("http://192.168.56.105:5556/analyze")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; utf-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.doOutput = true

            val jsonInputString = "{\"url\": \"$extractedUrl\"}"

            conn.outputStream.use { os: OutputStream ->
                val input = jsonInputString.toByteArray(charset("utf-8"))
                os.write(input, 0, input.size)
            }

            // 서버 응답을 읽기
            val responseCode = conn.responseCode
            Log.d("URL 전송 결과", "Response Code: $responseCode")

            // 응답 본문 읽기
            val responseMessage = conn.inputStream.bufferedReader().use { it.readText() }
            Log.d("분석 결과", responseMessage)

            // 추가적으로 응답을 UI에 표시하거나 다른 작업을 수행할 수 있습니다.
            handleResponse(responseMessage)

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("URL 전송 실패", e.toString())
        }
    }

    // 응답 메시지를 처리하는 함수 (예: UI 업데이트, 사용자 알림 등)
    fun handleResponse(responseMessage: String) {
        // 이 함수에서 분석 결과를 UI에 표시하거나 다른 작업을 수행할 수 있습니다.
        // 예: Toast, Notification 등
        Log.d("분석 결과 처리", responseMessage)
    }

}
