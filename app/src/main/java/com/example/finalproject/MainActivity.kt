package com.example.finalproject

import android.content.pm.PackageManager
import android.location.LocationRequest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.finalproject.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.Manifest
import android.content.Intent
import android.location.Location
import android.nfc.Tag
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.Console
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var currentLatitude: Number
    lateinit var currentLongitude: Number
    lateinit var restaurant: Restaurant


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        if (hasLocationPermission()) {
            findLocation()
        }

        binding.minRatingInput.setOnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                val inputValue = binding.minRatingInput.text.toString().toFloatOrNull()
                if (inputValue == null || inputValue < 0 || inputValue > 5) {
                    binding.minRatingInput.error = "Please enter a number between 0 and 5"
                }
            }
        }

        binding.findRestaurantButton.setOnClickListener{
            if(binding.minRatingInput.text?.length == 0 || binding.maxDistanceInput.text?.length == 0) {
                binding.restaurantName.text = "One of the fields is empty"
            } else if (binding.minRatingInput.text.toString().toDouble() > 5 || binding.minRatingInput.text.toString().toDouble() < 0 || binding.maxDistanceInput.text.toString().toDouble() < 0) {
                binding.restaurantName.text = "Please double check your input field"
            } else {
                val minRating = binding.minRatingInput.text.toString().trim().toDouble()
                val restaurantType = binding.restaurantTypeInput.text.toString().trim()
                val maxDistance = binding.maxDistanceInput.text.toString().trim()

                CoroutineScope(Dispatchers.IO).launch {
                    val jsonUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?keyword=$restaurantType&location=$currentLatitude%2C${currentLongitude}&radius=${maxDistance.toDouble() * 1609.34f}&type=restaurant&key=${BuildConfig.GoogleApiKey}"
                    val jsonData = fetchJsonData(jsonUrl)

                    if (jsonData != null) {
                        do {
                            try {
                                restaurant = extractRandomRestaurant(jsonData)
                            }
                            catch (e:Exception) {
                                binding.restaurantName.text = "No Results. Please Increase Distance"
                            }
                        } while (restaurant.rating < minRating)
                        withContext(Dispatchers.Main) {
                            if(restaurant.id == "1") {
                                binding.restaurantName.text = restaurant.name
                                binding.restaurantName.visibility = View.VISIBLE
                            }
                            else {
                                binding.restaurantName.text = restaurant.name
                                binding.restaurantName.visibility = View.VISIBLE

                                binding.restaurantRating.text =
                                    "Rating: ${restaurant.rating}"
                                binding.restaurantRating.visibility = View.VISIBLE

                                val distance = distanceTo(restaurant)
                                binding.restaurantDistance.text = "Distance: $distance miles"
                                binding.restaurantDistance.visibility = View.VISIBLE

                                binding.restaurantOpenStatus.text =
                                    if (restaurant.openNow) "Open Now" else "Closed"
                                binding.restaurantOpenStatus.visibility = View.VISIBLE

                                binding.moreDetailsButton.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }

        }

        binding.moreDetailsButton.setOnClickListener {
            val intent = Intent(this, Details::class.java)
            intent.putExtra("id", restaurant.id)
            startActivity(intent)
        }

        binding.savedRestaurants.setOnClickListener {
            val intent = Intent(this, SavedRestaurants::class.java)
            startActivity(intent)
        }
    }

    fun distanceTo(restaurant: Restaurant): Float {
        val loc1 = Location("").apply {
            latitude = restaurant.lat
            longitude = restaurant.lng
        }
        val loc2 = Location("").apply {
            latitude = currentLatitude.toDouble()
            longitude = currentLongitude.toDouble()
        }

        Log.d("latitude", currentLatitude.toString())
        Log.d("restaurant.lat", restaurant.lat.toString())

        return (loc1.distanceTo(loc2))/1609.34f
    }

    fun extractRandomRestaurant(jsonData: String): Restaurant{
        val jsonObject = JSONObject(jsonData)
        val resultsArray = jsonObject.getJSONArray("results")
        if(resultsArray.length() == 0) {
            return Restaurant("NO RESULTS", 5.0, false, 0.0, 0.0, "1")
        }

        val randomIndex = Random.nextInt(resultsArray.length())
        val randomResult = resultsArray.getJSONObject(randomIndex)

        val name = randomResult.getString("name")
        val rating = randomResult.getDouble("rating")
        val openNow = randomResult.getJSONObject("opening_hours").getBoolean("open_now")
        val location = randomResult.getJSONObject("geometry").getJSONObject("location")
        val lat = location.getDouble("lat")
        val lng = location.getDouble("lng")
        val id = randomResult.getString("place_id")

        return Restaurant(name, rating, openNow, lat, lng, id)
    }

    private fun fetchJsonData(url: String): String? {
        var result: String? = null
        try {
            val urlConnection = URL(url).openConnection() as HttpURLConnection
            urlConnection.requestMethod = "GET"
            urlConnection.connect()

            if (urlConnection.responseCode == HttpURLConnection.HTTP_OK) {
                val inputStreamReader = InputStreamReader(urlConnection.inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                val stringBuilder = StringBuilder()
                bufferedReader.forEachLine { stringBuilder.append(it) }
                result = stringBuilder.toString()
            }

            urlConnection.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    private fun hasLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_DENIED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return false
        }
        return true
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                findLocation()
            }
        }

    private fun findLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val client = LocationServices.getFusedLocationProviderClient(this)
        client.lastLocation.addOnSuccessListener(this) { location: Location? ->
            location?.let {
                currentLatitude = it.latitude
                currentLongitude = it.longitude
            }
        }
    }


}