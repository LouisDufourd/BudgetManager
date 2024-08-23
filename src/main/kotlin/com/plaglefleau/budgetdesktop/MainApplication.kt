package com.plaglefleau.budgetdesktop

import com.plaglefleau.budgetdesktop.controller.LoginController
import com.plaglefleau.budgetdesktop.database.Connexion
import javafx.application.Application
import javafx.application.Application.launch
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage

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
}

fun main(args: Array<String>) {
    launch(Application::class.java)
}