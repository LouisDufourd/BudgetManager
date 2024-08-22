package com.plaglefleau.budgetdesktop.managers

import com.plaglefleau.budgetdesktop.database.models.DatabaseTransactionModel
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.*

class DatabaseManager {

    private val format = SimpleDateFormat("yyyy-MM-dd")

    /**
     * Retrieves a transaction from the database based on the given date and description.
     *
     * @param date The date of the transaction.
     * @param description The description of the transaction.
     * @return The Transaction object if found in the database, otherwise null.
     */
    fun getTransactions(date: Calendar, description: String): DatabaseTransactionModel? {
        val connection = getConnection()
        val preparedStatement = connection.prepareStatement(
            "SELECT credit, debit FROM transactions WHERE date = ? AND description = ? ORDER BY date DESC"
        )
        preparedStatement.setString(1, date.toString())
        preparedStatement.setString(2, description)
        val rs = preparedStatement.executeQuery()
        val databaseTransactionModel = if (rs.next()) {
            DatabaseTransactionModel(
                date,
                description,
                rs.getDouble("credit"),
                rs.getDouble("debit")
            )
        } else {
            null
        }
        rs.close()
        preparedStatement.close()
        connection.close()
        return databaseTransactionModel;
    }


    /**
     * Retrieves a list of transactions from the database that occurred after the given date.
     *
     * This method connects to the database, executes an SQL query to retrieve transactions with a date greater than or equal to the given date,
     * and converts the retrieved data into a list of Transaction objects.
     *
     * @param date The date to retrieve transactions after.
     * @return The list of transactions that occurred after the given date.
     */
    fun getTransactionsAfter(date: Calendar): List<DatabaseTransactionModel> {
        val databaseTransactionModels = mutableListOf<DatabaseTransactionModel>()
        val connection = getConnection()
        val preparedStatement = connection.prepareStatement(
            "SELECT description, credit, debit, date FROM transactions WHERE date >= ? ORDER BY date DESC"
        )
        preparedStatement.setString(1, calendarToSqlDate(date).toString())
        val rs = preparedStatement.executeQuery()
        while (rs.next()) {
            databaseTransactionModels.add(
                DatabaseTransactionModel(
                    sqlDateToCalendar(rs.getString("date")),
                    rs.getString("description"),
                    rs.getDouble("credit"),
                    rs.getDouble("debit")
                )
            )
        }
        rs.close()
        preparedStatement.close()
        connection.close()
        return databaseTransactionModels;
    }

    /**
     * Retrieves a list of transactions from the database that occurred before the given date.
     *
     * This method connects to the database, executes an SQL query to retrieve transactions with a date less than or equal to the given date,
     * and converts the retrieved data into a list of Transaction objects.
     *
     * @param date The date to retrieve transactions before.
     * @return The list of transactions that occurred before the given date.
     */
    fun getTransactionsBefore(date: Calendar): List<DatabaseTransactionModel> {
        val databaseTransactionModels = mutableListOf<DatabaseTransactionModel>()
        val connection = getConnection()
        val preparedStatement = connection.prepareStatement(
            "SELECT description, credit, debit, date FROM transactions WHERE date <= ? ORDER BY date DESC"
        )
        preparedStatement.setString(1, calendarToSqlDate(date).toString())
        val rs = preparedStatement.executeQuery()
        while (rs.next()) {
            databaseTransactionModels.add(
                DatabaseTransactionModel(
                    sqlDateToCalendar(rs.getString("date")),
                    rs.getString("description"),
                    rs.getDouble("credit"),
                    rs.getDouble("debit")
                )
            )
        }
        rs.close()
        preparedStatement.close()
        connection.close()
        return databaseTransactionModels;
    }

