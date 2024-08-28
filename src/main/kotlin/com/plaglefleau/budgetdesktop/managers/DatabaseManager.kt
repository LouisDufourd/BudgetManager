package com.plaglefleau.budgetdesktop.managers

import com.plaglefleau.budgetdesktop.database.Connexion
import com.plaglefleau.budgetdesktop.database.Migrate
import com.plaglefleau.budgetdesktop.database.models.DatabaseTransactionModel
import com.plaglefleau.budgetdesktop.database.models.User
import java.sql.*
import java.sql.Date
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
                Migrate().migrate(username, password)
                return true
            } else {
                return if(
                    EncryptManager.hmacSha256(
                        username + (username.length + password.length),
                        password
                    ) == user.password) {
                    Migrate().migrate(username, password)
                    true
                } else {
                    false
                }
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
     * Retrieves a list of accounts associated with the provided username.
     *
     * @param username The username for which to retrieve the accounts.
     * @return The list of account names as strings.
     */
    fun getAccounts(username: String) : List<String> {
        val connection = getConnection()
        val preparedStatement = connection.prepareStatement(
            "SELECT account FROM transactions WHERE username = ?"
        )
        preparedStatement.setString(1, encrypt(username))
        val rs = preparedStatement.executeQuery()
        val accounts = mutableListOf<String>()
        while(rs.next()) {
            val account = decrypt(rs.getString("account"))
            if(!accounts.contains(account)) {
                accounts.add(account)
            }
        }

        return accounts
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
            "SELECT id, date, username, description, custom_description, account, credit, debit FROM transactions"
        )
        val rs = preparedStatement.executeQuery()
        val transactions = getTransactionsFromResultSet(rs)
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
            "SELECT id, date, username, description, custom_description, account, credit, debit FROM transactions WHERE username = ?"
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
    fun addTransaction(date: Calendar, username: String, description: String, account: String, credit: Double?, debit: Double?) {
        val connection = getConnection()
        val preparedStatement = connection.prepareStatement(
            "INSERT INTO transactions (id, username, date, description, custom_description, account, credit, debit) " +
                    "VALUES (NULL, ?, ?, ?, ?, ?, ?, ?)"
        )
        preparedStatement.setString(1, encrypt(username))
        preparedStatement.setString(2, encrypt(calendarToSqlDate(date).toString()))
        preparedStatement.setString(3, encrypt(description))
        preparedStatement.setNull(4, Types.VARCHAR)
        preparedStatement.setString(5, encrypt(account))

        // SET Credit
        if (credit != null) {
            preparedStatement.setString(6, encrypt(credit.toString()))
        } else {
            preparedStatement.setNull(6, Types.VARCHAR)
        }

        // SET Debit
        if (debit != null) {
            preparedStatement.setString(7, encrypt(debit.toString()))
        } else {
            preparedStatement.setNull(7, Types.VARCHAR)
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
                    transaction.account ?: "unknown",
                    transaction.credit,
                    transaction.debit
                )
            }
        }
    }

    /**
     * Updates the description of a transaction with the provided ID.
     * This method modifies the 'custom_description' field of the 'transactions' table in the database.
     * The updated description is encrypted using AES encryption with the provided key.
     *
     * @param id The ID of the transaction to update.
     * @param customDescription The new description to set for the transaction.
     */
    fun updateTransactionDescription(id: Int, customDescription: String) {
        val connection = getConnection()
        val preparedStatement = connection.prepareStatement(
            "UPDATE transactions SET custom_description = ? WHERE id = ?"
        )
        preparedStatement.setString(1, encrypt(customDescription))
        preparedStatement.setInt(2, id)
        preparedStatement.executeUpdate()
        preparedStatement.close()
        connection.close()
    }

    /**
     * Retrieves a connection to the*/
    private fun getConnection(): Connection {
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
                    rs.getInt("id"),
                    sqlDateToCalendar(date),
                    decrypt(rs.getString("username")),
                    decrypt(rs.getString("description")),
                    decrypt(rs.getString("custom_description")?:""),
                    account = decrypt(
                        rs.getString("account")?:
                        encrypt("Main account")
                    ),
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
