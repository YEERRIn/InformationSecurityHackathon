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

            // 응답 메시지 처리
            context?.let { ctx ->
                Handler(Looper.getMainLooper()).post {
                    handleResponse(ctx, responseMessage)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("URL 전송 실패", e.toString())
            // 여기서 사용자에게 오류 메시지를 알리거나 다른 처리를 할 수 있습니다.
            context?.let { ctx ->
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(ctx, "URL 전송 중 오류 발생", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    // 응답 메시지를 처리하는 함수 (예: UI 업데이트, 사용자 알림 등)
    fun handleResponse(context: Context, responseMessage: String) {
        // JSON 파싱을 위한 기본적인 예제 (응답 형식이 변경될 경우 조정 필요)
        try {
            val jsonObject = JSONObject(responseMessage)
            val staticAnalysis = jsonObject.getJSONObject("static_analysis")
            val dynamicAnalysis = jsonObject.getJSONObject("dynamic_analysis")

            val hasHttps = staticAnalysis.getBoolean("has_https")
            val urlLength = staticAnalysis.getInt("url_length")
            val dynamicStatus = dynamicAnalysis.getString("status")
            val logAnalysis = dynamicAnalysis.getString("log_analysis")

            // 분석 결과에 따라 메시지 생성
            val isSuspicious = logAnalysis.contains("No Suspicious activity", ignoreCase = true) ||
                    urlLength > 20 || !hasHttps

            val resultMessage = if (isSuspicious) {
                "안전한 문자: URL은 안전하며 로그에서도 악의적인 활동이 발견되지 않았습니다."
            } else {
                "스미싱 의심: URL이 의심스럽거나 로그에서 악의적인 활동이 발견되었습니다."
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
