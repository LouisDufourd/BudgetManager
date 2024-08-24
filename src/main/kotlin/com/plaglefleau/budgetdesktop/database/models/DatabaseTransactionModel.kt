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

    /**
     * Checks if this object is equal to the specified object.
     *
     * @param other The object to compare for equality.
     * @return true if the objects are equal, false otherwise.
     */
    override fun equals(other: Any?): Boolean {
        val res = (other is DatabaseTransactionModel)
                && (other.date == date)
                && (other.description == description)
                && (other.username == username)
        return res
    }

    /**
     * Returns a string representation of the object.
     * The returned string is formatted as follows:
     * <Date> :
     *     <Description>
     *     <Credit>
     *     <Debit>
     *
     * The date is formatted using the pattern "yyyy-MM-dd".
     *
     * @return a string representation of the object
     */
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
