package com.plaglefleau.budgetdesktop.managers

import com.plaglefleau.budgetdesktop.database.Connexion
import com.plaglefleau.budgetdesktop.database.models.DatabaseTransactionModel
import com.plaglefleau.budgetdesktop.database.models.User
import java.io.File
import java.sql.Connection
import java.sql.Date
import java.sql.DriverManager
import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.util.*

class DatabaseManager(private val username: String, private val password: String) {

    private val format = SimpleDateFormat("yyyy-MM-dd")

    companion object {
        /**
         * Registers a new user in the database with the provided username and password.
         *
         * @param username The username of the user to register.
         * @param password The password of the user to register.
         */
        fun register(username: String, password: String) {
            val connection = getConnection(username, password)
            val preparedStatement = connection.prepareStatement(
                "INSERT INTO users (username, password) VALUES (?, ?)"
            )
            preparedStatement.setString(1, username)
            preparedStatement.setString(
                2,
                EncryptManager.hmacSha256(
                    username + (username.length + password.length),
                    password
                )
            )
            preparedStatement.executeUpdate()
            preparedStatement.close()
            connection.close()
        }

        /**
         * Retrieves a user from the database based on the provided username.
         *
         * @param username The username of the user.
         * @return The User object representing the retrieved user, or null if no user is found.
         */
        private fun getUser(username: String): User? {
            val connection = getConnection()
            val preparedStatement = connection.prepareStatement(
                "SELECT password FROM users WHERE username = ?"
            )
            preparedStatement.setString(1, username)
            val rs = preparedStatement.executeQuery()
            val user = if(rs.next()) {
                User(
                    username = username,
                    password = rs.getString("password")
                )
            } else {
                null
            }

            rs.close()
            preparedStatement.close()
            connection.close()

            return user
        }

        /**
         * Logs in a user with the provided username and password.
         *
         * @param username The username of the user.
         * @param password The password of the user.
         * @return A Boolean value indicating whether the login was successful.
         */
        fun login(username: String, password: String): Boolean {
            val user = getUser(username)

            if(user == null) {
                register(username, password)
                return true
            } else {
                return EncryptManager.hmacSha256(
                    username + (username.length + password.length),
                    password
                ) == user.password
            }
        }

        /**
         * Retrieves a connection to the database.
         *
         * @return The connection to the database.
         */
        private fun getConnection(username: String, password: String): Connection {
            return Connexion(username, password).getConnection()
        }

        private fun getConnection(): Connection {
            return Connexion.getConnection()
        }
    }

    /**
     * Registers a new user in the database with the provided username and password.
     *
     * @param username The username of the user to register.
     * @param password The password of the user to register.
     */
    fun register(username: String, password: String) {
        DatabaseManager.register(username, password)
    }

    /**
     * Logs in a user with the provided username and password.
     *
     * @param username The username of the user.
     * @param password The password of the user.
     * @return A Boolean value indicating whether the login was successful.
     */
    fun login(username: String, password: String): Boolean {
        return DatabaseManager.login(username, password)
    }

    /**
     * Retrieves a transaction from the database based on the provided username, date, and description.
     *
     * @param username The username of the user who made the transaction.
     * @param date The date of the transaction.
     * @param description The description of the transaction.
     * @return The DatabaseTransactionModel object representing the transaction, or null if no transaction is found.
     */
    private fun transactionCount(databaseTransactionModel: DatabaseTransactionModel): Int {
        val transactions = getTransactions(databaseTransactionModel.username)
        return transactions.count {
            it == databaseTransactionModel
        }
    }

    /**
     * Retrieves a list of transactions from the database that fall between the provided dates for a specific user.
     *
     * @param username The username of the user.
     * @param startDate The calendar date representing the upper bound (exclusive) of the date range of the transactions.
     * @param endDate The calendar date representing the lower bound (inclusive) of the date range of the transactions.
     * @return The list of DatabaseTransactionModel objects representing the transactions.
     */
    fun getTransactionsBetween(username: String, startDate: Calendar,
                               endDate: Calendar): List<DatabaseTransactionModel> {
        val transactions = getTransactions(username)
        return transactions.filter { (it.date.after(startDate) || it.date == startDate) && (it.date.before(endDate) && it.date != endDate) }.sortedByDescending { it.date }
    }

    /**
     * Retrieves a list of transactions from the database that occurred after a specific date for a given user.
     *
     * @param username The username of the user.
     * @param date The calendar date representing the starting point (inclusive) for the transactions.
     * @return The list of DatabaseTransactionModel objects representing the transactions that occurred after the specified date for the given user.
     */
    fun getTransactionsAfter(username: String, date: Calendar): List<DatabaseTransactionModel> {
        val transactions = getTransactions(username)
        return transactions.filter { it.date.after(date) || it.date == date }.sortedByDescending { it.date }
    }

    /**
     * Retrieves a list of transactions from the database that occurred before a specific date for a given user.
     *
     * @param username The username of the user.
     * @param date The calendar date representing the upper bound (exclusive) of the date range of the transactions.
     * @return The list of Database*/
    fun getTransactionsBefore(username: String, date: Calendar): List<DatabaseTransactionModel> {
        val transactions = getTransactions(username)
        return transactions.filter { it.date.before(date) || it.date == date }.sortedByDescending { it.date }
    }

