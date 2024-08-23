package com.plaglefleau.budgetdesktop.database.models

import java.text.SimpleDateFormat
import java.util.*

data class DatabaseTransactionModel(
    val date: Calendar,
    val username: String,
    val description: String,
    val credit: Double?,
    val debit: Double?
) {

    override fun equals(other: Any?): Boolean {
        val res = (other is DatabaseTransactionModel)
                && (other.date == date)
                && (other.description == description)
                && (other.username == username)
        return res
    }

    override fun toString(): String {
        val simpleDateFormat =  SimpleDateFormat("yyyy-MM-dd")
        return """
            ${simpleDateFormat.format(date.time)} :
                $description 
                $credit 
                $debit
        """.trimIndent()
    }
}
