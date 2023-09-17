package com.example.finalproject

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import com.example.finalproject.databinding.ActivityDetailsBinding
import com.example.finalproject.databinding.ActivityMainBinding
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.OpeningHours
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient

class Details : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        binding = ActivityDetailsBinding.inflate(layoutInflater)

        val view = binding.root
        setContentView(view)

        binding.backButton.setOnClickListener {
            finish()
        }



        Places.initialize(applicationContext, BuildConfig.GoogleApiKey)
        val placesClient: PlacesClient = Places.createClient(this)


        var id = intent.getStringExtra("id")
        var restaurantName = "none"

        if (sharedPreferences.getString(id, null) != null) {
            binding.saveButton.visibility = View.INVISIBLE
        }

        binding.saveButton.setOnClickListener {
            val editor = sharedPreferences.edit()

            editor.putString(id, restaurantName)

            editor.apply()

            binding.saved.text = "Saved!"
            binding.saved.visibility = View.VISIBLE
            binding.saveButton.visibility = View.INVISIBLE

        }

        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.WEBSITE_URI,
            Place.Field.RATING,
            Place.Field.PHONE_NUMBER,
            Place.Field.OPENING_HOURS
        )

        val request = FetchPlaceRequest.newInstance(id, placeFields)

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place

                binding.name.text = place.name
                restaurantName = place.name

                binding.address.text = "Address:\n" + place.address

                if (place.websiteUri != null) {
                    val spannableString = SpannableString(place.websiteUri.toString())
                    val clickableSpan = object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            val intent = Intent(Intent.ACTION_VIEW, place.websiteUri)
                            startActivity(intent)
                        }
                    }

                    spannableString.setSpan(clickableSpan, 0, spannableString.length, 0)
                    binding.website.text = spannableString
                } else {
                    binding.website.text = "No Website"
                }

                binding.website.movementMethod = LinkMovementMethod.getInstance()

                binding.rating.text = "Rating: \n" + place.rating.toString()
                binding.phone.text = "Phone Number: \n" + place.phoneNumber

                val openingHours: OpeningHours? = place.openingHours
                if (openingHours != null) {
                    val weekdayText = openingHours.weekdayText.joinToString("\n")
                    val openingHoursText = "Hours of Operation: \n$weekdayText"
                    binding.openingHours.text = openingHoursText
                } else {
                }
            }.addOnFailureListener { exception ->
                if (exception is ApiException) {
//                    Log.e(TAG, "Place not found: ${exception.message}")
                    val statusCode = exception.statusCode
                    // TODO("Handle error with given status code")
                }
            }

    }
}