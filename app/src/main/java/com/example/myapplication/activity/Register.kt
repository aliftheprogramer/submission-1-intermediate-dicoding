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
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.api.RegisterRequest
import com.example.myapplication.data.AuthRepository
import com.example.myapplication.databinding.ActivityRegisterBinding
import com.example.myapplication.response.RegisterResponse
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject


@AndroidEntryPoint
class Register : AppCompatActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)

        binding.buttonRegister.setOnClickListener {
            val email = binding.edRegisterEmail.text.toString()
            val name = binding.edRegisterName.text.toString()
            val password = binding.edRegisterPassword.text.toString()

            if (password.length >= 8) {
                val registerRequest = RegisterRequest(email, name, password)
                Log.d("Register", "Register request: $registerRequest")

                authRepository.register(email, name, password).enqueue(object : Callback<RegisterResponse> {
                    override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                        if (response.isSuccessful && response.body()?.error == false) {
                            saveUserSession(email, name, "dummy_token")
                            Toast.makeText(this@Register, "Account created successfully", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@Register, Login::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            val errorMessage = response.body()?.message ?: "Register failed"
                            if (response.code() == 400 || errorMessage.contains("email", ignoreCase = true)) {
                                Toast.makeText(this@Register, "Email is already registered", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.e("Register", "Register failed: ${response.code()} - $errorMessage")
                                Toast.makeText(this@Register, errorMessage, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                        Toast.makeText(this@Register, "Network Error", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(this@Register, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
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
            duration = 2000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
        }.start()

        val registerText = ObjectAnimator.ofFloat(binding.edRegisterEmail, View.ALPHA, 1f).setDuration(100)
        val  nameText = ObjectAnimator.ofFloat(binding.edRegisterName, View.ALPHA, 1f).setDuration(100)
        val  passwordText = ObjectAnimator.ofFloat(binding.edRegisterPassword, View.ALPHA, 1f).setDuration(100)
        val  loginButton = ObjectAnimator.ofFloat(binding.buttonRegister, View.ALPHA, 1f).setDuration(100)
        val  registerButton = ObjectAnimator.ofFloat(binding.buttonRegister, View.ALPHA, 1f).setDuration(100)

        val together = AnimatorSet().apply {
            playTogether(registerText,nameText,  passwordText)
        }

        AnimatorSet().apply {
            playSequentially(loginButton, registerButton, together)
            start()
        }
    }

    private fun saveUserSession(email: String, name: String, token: String) {
        with(sharedPreferences.edit()){
            putString("email", email)
            putString("name", name)
            putString("token", token)
            apply()
        }
    }



}