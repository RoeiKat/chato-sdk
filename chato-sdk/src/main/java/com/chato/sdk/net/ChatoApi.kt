package com.chato.sdk.net

import com.chato.sdk.net.dto.SdkConfigRes
import com.chato.sdk.net.dto.SendMessageReq
import com.chato.sdk.net.dto.SendMessageRes
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ChatoApi {
    @POST("sdk/message")
    suspend fun sendMessage(
        @Header("x-api-key") apiKey: String,
        @Body body: SendMessageReq
    ): SendMessageRes

    @GET("sdk/config")
    suspend fun getConfig(
        @Header("x-api-key") apiKey: String
    ): SdkConfigRes
}
