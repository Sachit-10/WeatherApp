package com.example.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object constants {

    const val METRIC_UNIT:String = "metric"
    const val APP_ID:String ="4a9a2ddf2ae3eb0c9a28776dbac87f26"
    const val preferencname = "WeatherPreferenceName"
    const val weather_response_data = "Weather Response Data"

    fun isnetworkavailable(context: Context):Boolean {

        var connectingmanager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager


            val network = connectingmanager.activeNetwork ?: return false
            val activenetwork = connectingmanager.getNetworkCapabilities(network) ?:return false


        return  when {
            activenetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            activenetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activenetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true

            else -> false
        }





    }
}