package com.plaglefleau.budgetdesktop.database.models

import java.util.*

data class DatabaseTransactionModel(
    val date: Calendar,
    val description: String,
    val credit: Double?,
    val debit: Double?
)
