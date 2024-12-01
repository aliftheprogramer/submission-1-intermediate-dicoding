package com.example.myapplication.activity

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.databinding.ActivityLoginBinding
import com.example.myapplication.data.AuthRepository
import com.example.myapplication.response.LoginResponse
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject



@AndroidEntryPoint
class Login : AppCompatActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityLoginBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)

        binding.buttonLogin.setOnClickListener {
            val email = binding.edLoginEmail.text.toString()
            val password = binding.edLoginPassword.text.toString()

            authRepository.login(email, password).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful && response.body()?.error == false) {
                        val loginResult = response.body()?.loginResult ?: return
                        saveUserSession(email, loginResult.name, loginResult.token) // Pass the name
                        val intent = Intent(this@Login, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@Login, "Login failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {

                    Toast.makeText(this@Login, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
        }

        binding.buttonRegister.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }

        playAnimation()
    }

    private fun playAnimation() {
        ObjectAnimator.ofFloat(binding.gambardicoding, View.TRANSLATION_X, -50f, 50f).apply {
            duration = 2000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }.start()
        ObjectAnimator.ofFloat(binding.gambarbangkit, View.TRANSLATION_X, 50f, -50f).apply {
            duration = 2000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }.start()

        ObjectAnimator.ofFloat(binding.textView, View.ROTATION, 0f, 3600f).apply {
            duration = 6000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
        }.start()



        val registerText = ObjectAnimator.ofFloat(binding.edLoginEmail, View.ALPHA, 1f).setDuration(100)
        val  passwordText = ObjectAnimator.ofFloat(binding.edLoginPassword, View.ALPHA, 1f).setDuration(100)
        val  loginButton = ObjectAnimator.ofFloat(binding.buttonLogin, View.ALPHA, 1f).setDuration(100)
        val  registerButton = ObjectAnimator.ofFloat(binding.buttonRegister, View.ALPHA, 1f).setDuration(100)

        val together = AnimatorSet().apply {
            playTogether(registerText, passwordText)
        }

        AnimatorSet().apply {
            playSequentially(loginButton, registerButton, together)
            start()
        }
    }

    private fun saveUserSession(email: String,name: String, token: String) {
        with(sharedPreferences.edit()) {
            putString("email", email)
            putString("name", name) // Save the name
            putString("token", token)
            apply()
        }
        // Tambahkan log untuk memeriksa token yang disimpan
        val savedName = sharedPreferences.getString("name", null)
        val savedToken = sharedPreferences.getString("token", null)
        Log.d("Login", "Saved Token: $savedToken , saved Name: $savedName")
    }



}