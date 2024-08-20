package com.example.hackathon_sms

import android.content.Context
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject

class MalwareReporter(private val context: Context) {

    enum class ReportType(val url: String) {
        POLICE("https://example.com/report/police"),
        FINANCIAL("https://example.com/report/financial"),
        SPAM("https://example.com/report/spam")
    }

    fun reportMalware(reportType: ReportType) {
        val packageName = "com.example.hackathon_sms"
        val details = "악성 앱으로 확인됨"

        val url = reportType.url

        val jsonObject = JSONObject().apply {
            put("packageName", packageName)
            put("details", details)
        }

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            { response ->
                showToast("신고가 완료되었습니다.")
            },
            { error ->
                showToast("신고 처리 중 오류가 발생했습니다.")
            }
        )

        val requestQueue = Volley.newRequestQueue(context)
        requestQueue.add(jsonObjectRequest)
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