    /**
     * Retrieves a list of transactions from the database.
     *
     * This method retrieves transactions from the database by executing an SQL query to select all rows from the "transactions" table.
     * The retrieved data is then converted into a list of Transaction objects.
     *
     * @return The list of transactions found in the database.
     */
    fun getTransactions() : List<DatabaseTransactionModel> {
        val databaseTransactionModels = mutableListOf<DatabaseTransactionModel>()
        val connection = getConnection()
        val preparedStatement = connection.prepareStatement(
            "SELECT date, description, credit, debit FROM transactions ORDER BY date DESC"
        )
        val rs = preparedStatement.executeQuery()
        while (rs.next()) {
            databaseTransactionModels.add(
                DatabaseTransactionModel(
                    sqlDateToCalendar(rs.getString("date")),
                    rs.getString("description"),
                    rs.getDouble("credit"),
                    rs.getDouble("debit")
                )
            )
        }
        return databaseTransactionModels
    }

    /**
     * Adds a transaction to the database.
     *
     * @param date The date of the transaction.
     * @param description The description of the transaction.
     * @param credit The amount of credit for the transaction. Can be null if there is no credit.
     * @param debit The amount of debit for the transaction. Can be null if there is no debit.
     */
    fun addTransaction(date: Calendar, description: String, credit: Double?, debit: Double?) {
        val connection = getConnection()
        val preparedStatement = connection.prepareStatement(
            "INSERT INTO transactions (id, date, description, credit, debit) " +
                    "VALUES (NULL, ?, ?, ?, ?)"
        )
        preparedStatement.setString(1, format.format(date.time))
        preparedStatement.setString(2, description)
        //SET Credit
        if(credit != null) {
            preparedStatement.setDouble(3, credit)
        } else {
            preparedStatement.setNull(3, java.sql.Types.DOUBLE)
        }
        //SET Debit
        if (debit != null) {
            preparedStatement.setDouble(4, debit)
        } else {
            preparedStatement.setNull(4, java.sql.Types.DOUBLE)
        }

        preparedStatement.executeUpdate()

        preparedStatement.close()
        connection.close()
    }

    /**
     * Uploads a list of transactions to the database.
     *
     * @param databaseTransactionModels The list of transactions to be uploaded.
     *                     Each transaction should have a date, description, debit amount (optional), and credit amount (optional).
     *                     If a transaction with the same date and description already exists in the database,
     *                     it will not be uploaded again.
     *                     The function uses the getTransactions() method to check if a transaction already exists
     *                     and the addTransaction() method to add new transactions to the database.
     */
    fun uploadTransactions(databaseTransactionModels: List<DatabaseTransactionModel>) {
        for (transaction in databaseTransactionModels) {
            if(getTransactions(transaction.date, transaction.description) == null) {
                addTransaction(
                    transaction.date,
                    transaction.description,
                    transaction.credit,
                    transaction.debit
                )
            }
        }
    }

    /**
     * Retrieves a database connection.
     *
     * This method returns a Connection object that represents a connection to the database.
     * The Connection object can be used to execute SQL queries and perform transactions on the database.
     *
     * @return The Connection object for the database.
     */
    private fun getConnection(): java.sql.Connection {
        return com.plaglefleau.budgetdesktop.database.Connexion().getConnection()
    }

    /**
     * Converts a SQL Date object to a Calendar object.
     *
     * @param sqlDate The SQL Date object to convert.
     * @return The converted Calendar object.
     */
    private fun sqlDateToCalendar(sqlDate: String): Calendar {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        val calendar = Calendar.getInstance()
        calendar.time = dateFormat.parse(sqlDate)
        return calendar
    }

    /**
     * Converts a Calendar object to a SQL Date object.
     *
     * This function takes a Calendar object and converts it to a SQL Date object by using the time in milliseconds from
     * the Calendar object. The resulting SQL Date object represents the same date and time as the Calendar object.
     *
     * @param calendar The Calendar object to convert to a SQL Date.
     * @return The converted SQL Date object.
     */
    private fun calendarToSqlDate(calendar: Calendar): Date {
        // Use the time in milliseconds from the Calendar object
        val millis = calendar.timeInMillis
        return Date(millis)
    }
}