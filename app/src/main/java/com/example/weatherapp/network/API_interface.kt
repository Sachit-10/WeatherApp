package com.example.weatherapp.network

import com.example.weatherapp.Models.weather_dataclass
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface API_interface {

    @GET("data/2.5/weather?")

    fun getweather(

        @Query("lat") lat:Double,
        @Query("lon") lon:Double,
        @Query("units") units:String,
        @Query("appid") appid:String


    ) :Call<weather_dataclass>
}