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
import org.json.JSONObject
import org.json.JSONException
import android.widget.Toast
import android.os.Handler
import android.os.Looper

class SMSReceiver : BroadcastReceiver() {
    private var context: Context? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        this.context = context // context를 저장

        if (intent?.action == "android.provider.Telephony.SMS_RECEIVED") {
            Log.v("test", "SMS 수신됨")
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
    private fun smsMessageParse(bundle: Bundle?): Array<SmsMessage?>? {
        if (bundle == null) return null
        val objs = bundle["pdus"] as Array<Any>?

        if (objs == null) return null
        val message: Array<SmsMessage?> = arrayOfNulls(objs.size)
        for (i in objs.indices) {
            message[i] = SmsMessage.createFromPdu(objs[i] as ByteArray)
        }
        return message
    }

    // URL 패턴 탐지
    private fun extractURL(message: String): List<String> {
        val urlPattern = "(https?://\\S+)".toRegex()
        return urlPattern.findAll(message)
            .map { it.value }
            .toList()
    }

    private fun sendUrlToContainer(extractedUrl: String) {
        try {
            val url = URL("http://133.186.228.209:5556/analyze")
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

            val responseCode = conn.responseCode
            Log.d("URL 전송 결과", "Response Code: $responseCode")

            // 응답 메시지 처리
            val responseMessage = if (responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream.bufferedReader().use { it.readText() }
            }
            Log.d("분석 결과", responseMessage)

            context?.let { ctx ->
                Handler(Looper.getMainLooper()).post {
                    handleResponse(ctx, responseMessage)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("URL 전송 실패", e.toString())
            context?.let { ctx ->
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(ctx, "URL 전송 중 오류 발생", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 응답 메시지를 처리하는 함수 (예: UI 업데이트, 사용자 알림 등)
    private fun handleResponse(context: Context, responseMessage: String) {
        try {
            val jsonObject = JSONObject(responseMessage)

            // static_analysis 존재 여부 확인
            val staticAnalysis = if (jsonObject.has("static_analysis")) {
                jsonObject.getJSONObject("static_analysis")
            } else {
                null
            }

            // dynamic_analysis 존재 여부 확인
            val dynamicAnalysis = if (jsonObject.has("dynamic_analysis")) {
                jsonObject.getJSONObject("dynamic_analysis")
            } else {
                null
            }

            // 기본 값 설정
            val hasHttps = staticAnalysis?.optBoolean("has_https", false) ?: false
            val urlLength = staticAnalysis?.optInt("url_length", 0) ?: 0
            val dynamicStatus = dynamicAnalysis?.optString("status", "unknown") ?: "unknown"
            val logAnalysis = dynamicAnalysis?.optString("log_analysis", "") ?: ""

            // 분석 결과에 따라 메시지 생성
            val isSuspicious = logAnalysis.contains("suspicious", ignoreCase = true) ||
                    urlLength > 20 || !hasHttps

            val resultMessage = if (!isSuspicious) {
                "스미싱 의심: URL이 의심스럽거나 로그에서 악의적인 활동이 발견되었습니다."
            } else {
                "안전한 문자: URL은 안전하며 로그에서도 악의적인 활동이 발견되지 않았습니다."
            }

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, resultMessage, Toast.LENGTH_LONG).show()
            }

        } catch (e: JSONException) {
            e.printStackTrace()
            Log.e("분석 결과 처리", "JSON 파싱 오류: ${e.message}")
        }
    }

}
