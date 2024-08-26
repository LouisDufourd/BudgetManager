package com.plaglefleau.budgetdesktop

import com.plaglefleau.budgetdesktop.controller.LoginController
import com.plaglefleau.translate.Translation
import javafx.application.Application
import javafx.application.Application.launch
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
import java.io.File

class MainApplication : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader()
        fxmlLoader.location = javaClass.getResource("/fxml/login.fxml")

        val scene = Scene(fxmlLoader.load())
        stage.title = "Budget Desktop"
        stage.scene = scene
        stage.setOnCloseRequest {}
        stage.isResizable = false
        stage.icons.add(
            Image(javaClass.getResourceAsStream("/images/icon.png"))
        )
        stage.show()


        val controller = fxmlLoader.getController<LoginController>()
        controller.primaryStage = stage
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            try {
                setupEnTranslation()
                setupFrTranslation()

                launch(MainApplication::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}

fun main(args: Array<String>) {
    try {
        setupEnTranslation()
        setupFrTranslation()

        launch(MainApplication::class.java)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun setupEnTranslation() {
    val translation = Language.translation
    val lang = "en"
    translation.setTraduction(lang, "text.username", "Username")
    translation.setTraduction(lang, "text.password", "Password")
    translation.setTraduction(lang, "text.login", "Login")

    translation.setTraduction(lang, "text.confirmation.title", "Confirmation")
    translation.setTraduction(lang, "text.confirmation.header", "Are you sure you want to continue ?")
    translation.setTraduction(lang, "text.confirmation.content", "Are you sure that you want to login with this username \"{username}\" knowing it will create the user if the user don't already exist ?")
    translation.setTraduction(lang, "text.loginError", "Please enter your username and password")
    translation.setTraduction(lang, "text.usernameSizeError", "Your username length can't be superior to 25")
    translation.setTraduction(lang, "text.badLoginError", "Your username or password is incorrect")

    translation.setTraduction(lang, "text.before", "Before")
    translation.setTraduction(lang, "text.after", "After")
    translation.setTraduction(lang, "text.debit", "Debit")
    translation.setTraduction(lang, "text.credit", "Credit")
    translation.setTraduction(lang, "text.remaining", "Remaining")

    translation.setTraduction(lang, "text.file", "File")
    translation.setTraduction(lang, "text.loadFile", "Load a CSV file")
    translation.setTraduction(lang, "text.quit", "Quit")

    translation.setTraduction(lang, "text.edit", "Edit")
    translation.setTraduction(lang, "text.showAll", "Show all")
    translation.setTraduction(lang, "text.onlyCredits" , "Show only credits")
    translation.setTraduction(lang, "text.onlyDebits", "Show only debits")
}

fun setupFrTranslation() {
    val translation = Language.translation
    val lang = "fr"
    translation.setTraduction(lang, "text.username", "Nom d'utilisateur")
    translation.setTraduction(lang, "text.password", "Mots de passe")
    translation.setTraduction(lang, "text.login", "Se connecter")

    translation.setTraduction(lang, "text.confirmation.title", "Confirmation")
    translation.setTraduction(lang, "text.confirmation.header", "Êtes-vous sûr de vouloir continuer ?")
    translation.setTraduction(lang, "text.confirmation.content", "Êtes-vous sûr de vouloir vous connecter avec ce nom d'utilisateur \"{username}\". Sachant que cela créera un utilisateur si ce dernier n'existe pas déjà ?")
    translation.setTraduction(lang, "text.loginError", "Veuillez entrer votre nom d'utilisateur et votre mot de passe")
    translation.setTraduction(lang, "text.usernameSizeError", "La longueur de votre nom d'utilisateur ne peut pas dépasser 25 caractères")
    translation.setTraduction(lang, "text.badLoginError", "Votre nom d'utilisateur ou mot de passe est incorrect")

    translation.setTraduction(lang, "text.before", "Avant")
    translation.setTraduction(lang, "text.after", "Après")
    translation.setTraduction(lang, "text.debit", "Débit")
    translation.setTraduction(lang, "text.credit", "Crédit")
    translation.setTraduction(lang, "text.remaining", "Restant")

    translation.setTraduction(lang, "text.file", "Fichier")
    translation.setTraduction(lang, "text.loadFile", "Charger un fichier CSV")
    translation.setTraduction(lang, "text.quit", "Quitter")

    translation.setTraduction(lang, "text.edit", "Modifier")
    translation.setTraduction(lang, "text.showAll", "Tout afficher")
    translation.setTraduction(lang, "text.onlyCredits" , "Uniquement les crédits")
    translation.setTraduction(lang, "text.onlyDebits", "Uniquement les débits")
}


