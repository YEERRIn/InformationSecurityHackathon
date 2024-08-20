package com.example.hackathon_sms

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.GET

interface receiveResult {
    @GET("/")
    fun getResult() : Call<JsonObject>
}