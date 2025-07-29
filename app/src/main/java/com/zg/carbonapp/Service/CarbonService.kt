
package com.zg.carbonapp.Service

import com.zg.carbonapp.Entity.CarbonFootprint
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface CarbonService {
    @GET("api/carbon/query")
    suspend fun queryCarbonByBarcode(@Query("barcode") barcode: String): Response<CarbonFootprint>

    @GET("api/carbon/search")
    suspend fun searchCarbonByName(@Query("name") name: String): Response<CarbonFootprint>
}