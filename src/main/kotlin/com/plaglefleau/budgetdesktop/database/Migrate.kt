package com.plaglefleau.budgetdesktop.database

import com.plaglefleau.budgetdesktop.managers.DatabaseManager
import com.plaglefleau.budgetdesktop.managers.EncryptManager
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.sql.Connection
import java.sql.Types

class Migrate {

    private val userHome: String = System.getProperty("user.home")

    private val origin: Path = Path.of("$userHome\\Documents\\Budget Manager\\budget.db")
    private val backup: Path = Path.of("$userHome\\Documents\\Budget Manager\\budget_backup.db")

    fun migrate(username: String, password: String) {
        if(!origin.toFile().exists()) {
            DatabaseVersion.VERSION = 1
            return
        }

        backup()

        when (DatabaseVersion.VERSION) {
            0 -> {
                val schemas = getDatabaseSchema()
                var whichVersion = false
                for (schema in schemas) {
                    if(schema.key == "transactions" && schema.value.contains("username")) {
                        whichVersion = true
                        break
                    }
                }
                if(!whichVersion) {
                    migrateFromVersion0(username, password)
                } else {
                    migrateFromVersion1(username, password)
                }
            }
            1 -> {
                migrateFromVersion1(username, password)
            }
            2 -> {
                migrateFromVersion2()
            }
        }
    }

    private fun migrateFromVersion0(username: String, password: String) {
        /**
         * ADD A COLUMN username TO THE transactions TABLE AND GIVE IT AS A DEFAULT VALUE THE USERNAME
         */
        val encryptedUsername = encrypt(username, password, username)
        val connection = getConnection()
        var preparedStatement = connection.prepareStatement("ALTER TABLE transactions ADD COLUMN username VARCHAR(255) NOT NULL DEFAULT '$encryptedUsername'")
        preparedStatement.executeUpdate()

        /**
         * CREATE A TABLE USER AND A USER
         */
        println("CREATE TABLE USER")
        preparedStatement = connection.prepareStatement("""
            CREATE TABLE IF NOT EXISTS users (
                username VARCHAR(25) NOT NULL,
                password VARCHAR(255) NOT NULL
            )""")
        preparedStatement.executeUpdate()


        println("CREATE USER")
        val encryptedPassword = EncryptManager.hmacSha256(
            username + (username.length + password.length),
            password
        )
        preparedStatement = connection.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)")
        preparedStatement.setString(1, username)
        preparedStatement.setString(2, encryptedPassword)
        preparedStatement.executeUpdate()

        /**
         * ENCRYPT ALL DATA FROM transactions TABLE
         */
        preparedStatement = connection.prepareStatement("SELECT id, username, date, description, credit, debit FROM transactions")
        val rs = preparedStatement.executeQuery()
        val transactions = mutableListOf<TransactionV0>()
        while (rs.next()) {
            transactions.add(
                TransactionV0(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("date"),
                    rs.getString("description"),
                    rs.getString("credit"),
                    rs.getString("debit")
                )
            )
        }
        for (transaction in transactions) {
            preparedStatement = connection.prepareStatement("UPDATE transactions " +
                    "SET credit = ?, debit = ?, description = ?, date = ? " +
                    "WHERE id = ?")
            if(transaction.credit != null) {
                preparedStatement.setString(1, encrypt(username, password, transaction.credit))
            } else {
                preparedStatement.setNull(1, Types.VARCHAR)
            }

            if(transaction.debit != null) {
                preparedStatement.setString(2, encrypt(username, password, transaction.debit))
            } else {
                preparedStatement.setNull(2, Types.VARCHAR)
            }

            preparedStatement.setString(3, encrypt(username, password, transaction.description))
            preparedStatement.setString(4, encrypt(username, password, transaction.date))
            preparedStatement.setInt(5, transaction.id)
            preparedStatement.executeUpdate()
        }

        preparedStatement.close()
        connection.close()

        migrateFromVersion1(username, password)
    }
    private fun migrateFromVersion1(username: String, password: String) {
        /**
         * CHANGE SCHEMA VERSION TO 1
         */
        DatabaseVersion.VERSION = 1

        println("alter table transactions add column custom_description TEXT NULL")

        val connection = getConnection()
        var preparedStatement = connection.prepareStatement("ALTER TABLE transactions ADD COLUMN custom_description TEXT NULL")
        preparedStatement.execute()

        preparedStatement = connection.prepareStatement("ALTER TABLE transactions ADD COLUMN account VARCHAR(255)")
        preparedStatement.execute()

        preparedStatement = connection.prepareStatement("UPDATE transactions SET account = ? WHERE username = ?")
        preparedStatement.setString(1, encrypt(username, password, "Main Account"))
        preparedStatement.setString(2, encrypt(username, password, username))
        preparedStatement.executeUpdate()

        migrateFromVersion2()
    }
    private fun migrateFromVersion2() {
        DatabaseVersion.VERSION = 2
    }

    private fun backup() {
        Files.copy(origin, backup, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun getDatabaseSchema(): Map<String, String> {
        val connection = getConnection()
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery("SELECT name, sql FROM sqlite_master WHERE type='table'")
        val schema = mutableMapOf<String, String>()

        while (resultSet.next()) {
            val tableName = resultSet.getString("name")
            val createStatement = resultSet.getString("sql")
            schema[tableName] = createStatement
        }

        resultSet.close()
        statement.close()
        connection.close()
        return schema
    }

    private fun encrypt(username: String, password: String, data: String): String {
        return DatabaseManager(username, password).encrypt(data)
    }

    private fun decrypt(username: String, password: String, data: String): String {
        return DatabaseManager(username, password).decrypt(data)
    }

    private class TransactionV0(
        val id:Int,
        val username: String,
        val date: String,
        val description: String,
        val credit: String?,
        val debit: String?,
        )

    private fun getConnection(): Connection {
        return Connexion.getConnection()
    }
}