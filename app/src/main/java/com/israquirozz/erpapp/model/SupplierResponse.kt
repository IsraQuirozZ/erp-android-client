package com.israquirozz.erpapp.model

data class SupplierResponse(
    val data: List<Supplier>,
    val page: Int,
    val pages: Int,
    val total: Int
)