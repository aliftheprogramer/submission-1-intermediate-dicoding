package com.example.myapplication.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.adapter.StoryAdapter
import com.example.myapplication.data.AuthRepository
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.response.ListStoryItem
import com.example.myapplication.response.StoryResponse
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject



@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    private lateinit var  binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
            val token = sharedPreferences.getString("token", null)
            if(token == null){
            // Navigate to login activity
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        } else {
            fetchStories(token)
        }
    }


    private fun setupRecycleView(stories: List<ListStoryItem>) {
        val adapter = StoryAdapter(stories)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            R.id.action_logout -> {
                logout()
                true
            }
            R.id.action_add_story -> {
                val intent = Intent(this, AddStoryActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        with(sharedPreferences.edit()){
            remove("email")
            remove("name")
            remove("token")
            apply()
        }
        val intent = Intent(this, Login::class.java)
        startActivity(intent)
        finish()
    }

    private fun fetchStories(token: String) {
        var page = 1
        val allStories = mutableListOf<ListStoryItem>()

        fun fetchPage() {
            authRepository.getStories(token, page).enqueue(object : Callback<StoryResponse> {
                override fun onResponse(call: Call<StoryResponse>, response: Response<StoryResponse>) {
                    if (response.isSuccessful && response.body()?.error == false) {
                        val stories = response.body()?.listStory ?: emptyList()
                        allStories.addAll(stories)
                        if (stories.isNotEmpty()) {
                            page++
                            fetchPage() // Fetch next page
                        } else {
                            setupRecycleView(allStories) // All stories fetched
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to fetch stories", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<StoryResponse>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            })
        }

        fetchPage() // Start fetching pages
    }


}