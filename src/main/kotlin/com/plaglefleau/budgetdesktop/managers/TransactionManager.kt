package com.plaglefleau.budgetdesktop.managers

import com.plaglefleau.budgetdesktop.Language
import com.plaglefleau.budgetdesktop.database.models.DatabaseTransactionModel
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*

class TransactionManager(val username: String, val password:String) {

    fun parseAccountName(rawData: String): String {
        val accountNameRegex = Regex("^(.*?)(carte|n°)")
        val lines = rawData.lines()

        for (line in lines) {
            val matchResult = accountNameRegex.find(line)
            if (matchResult != null) {
                return matchResult.groupValues[1].trim()
            }
        }

        return "Unknown Account" // Fallback if no account name is found
    }

    /**
     * Parses the transactions from a file.
     *
     * @param filePath The path of the file to parse.
     * @return A list of parsed transactions.
     */
    fun parseTransactions(filePath: Path, username: String): List<DatabaseTransactionModel> {
        return parseTransactions(filePath.toFile(), username)
    }

    /**
     * Parses the transactions from a file.
     *
     * @param file The file to parse.
     * @return A list of parsed transactions.
     */
    fun parseTransactions(file: File, username: String): List<DatabaseTransactionModel> {

        val rawData = file.readText(Charset.forName("Cp1252"))

        return parseTransactions(rawData, username)
    }

    /**
     * Parses the transactions from a raw data string.
     *
     * @param rawData The raw data string containing transaction information.
     **/
    fun parseTransactions(rawData: String, username: String): List<DatabaseTransactionModel> {

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

                    if(date == null) {
                        i++
                        continue
                    }

                    databaseTransactionModels.add(
                        DatabaseTransactionModel(
                            id = 0,
                            date = date,
                            username = username,
                            description = description,
                            account = parseAccountName(rawData),
                            customDescription = "",
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

    /**
     * Retrieves a list of transactions based on the selected date criteria.
     *
     * This method retrieves the transactions from the database using the databaseManager.
     * It checks the beforeDatePicker and afterDatePicker values to determine the date range.
     * If both values are null, it retrieves all transactions for the given username.
     * If only the beforeDatePicker value is not null, it retrieves transactions that occurred before the specified date.
     * If only the afterDatePicker value is not null, it retrieves transactions that occurred after the specified date.
     * If both values are not null, it retrieves transactions that fall between the specified dates.
     * The returned list of transactions is of type List<DatabaseTransactionModel>.
     *
     * @return The list of transactions based on the selected date criteria.
     */
    fun getTransactionsBasedOnSelectedDate(afterDate: Calendar?, beforeDate: Calendar?) : List<DatabaseTransactionModel> {
        val databaseManager = DatabaseManager(username, password)
        return if (afterDate == null && beforeDate != null) {
            databaseManager.getTransactionsBefore(
                username,
                beforeDate
            )
        } else if (afterDate != null && beforeDate == null) {
            databaseManager.getTransactionsAfter(
                username,
                afterDate
            )
        } else if (beforeDate != null && afterDate != null){
            databaseManager.getTransactionsBetween(
                username,
                afterDate,
                beforeDate
            )
        } else {
            databaseManager.getTransactions(username)
        }
    }

    /**
     * Retrieves a list of transactions based on the selected date criteria.
     *
     * This method retrieves the transactions from the database using the databaseManager.
     * It checks the beforeDatePicker and afterDatePicker values to determine the date range.
     * If both values are null, it retrieves all transactions for the given username.
     * If only the beforeDatePicker*/
    fun getTransactions(account: String, onlyCredits: Boolean, onlyDebits: Boolean, beforeDate: Calendar?, afterDate: Calendar?): List<DatabaseTransactionModel> {
        val transactions = getTransactionsByAccount(account, beforeDate, afterDate)

        return if(onlyCredits) {
            transactions.filter {
                it.credit != 0.0 && it.debit == 0.0
            }
        } else if (onlyDebits) {
            transactions.filter {
                it.debit != 0.0 && it.credit == 0.0
            }
        } else {
            transactions
        }
    }

    /**
     * Retrieves a list of transactions associated with a specific account, filtered by the given date range.
     *
     * @param account The account identifier for which transactions are to be retrieved.
     * @param beforeDate The upper bound of the date range (inclusive). Transactions must occur before this date.
     * @param afterDate The lower bound of the date range (inclusive). Transactions must occur after this date.
     * @return A list of DatabaseTransactionModel objects representing the filtered transactions.
     */
    fun getTransactionsByAccount(account: String, beforeDate: Calendar?, afterDate: Calendar?): List<DatabaseTransactionModel> {
        val transactions = getTransactionsBasedOnSelectedDate(afterDate, beforeDate)

        return transactions.filter { transaction ->
            transaction.account == account
                    || account == Language.translation
                .getTraduction(
                    Language.lang,
                    "comboBox.all"
                )
        }.sortedByDescending { it.date }
    }
}