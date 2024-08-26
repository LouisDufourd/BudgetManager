package com.plaglefleau.budgetdesktop.database

import java.sql.Connection

object DatabaseVersion {
    var VERSION: Int
        get() {
            val connection = getConnection()
            val preparedStatement = connection.prepareStatement("PRAGMA user_version;")
            val resultSet = preparedStatement.executeQuery()
            val version = if (resultSet.next()) {
                resultSet.getInt(1)
            } else {
                0
            }
            resultSet.close()
            preparedStatement.close()
            connection.close()
            return version
        }
        set(version: Int) {
            val connection = getConnection()
            val preparedStatement = connection.prepareStatement("PRAGMA user_version = $version;")
            preparedStatement.execute()
            preparedStatement.close()
            connection.close()
        }

    private fun getConnection(): Connection {
        return Connexion.getConnection()
    }
}