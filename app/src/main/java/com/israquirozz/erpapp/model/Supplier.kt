package com.israquirozz.erpapp.model

data class Supplier(
    val id_supplier: Int,
    val name: String,
    val phone: String,
    val email: String,
    val active: Boolean,
    val id_address: Int
)