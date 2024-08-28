package com.plaglefleau.budgetdesktop.database.models

import java.text.SimpleDateFormat
import java.util.*

data class DatabaseTransactionModel(
    val id: Int,
    val date: Calendar,
    val username: String,
    val description: String,
    var customDescription: String?,
    val account: String?,
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

    override fun hashCode(): Int {
        var result = date.hashCode()
        result = 31 * result + username.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + (credit?.hashCode() ?: 0)
        result = 31 * result + (debit?.hashCode() ?: 0)
        result = 31 * result + (customDescription?.hashCode() ?: 0)
        result = 31 * result + (account?.hashCode() ?: 0)
        return result
    }
}
