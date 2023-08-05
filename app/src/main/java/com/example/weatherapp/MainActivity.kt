package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.webkit.PermissionRequest
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.core.motion.utils.Utils
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.util.Util
import com.example.weatherapp.Models.weather_dataclass
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.network.retrofit_instance
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    var binding: ActivityMainBinding? = null

   private lateinit var fuselocationuser: FusedLocationProviderClient //to retrive latitude and logitude of user

    private var permission_code=1000

    private var sharedpref: SharedPreferences ?=null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        fuselocationuser = LocationServices.getFusedLocationProviderClient(this)

        sharedpref = getSharedPreferences(constants.preferencname, MODE_PRIVATE) //use to store data in this applicatoin

        getCurrentLocation()

    }



// to check is location enabled
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }





    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }





    // Function to request location permissions
    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            permission_code
        )
    }




    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permission_code) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED
            ) {
                getCurrentLocation()
            } else {

                Toast.makeText(this,"Permission not granted",Toast.LENGTH_SHORT).show()
            }
        }
    }






    @SuppressLint("MissingPermission", "SetTextI18n")
    private fun getCurrentLocation() {

        if (checkPermissions()) {
            if (isLocationEnabled()) {

                 val priority = Priority.PRIORITY_HIGH_ACCURACY
                val cancellationTokenSource = CancellationTokenSource()

                fuselocationuser.getCurrentLocation(priority, cancellationTokenSource.token)
                    .addOnSuccessListener { location ->
                        Log.d("Location", "location is found: $location")

                        if(location !=null) {

                            val latitude: Double = location.latitude
                            val longitude: Double = location.longitude

                            getlocationweatherdetails(latitude, longitude)
                        }
                        else {
                            Toast.makeText(this,"location is NULL",Toast.LENGTH_SHORT).show()
                        }


                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this,"Failed on getting current location",Toast.LENGTH_LONG).show()
                    }

            } else {
                Toast.makeText(this, "Please turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }

        } else {
            requestLocationPermissions()
        }
    }







//creating API
    fun getlocationweatherdetails(latitude:Double, longitude:Double) {

        if(constants.isnetworkavailable(this)){
            Toast.makeText(this,"You are connected to the internet",Toast.LENGTH_SHORT).show()

            var progressdialog = ProgressDialog(this)
            progressdialog.setMessage("PLease wait while data is fetching")
            progressdialog.show()

            retrofit_instance.apiinterface.getweather(latitude, longitude,  constants.METRIC_UNIT, constants.APP_ID)
                .enqueue(object : retrofit2.Callback<weather_dataclass?> {
                    override fun onResponse(
                        call: Call<weather_dataclass?>,
                        response: Response<weather_dataclass?>
                    ) {

                        progressdialog.dismiss()

                        if (response.isSuccessful) {

                            val weatherList: weather_dataclass = response.body()!!

                            val weatherresponeJSONstring = Gson().toJson(weatherList) //to  convert weather list to string as shared pref
                                                                                      // only acceps string not objects
                            val editor = sharedpref?.edit()
                            editor?.putString(constants.weather_response_data, weatherresponeJSONstring)// it will copy value of weatherresponeJSONstring
                                                                                                        // to  constants.weather_response_data
                            editor?.apply()


                            setupUI()


                        } else {
                            // If the response is not success
                            val sc = response.code()
                            when (sc) {
                                400 -> {
                                    Toast.makeText(this@MainActivity, "Error 400",Toast.LENGTH_SHORT).show()
                                }

                                404 -> {
                                    Toast.makeText(this@MainActivity, "Error 400",Toast.LENGTH_SHORT).show()
                                }

                                else -> {
                                    Toast.makeText(this@MainActivity, "Error 400",Toast.LENGTH_SHORT).show()

                                }
                            }
                        }
                    }

                    override fun onFailure(call: Call<weather_dataclass?>, t: Throwable) {
                        Toast.makeText(this@MainActivity, t.localizedMessage, Toast.LENGTH_SHORT)
                            .show()

                        progressdialog.dismiss()
                    }


                })
        }
        else {
            Toast.makeText(this,"You are  not connected to the internet",Toast.LENGTH_SHORT).show()
            setupUI()
        }
    }







    private fun setupUI(){

        var weatherresponeJSONString = sharedpref?.getString(constants.weather_response_data,"")

        if(weatherresponeJSONString!!.isNotEmpty()) {
            val weatherlist =
                Gson().fromJson(weatherresponeJSONString, weather_dataclass::class.java)


            for (i in weatherlist.weather.indices) {

                binding?.weatherCondition?.text = weatherlist.weather[i].main
                binding?.temperature?.text = weatherlist.main.temp.toString() + " °C"
                binding?.maxTemperature?.text = weatherlist.main.temp_max.toString()
                binding?.minTemperature?.text = weatherlist.main.temp_min.toString()
                binding?.countryName?.text = weatherlist.sys.country
                binding?.windSpeed?.text = weatherlist.wind.speed.toString() + " km/hr"
                binding?.sunriseTime?.text = converttime(weatherlist.sys.sunrise)
                binding?.sunsetTime?.text = converttime(weatherlist.sys.sunset)
                binding?.humid?.text = weatherlist.main.humidity.toString() + "%"


                when (weatherlist.weather[i].icon) {
                    "01d" -> binding?.ivCondition?.setImageResource(R.drawable.oned)
                    "01n" -> binding?.ivCondition?.setImageResource(R.drawable.onen)
                    "02d" -> binding?.ivCondition?.setImageResource(R.drawable.twod)
                    "02n" -> binding?.ivCondition?.setImageResource(R.drawable.twon)
                    "03d" -> binding?.ivCondition?.setImageResource(R.drawable.threedn)
                    "03n" -> binding?.ivCondition?.setImageResource(R.drawable.threedn)
                    "04d" -> binding?.ivCondition?.setImageResource(R.drawable.fourdn)
                    "04n" -> binding?.ivCondition?.setImageResource(R.drawable.fourdn)
                    "09d" -> binding?.ivCondition?.setImageResource(R.drawable.ninedn)
                    "09n" -> binding?.ivCondition?.setImageResource(R.drawable.ninedn)
                    "10d" -> binding?.ivCondition?.setImageResource(R.drawable.tend)
                    "10n" -> binding?.ivCondition?.setImageResource(R.drawable.tend)
                    "11d" -> binding?.ivCondition?.setImageResource(R.drawable.elevend)
                    "11n" -> binding?.ivCondition?.setImageResource(R.drawable.elevend)
                    "13d" -> binding?.ivCondition?.setImageResource(R.drawable.thirteend)
                    "13n" -> binding?.ivCondition?.setImageResource(R.drawable.thirteend)
                    "50d" -> binding?.ivCondition?.setImageResource(R.drawable.fiftydn)
                    "50n" -> binding?.ivCondition?.setImageResource(R.drawable.fiftydn)


                }

            }
        }
    }




    private fun converttime(time:Long): String{

        val date = Date(time *1000L)
        val sdf  = SimpleDateFormat("HH:mm", Locale.US)

        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)


    }






}

