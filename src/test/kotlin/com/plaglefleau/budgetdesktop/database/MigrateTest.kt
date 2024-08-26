package com.plaglefleau.budgetdesktop.database

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class MigrateTest {

    @Test
    fun migrate() {
        val migrate = Migrate()
        DatabaseVersion.VERSION = 0
        migrate.migrate("Plag", "Plag0745")
    }
}