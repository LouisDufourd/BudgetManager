package com.plaglefleau.budgetdesktop

import javafx.application.Application
import javafx.application.Application.launch
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage

class MainApplication : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader()
        fxmlLoader.location = javaClass.getResource("/fxml/main.fxml")

        val scene = Scene(fxmlLoader.load())
        stage.title = "Budget Desktop"
        stage.scene = scene
        stage.setOnCloseRequest {}
        stage.minWidth = 850.0
        stage.minHeight = 600.0
        stage.height = 600.0
        stage.icons.add(
            Image(javaClass.getResourceAsStream("/images/icon.png"))
        )
        stage.show()


        val controller = fxmlLoader.getController<MainController>()
        controller.primaryStage = stage
    }
}

fun main(args: Array<String>) {
    launch(Application::class.java)
}