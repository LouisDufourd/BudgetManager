package com.plaglefleau.budgetdesktop.managers

import com.plaglefleau.budgetdesktop.database.models.DatabaseTransactionModel
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*

class TransactionManager {

    /**
     * Parses the transactions from a file.
     *
     * @param filePath The path of the file to parse.
     * @return A list of parsed transactions.
     */
    fun parseTransactions(filePath: Path): List<DatabaseTransactionModel> {
        return parseTransactions(filePath.toFile())
    }

    /**
     * Parses the transactions from a file.
     *
     * @param file The file to parse.
     * @return A list of parsed transactions.
     */
    fun parseTransactions(file: File): List<DatabaseTransactionModel> {

        val rawData = file.readText(Charset.forName("Cp1252"))

        return parseTransactions(rawData)
    }

    /**
     * Parses the transactions from a raw data string.
     *
     * @param rawData The raw data string containing transaction information.
     **/
    fun parseTransactions(rawData: String): List<DatabaseTransactionModel> {

        val databaseTransactionModels = mutableListOf<DatabaseTransactionModel>()

        rawData.replace(";;", ";null;")

        val raws = rawData.split(";")

        var i = 4

        try {
            while (i < raws.size) {

                if(raws[i].isEmpty()) {
                    i++
                    continue
                } else {

                    val date = parseDateToCalendar(
                        raws[i].replace("\n", "")
                            .replace("\r", "")
                    )
                    val description = raws[i+1]
                        .replace("\n", "")
                        .replace("\r", "")
                        .replace("\"","")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                    val debit = raws[i+2]
                        .replace("\n", "")
                        .replace("\r", "")
                        .replace(",", ".")
                        .toDoubleOrNull()
                    val credit = raws[i+3]
                        .replace("\n", "")
                        .replace("\r", "")
                        .replace(",", ".")
                        .toDoubleOrNull()
                    /*println("date[${i / 4}] : $date")
                    println("description[${i / 4}] : $description")
                    println("debit[${i / 4}] : $credit")
                    println("credit[${i / 4}] : $debit")
                    println()*/

                    if(date == null) {
                        i++
                        continue
                    }

                    databaseTransactionModels.add(
                        DatabaseTransactionModel(
                            date = date,
                            description = description,
                            debit = debit,
                            credit = credit
                        )
                    )
                    i += 4
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            return databaseTransactionModels
        }

        return databaseTransactionModels
    }

    /**
     * Parses the given date string into a Calendar object.
     *
     * @param dateString The date string to be parsed. It should be in the format "dd/MM/yyyy".
     * @return A Calendar object representing the parsed date, or null if the parsing fails.
     */
    fun parseDateToCalendar(dateString: String): Calendar? {
        // Define the date format pattern for DD/MM/YYYY
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        return try {
            // Parse the date string into a Date object
            val date: Date = dateFormat.parse(dateString) ?: return null

            // Create a Calendar instance and set the date
            val calendar = Calendar.getInstance()
            calendar.time = date

            calendar
        } catch (e: Exception) {
            null
        }
    }
}