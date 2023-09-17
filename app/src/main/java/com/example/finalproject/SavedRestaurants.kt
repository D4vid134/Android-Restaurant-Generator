package com.example.finalproject

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.finalproject.databinding.ActivitySavedRestaurantsBinding


class SavedRestaurants : AppCompatActivity() {

    private lateinit var binding: ActivitySavedRestaurantsBinding
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_restaurants)
        binding = ActivitySavedRestaurantsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }
        preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)

        val savedRestaurants = preferences.all

        for ((key, value) in savedRestaurants) {
            val textView = TextView(this)
            textView.text = value as CharSequence?
            textView.setTextColor(Color.BLACK)
            textView.textSize = 20f
            textView.gravity = Gravity.CENTER
            binding.linearLayout.addView(textView)

            val button = Button(this)
            val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 0, 0, 50)
            button.layoutParams = params
            button.text = "More Details"
            button.setBackgroundColor(Color.parseColor("#2B41BD"))
            button.setTextColor(Color.WHITE)
            button.setOnClickListener {
                val intent = Intent(this, Details::class.java)
                intent.putExtra("id", key)
                startActivity(intent)
            }
            binding.linearLayout.addView(button)
        }


    }
}