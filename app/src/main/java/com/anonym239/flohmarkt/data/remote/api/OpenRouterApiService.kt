package com.anonym239.flohmarkt.data.remote.api

import com.anonym239.flohmarkt.data.remote.dto.OpenRouterRequest
import com.anonym239.flohmarkt.data.remote.dto.OpenRouterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface OpenRouterApiService {

    @POST("chat/completions")
    suspend fun chatCompletion(
        @Body request: OpenRouterRequest
    ): Response<OpenRouterResponse>
}
