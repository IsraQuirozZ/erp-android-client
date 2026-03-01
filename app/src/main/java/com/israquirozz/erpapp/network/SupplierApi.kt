package com.israquirozz.erpapp.network

import com.israquirozz.erpapp.model.SupplierResponse
import com.israquirozz.erpapp.model.Supplier
import com.israquirozz.erpapp.model.SupplierCreateRequest
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.DELETE
import retrofit2.http.Query


interface SupplierApi {

    @GET("api/suppliers")
    fun getSuppliers(
        @Query("status") status: String = "all"
    ): Call<SupplierResponse>

    @POST("api/suppliers")
    fun createSupplier(@Body supplier: SupplierCreateRequest): Call<Supplier>

    @PUT("api/suppliers/{id}")
    fun updateSupplier(
        @Path("id") id: Int,
        @Body supplier: SupplierCreateRequest
    ): Call<Supplier>

    @DELETE("api/suppliers/{id}/")
    fun toggleSupplier(
        @Path("id") id: Int
    ): Call<Supplier>
}