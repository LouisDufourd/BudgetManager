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
                username VARCHAR(255) NOT NULL,
                date VARCHAR(255) NOT NULL,
                description TEXT NOT NULL,
                credit VARCHAR(255),
                debit VARCHAR(255)
            );
            
            CREATE TABLE IF NOT EXISTS users (
                username VARCHAR(25) NOT NULL,
                password VARCHAR(255) NOT NULL
            );
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