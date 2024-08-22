package com.plaglefleau.budgetdesktop.database

import java.io.File
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
        val userHome = System.getProperty("user.home")
        val documentsPath = "${userHome + File.separator}Documents"
        val dbPath = "${documentsPath + File.separator}Budget Manager${File.separator}budget.db"

        File(dbPath).parentFile.mkdirs()

        return DriverManager.getConnection("jdbc:sqlite:$dbPath")
    }
}