package com.example.myapplication.data

import com.example.myapplication.api.ApiService
import com.example.myapplication.api.LoginRequest
import com.example.myapplication.api.RegisterRequest
import com.example.myapplication.response.AddStoryResponse
import com.example.myapplication.response.LoginResponse
import com.example.myapplication.response.RegisterResponse
import com.example.myapplication.response.StoryResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import javax.inject.Inject

class AuthRepository @Inject constructor(private val apiService: ApiService) {
    fun login(email: String, password: String): Call<LoginResponse> {
        return apiService.login(LoginRequest(email, password))
    }

    fun register(email: String, name: String, password: String): Call<RegisterResponse> {
        return apiService.register(RegisterRequest(email, name, password))
    }

    fun getStories(token: String, page: Int, size: Int? = null, location: Int = 0): Call<StoryResponse>{
        return apiService.getStories(page,size, location, "Bearer $token")
    }

    fun addStory(token: String, description: RequestBody, photo: MultipartBody.Part): Call<AddStoryResponse> {
        return apiService.addStory("Bearer $token", description, photo)
    }
}