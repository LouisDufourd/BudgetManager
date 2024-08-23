package com.plaglefleau.budgetdesktop.controller

import com.plaglefleau.budgetdesktop.database.Connexion
import com.plaglefleau.budgetdesktop.database.models.User
import com.plaglefleau.budgetdesktop.managers.DatabaseManager
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.stage.Stage
import java.net.URL
import java.util.*

class LoginController: Initializable {

    lateinit var loginButton: Button
    lateinit var passwordTextField: PasswordField
    lateinit var usernameTextField: TextField
    var primaryStage: Stage? = null

    @FXML
    override fun initialize(url: URL?, resourceBundle: ResourceBundle?) {

        Connexion()

        loginButton.setOnMouseClicked {
            if(usernameTextField.text.isEmpty() || passwordTextField.text.isEmpty()) {
                Alert(Alert.AlertType.ERROR, "Please enter your username and password").showAndWait()
                return@setOnMouseClicked
            }
            if(usernameTextField.text.length > 25) {
                Alert(Alert.AlertType.ERROR, "Your username length can't be inferior to 25").showAndWait()
                return@setOnMouseClicked
            }
            if(DatabaseManager.login(usernameTextField.text, passwordTextField.text)) {
                val result = Alert(Alert.AlertType.CONFIRMATION).apply {
                    title = "Confirmation"
                    headerText = "Are you sure you want to continue?"
                    contentText = "Are you sure that you want to login with this username \"${usernameTextField.text}\" ?"

                }.showAndWait()

                if(!result.isPresent && result.get() != ButtonType.OK) {
                    usernameTextField.text = ""
                    passwordTextField.text = ""
                    return@setOnMouseClicked
                }

                val user = User(usernameTextField.text, passwordTextField.text)

                val fxmlLoader = FXMLLoader()
                fxmlLoader.location = javaClass.getResource("/fxml/main.fxml")
                val root:Parent = fxmlLoader.load()

                primaryStage!!.scene = Scene(root)
                primaryStage!!.show()

                val mainController = fxmlLoader.getController<MainController>()
                mainController.setupData(user, primaryStage!!)

            } else {
                Alert(Alert.AlertType.ERROR, "Your username or password is incorrect").showAndWait()
                usernameTextField.text = ""
                passwordTextField.text = ""
            }
        }
    }
}