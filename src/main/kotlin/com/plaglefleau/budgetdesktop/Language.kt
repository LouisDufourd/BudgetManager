package com.plaglefleau.budgetdesktop

import com.plaglefleau.translate.FileEditing
import com.plaglefleau.translate.Translation
import java.util.*

object Language {
    val lang: String
        get() {
            val lang = Locale.getDefault().language
            return if (languages.contains(lang)) lang else "en"
        }

    private val langFolder = "${System.getProperty("user.home")}\\Documents\\Budget Manager\\lang"

    val translation = Translation(langFolder)

    val languages: List<String>
        get() {
            val iterator = FileEditing.getFolderFileNames(langFolder, "json").iterator()

            val languages = mutableListOf<String>()

            iterator.forEach {
                languages.add(it)
            }

            return languages
        }
}