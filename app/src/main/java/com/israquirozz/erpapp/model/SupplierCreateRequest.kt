package com.israquirozz.erpapp.model

data class SupplierCreateRequest(
    val name: String,
    val phone: String,
    val email: String,
    val id_address: Int
)