    /**
     * Retrieves a list of all transactions from the database.
     *
     * @return The list of [DatabaseTransactionModel] objects representing the transactions.
     */
    fun getTransactions(): List<DatabaseTransactionModel> {
        val connection = getConnection()
        val preparedStatement = connection.prepareStatement(
            "SELECT date, username, description, credit, debit FROM transactions"
        )
        val rs = preparedStatement.executeQuery()
        val transactions = mutableListOf<DatabaseTransactionModel>()
        while (rs.next()) {
            transactions.add(
                DatabaseTransactionModel(
                    sqlDateToCalendar(rs.getString("date")),
                    rs.getString("username"),
                    rs.getString("description"),
                    rs.getString("credit").toDoubleOrNull() ?: 0.0,
                    rs.getString("debit").toDoubleOrNull() ?: 0.0
                )
            )
        }
        rs.close()
        preparedStatement.close()
        connection.close()
        return transactions
    }

    /**
     * Retrieves a list of transactions from the database for the given username.
     *
     * @param username The username for which to retrieve the transactions.
     * @return The list of DatabaseTransactionModel objects representing the transactions.
     */
    fun getTransactions(username: String): List<DatabaseTransactionModel> {
        val connection = getConnection()
        val preparedStatement = connection.prepareStatement(
            "SELECT date, username, description, credit, debit FROM transactions WHERE username = ?"
        )
        preparedStatement.setString(1, encrypt(username))
        val rs = preparedStatement.executeQuery()
        val transactions = getTransactionsFromResultSet(rs)
        rs.close()
        preparedStatement.close()
        connection.close()
        return transactions
    }

    /**
     * Adds a transaction to the database with the provided details.
     *
     * @param date The date of the transaction.
     * @param username The username of the user who made the transaction.
     * @param description The description of the transaction.
     * @param credit The amount credited in the transaction, or null if no credit amount.
     * @param debit The amount debited in the transaction, or null if no debit amount.
     */
    fun addTransaction(date: Calendar, username: String, description: String, credit: Double?, debit: Double?) {
        val connection = getConnection()
        val preparedStatement = connection.prepareStatement(
            "INSERT INTO transactions (id, username, date, description, credit, debit) " +
                    "VALUES (NULL, ?, ?, ?, ?, ?)"
        )
        preparedStatement.setString(1, encrypt(username))
        preparedStatement.setString(2, encrypt(calendarToSqlDate(date).toString()))
        preparedStatement.setString(3, encrypt(description))

        // SET Credit
        if (credit != null) {
            preparedStatement.setString(4, encrypt(credit.toString()))
        } else {
            preparedStatement.setNull(4, java.sql.Types.VARCHAR)
        }

        // SET Debit
        if (debit != null) {
            preparedStatement.setString(5, encrypt(debit.toString()))
        } else {
            preparedStatement.setNull(5, java.sql.Types.VARCHAR)
        }

        preparedStatement.executeUpdate()
        preparedStatement.close()
        connection.close()
    }

    /**
     * Uploads a list of transactions to the database.
     *
     * @param databaseTransactionModels The list of transactions to upload.
     */
    fun uploadTransactions(databaseTransactionModels: List<DatabaseTransactionModel>) {
        for (transaction in databaseTransactionModels) {
            val count1 = transactionCount(transaction)
            val count2 = databaseTransactionModels.count { it == transaction }
            if ( count1 < count2) {
                addTransaction(
                    transaction.date,
                    transaction.username,
                    transaction.description,
                    transaction.credit,
                    transaction.debit
                )
            }
        }
    }

    /**
     * Retrieves a connection to the*/
    private fun getConnection(): java.sql.Connection {
        return DatabaseManager.getConnection()
    }

    /**
     * Converts a SQL date string to a Calendar object.
     *
     * @param sqlDate The SQL date string to*/
    private fun sqlDateToCalendar(sqlDate: String): Calendar {
        val calendar = Calendar.getInstance()
        calendar.time = format.parse(sqlDate)
        return calendar
    }

    /**
     * Converts a Calendar object to a java.sql.Date object.
     *
     * @param calendar The Calendar object to be converted.
     * @return The java.sql.Date object representing the date from the Calendar.
     */
    private fun calendarToSqlDate(calendar: Calendar): Date {
        return Date(calendar.timeInMillis)
    }

    /**
     * Retrieves a list of transactions from the given ResultSet.
     *
     * @param rs The ResultSet containing the transactions.
     * @return The list of DatabaseTransactionModel objects representing the transactions.
     */
    private fun getTransactionsFromResultSet(rs: ResultSet): List<DatabaseTransactionModel> {
        val transactions = mutableListOf<DatabaseTransactionModel>()
        while (rs.next()) {
            val date = decrypt(rs.getString("date"))
            transactions.add(
                DatabaseTransactionModel(
                    sqlDateToCalendar(date),
                    decrypt(rs.getString("username")),
                    decrypt(rs.getString("description")),
                    decrypt(
                        rs.getString("credit")?:
                        Base64.getEncoder().encodeToString(
                            "0.0".toByteArray()
                        )
                    ).toDouble(),
                    decrypt(
                        rs.getString("debit")?:
                        Base64.getEncoder().encodeToString(
                            "0.0".toByteArray()
                        )
                    ).toDouble()
                )
            )
        }
        return transactions
    }

    /**
     * Decrypts the provided data using AES encryption with the given key.
     *
     * @param data The encrypted data to be decrypted.
     * @return The decrypted data as a string. If decryption fails, returns "0.0".
     */
    fun decrypt(data: String): String {
        return EncryptManager.decrypt(
            data,
            EncryptManager.getKeyFromString(password, 16)
        )
    }

    /**
     * Encrypts the provided data using AES encryption with the given key.
     *
     * @param data The data to be encrypted.
     * @return The encrypted data as a string.
     */
    fun encrypt(data: String): String {
        return EncryptManager.encrypt(
            data,
            EncryptManager.getKeyFromString(password, 16)
        )
    }
}
