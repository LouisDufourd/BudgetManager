package com.plaglefleau.budgetdesktop.database

import java.sql.Connection
import java.sql.DriverManager

class Connexion {

    init {
        val connection = getConnection()
        val statement = connection.createStatement()

        statement.executeUpdate("""
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date DATE NOT NULL,
                description TEXT NOT NULL,
                credit DOUBLE,
                debit DOUBLE
            )
        """)
    }

    fun getConnection(): Connection {
        return DriverManager.getConnection("jdbc:sqlite:budget.db")
    }
}