package com.plaglefleau.budgetdesktop.database

import javafx.scene.control.PasswordField
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import javax.crypto.SecretKey

class Connexion(username: String, password: String) {

    init {
        Migrate().migrate(username, password)

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

        val preparedStatement = connection.prepareStatement("PRAGMA user_version = ${DatabaseVersion.VERSION};")
        preparedStatement.executeUpdate()
    }

    /**
     * Retrieves a connection to the database.
     *
     * @return The connection to the database.
     */
    fun getConnection(): Connection {
        return Connexion.getConnection()
    }

    companion object {
        fun getConnection(): Connection {
            val userHome = System.getProperty("user.home")
            val documentsPath = "${userHome + File.separator}Documents"
            val dbPath = "${documentsPath + File.separator}Budget Manager${File.separator}budget.db"

            File(dbPath).parentFile.mkdirs()

            return DriverManager.getConnection("jdbc:sqlite:$dbPath")
        }
    }
}