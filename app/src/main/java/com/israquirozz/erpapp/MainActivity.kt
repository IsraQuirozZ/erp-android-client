package com.israquirozz.erpapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.israquirozz.erpapp.model.Supplier
import com.israquirozz.erpapp.model.SupplierResponse
import com.israquirozz.erpapp.network.RetrofitClient
import com.israquirozz.erpapp.network.SupplierApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.compose.material3.*
import com.israquirozz.erpapp.model.SupplierCreateRequest
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.israquirozz.erpapp.model.ApiError

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SupplierScreen()
        }
    }
}

fun validateName(name: String): String? {
    return when {
        name.isBlank() -> "Name is required"
        name.length < 3 -> "Name must be at least 3 characters"
        else -> null
    }
}

fun validatePhone(phone: String): String? {
    return when {
        phone.isBlank() -> "Phone is required"
        !phone.all { it.isDigit() } -> "Phone must contain only numbers"
        phone.length < 9 -> "Phone must be at least 9 digits"
        else -> null
    }
}

fun validateEmail(email: String): String? {
    return when {
        email.isBlank() -> "Email is required"
        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
            "Invalid email format"
        else -> null
    }
}

@Composable
fun SupplierScreen() {


    var suppliers by remember { mutableStateOf<List<Supplier>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var supplierToEdit by remember { mutableStateOf<Supplier?>(null) }
    var supplierToToggle by remember { mutableStateOf<Supplier?>(null) }
    val snackbarHostState = remember { SnackbarHostState()}
    val scope = rememberCoroutineScope()

    val api = RetrofitClient.instance.create(SupplierApi::class.java)


    fun loadSuppliers() {
        api.getSuppliers().enqueue(object : Callback<SupplierResponse> {
            override fun onResponse(
                call: Call<SupplierResponse>,
                response: Response<SupplierResponse>
            ) {
                if (response.isSuccessful) {
                    suppliers = response.body()?.data ?: emptyList()
                }
            }

            override fun onFailure(call: Call<SupplierResponse>, t: Throwable) {}
        })
    }

    fun showError(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                withDismissAction = true
            )
        }
    }


    fun parseError(response: Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            val apiError = Gson().fromJson(errorBody, ApiError::class.java)

            apiError.error
                ?: apiError.message
                ?: "Error desconocido"
        } catch (e: Exception) {
            "Error inesperado"
        }
    }

    LaunchedEffect(Unit) {
        loadSuppliers()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true }
            ) {
                Text("+")
            }
        }
    ){ paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            items(suppliers) { supplier ->
                SupplierCard(
                    supplier = supplier,
                    onToggleStatus = { selected ->
                        supplierToToggle = selected
                    },
                    onEdit = { selected ->
                        supplierToEdit = selected
                    }
                )
            }
        }

        supplierToEdit?.let { supplier ->

            EditSupplierDialog(
                supplier = supplier,
                onDismiss = { supplierToEdit = null },
                onUpdate = { updatedData ->

                    api.updateSupplier(supplier.id_supplier, updatedData)
                        .enqueue(object : Callback<Supplier> {

                            override fun onResponse(
                                call: Call<Supplier>,
                                response: Response<Supplier>
                            ) {
                                loadSuppliers()
                                supplierToEdit = null
                            }

                            override fun onFailure(call: Call<Supplier>, t: Throwable) {}
                        })
                }
            )
        }

        supplierToToggle?.let { supplier ->

            ConfirmStatusChangeDialog(
                supplier = supplier,
                onConfirm = {

                    api.toggleSupplier(supplier.id_supplier)
                        .enqueue(object : Callback<Supplier> {

                            override fun onResponse(
                                call: Call<Supplier>,
                                response: Response<Supplier>
                            ) {
                                if (response.isSuccessful) {
                                    loadSuppliers()
                                }
                                supplierToToggle = null
                            }

                            override fun onFailure(call: Call<Supplier>, t: Throwable) {
                                supplierToToggle = null
                            }
                        })
                },
                onDismiss = {
                    supplierToToggle = null
                }
            )
        }


        if (showDialog) {
            CreateSupplierDialog(
                onDismiss = { showDialog = false },
                onCreate = { newSupplier ->
                    api.createSupplier(newSupplier)
                        .enqueue(object : Callback<Supplier> {
                            override fun onResponse(
                                call: Call<Supplier>,
                                response: Response<Supplier>
                            ) {
                                if (response.isSuccessful) {
                                    loadSuppliers()
                                    showDialog = false
                                } else {
                                    val errorMsg = parseError(response)
                                    showError(errorMsg)
                                }
                            }

                            override fun onFailure(call: Call<Supplier>, t: Throwable) {}
                        })
                }
            )
        }
    }
}
@Composable
fun ConfirmStatusChangeDialog(
    supplier: Supplier,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {

    val actionText = if (supplier.active) "desactivar" else "activar"

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        title = { Text("Confirmar cambio") },
        text = {
            Text("¿Seguro que quieres $actionText a ${supplier.name}?")
        }
    )
}

