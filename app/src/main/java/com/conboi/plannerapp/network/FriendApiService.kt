package com.example.plannerapp.network

import com.example.plannerapp.data.FriendType
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

private const val BASE_URL =
    "https://android-kotlin-fun-mars-server.appspot.com"

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .build()


interface FriendApiService {

    @GET("friends")
    suspend fun getFriendList(): List<FriendType>


}

object FriendApi {
    val retrofitService: FriendApiService by lazy {
        retrofit.create(FriendApiService::class.java)
    }
}




