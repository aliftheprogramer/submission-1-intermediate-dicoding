package com.example.myapplication.api

import com.example.myapplication.response.AddStoryResponse
import retrofit2.Call
import com.example.myapplication.response.LoginResponse
import com.example.myapplication.response.RegisterResponse
import com.example.myapplication.response.StoryResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class RegisterRequest(
    val email: String,
    val name: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

interface ApiService{
    @POST("register")
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>

    @POST("login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @GET("stories")
    fun getStories(
        @retrofit2.http.Query("page") page: Int,
        @retrofit2.http.Query("size") size: Int? = null,
        @retrofit2.http.Query("location") location: Int = 0,
        @retrofit2.http.Header("Authorization") token: String
    ): Call<StoryResponse>

    @Multipart
    @POST("stories")
    fun addStory(
        @Header("Authorization") token: String,
        @Part("description") description: RequestBody,
        @Part photo: MultipartBody.Part
    ): Call<AddStoryResponse>

}