@Composable
fun SupplierCard(
    supplier: Supplier,
    onToggleStatus: (Supplier) -> Unit,
    onEdit: (Supplier) -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column {
                    Text(
                        text = supplier.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text("📧 ${supplier.email}")
                    Text("📞 ${supplier.phone}")
                }

                Switch(
                    checked = supplier.active,
                    onCheckedChange = {
                        onToggleStatus(supplier)
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onEdit(supplier) }
            ) {
                Text("Editar")
            }
        }
    }
}

@Composable
fun CreateSupplierDialog(
    onDismiss: () -> Unit,
    onCreate: (SupplierCreateRequest) -> Unit
) {

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {

                nameError = validateName(name)
                phoneError = validatePhone(phone)
                emailError = validateEmail(email)

                if (nameError == null &&
                    phoneError == null &&
                    emailError == null
                ) {
                    val newSupplier = SupplierCreateRequest(
                        name = name,
                        phone = phone,
                        email = email,
                        id_address = 1
                    )
                    onCreate(newSupplier)
                }

            }) {
                Text("Create")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("New Supplier") },
        text = {
            Column {

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = validateName(it)
                    },
                    label = { Text("Name") },
                    isError = nameError != null,
                    supportingText = {
                        nameError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        phoneError = validatePhone(it)
                    },
                    label = { Text("Phone") },
                    isError = phoneError != null,
                    supportingText = {
                        phoneError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = validateEmail(it)
                    },
                    label = { Text("Email") },
                    isError = emailError != null,
                    supportingText = {
                        emailError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }
        }
    )
}

@Composable
fun EditSupplierDialog(
    supplier: Supplier,
    onDismiss: () -> Unit,
    onUpdate: (SupplierCreateRequest) -> Unit
) {

    var name by remember { mutableStateOf(supplier.name) }
    var phone by remember { mutableStateOf(supplier.phone) }
    var email by remember { mutableStateOf(supplier.email) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {

                nameError = validateName(name)
                phoneError = validatePhone(phone)
                emailError = validateEmail(email)

                if (nameError == null &&
                    phoneError == null &&
                    emailError == null
                ) {
                    val updated = SupplierCreateRequest(
                        name = name,
                        phone = phone,
                        email = email,
                        id_address = supplier.id_address
                    )
                    onUpdate(updated)
                }

            }) {
                Text("Update")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        title = { Text("Editar Proveedor") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = validateName(it)
                    },
                    label = { Text("Name") },
                    isError = nameError != null,
                    supportingText = {
                        nameError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        phoneError = validatePhone(it)
                    },
                    label = { Text("Phone") },
                    isError = phoneError != null,
                    supportingText = {
                        phoneError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = validateEmail(it)
                    },
                    label = { Text("Email") },
                    isError = emailError != null,
                    supportingText = {
                        emailError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }
        }
    )
}
