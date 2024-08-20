package com.example.hackathon_sms

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class sendGetRequest {

    //콜백 인터페이스 정의
    interface ResponseCallback {
        fun onSuccess(message: String)
        fun onFailure(errorMessage: String)
    }

    //컨테이너 서버에서 받은 결과 관련 변수
    private lateinit var mRetrofit: Retrofit
    lateinit var mRetrofitAPI: receiveResult //정의한 코틀린 인터페이스
    private lateinit var mCallResult : retrofit2.Call<JsonObject>

    //레트로핏 세팅 함수 - Retrofit 클라이언트 생성
    fun setRetrofit(){
        mRetrofit = Retrofit
            .Builder()
            .baseUrl("http://10.0.2.2:8080") //localhost 쓰면 오류남 ^_^
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        mRetrofitAPI = mRetrofit.create(receiveResult::class.java)
    }


    //Retrofit API와 콜백 연결
    fun callAppAnalysis(callback: ResponseCallback){
        mCallResult = mRetrofitAPI.getResult()
        mCallResult.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                val analysisResult = response.body()

                var gson = Gson()
                val dataAnalsysResult = gson.fromJson(analysisResult, DTO.AppResult::class.java)
                val analysis_re = dataAnalsysResult.toString()
                Log.d("msg", "${analysis_re}")
                callback.onSuccess(analysis_re)
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                t.printStackTrace()
                Log.d("test", "에러입니다. ${t.message}")
            }

        })

    }


}