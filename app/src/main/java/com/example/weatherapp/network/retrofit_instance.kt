package com.example.weatherapp.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

object retrofit_instance {

   val retrofit by lazy {

       Retrofit.Builder().baseUrl("https://api.openweathermap.org/").addConverterFactory(GsonConverterFactory.create())
           .build()
   }

    val apiinterface by lazy {
        retrofit.create(API_interface::class.java)
    }
}