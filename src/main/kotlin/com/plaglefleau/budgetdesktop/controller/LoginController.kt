package com.plaglefleau.budgetdesktop.controller

import com.plaglefleau.budgetdesktop.Language
import com.plaglefleau.budgetdesktop.database.models.User
import com.plaglefleau.budgetdesktop.managers.DatabaseManager
import com.plaglefleau.translate.Translation
import javafx.event.ActionEvent
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import java.net.URL
import java.util.*
import java.util.stream.Collectors

class LoginController: Initializable {

    lateinit var loginGrid: GridPane
    lateinit var anchorPane: AnchorPane
    lateinit var usernameLabel: Label
    lateinit var passwordLabel: Label
    lateinit var loginButton: Button
    lateinit var passwordTextField: PasswordField
    lateinit var usernameTextField: TextField
    var primaryStage: Stage? = null


    /**
     * Initializes the LoginController.
     *
     * This method is called after the FXML file has been loaded and the fields have been injected.
     * It sets up the event handler for the loginButton's setOnMouseClicked event.
     * When the loginButton is clicked, it performs the following actions:
     *   - Validates the username and password fields. If either field is empty, an error message is shown.
     *   - Validates the length of the username field. If it exceeds 25 characters, an error message is shown.
     *   - Calls the login method from the DatabaseManager to check if the provided username and password are correct.
     *   - If the login is successful, a confirmation message is shown asking the user to continue.
     *   - If the user cancels the confirmation, the username and password fields are cleared.
     *   - If the user accepts the confirmation, a new User object is created with the provided username and password.
     *   - Loads the main.fxml file using FXMLLoader, sets it as the root of the primaryStage's scene, and shows the primaryStage.
     *   - Gets the controller for the main.fxml file and calls the setupData method to pass the User object and primaryStage to it.
     *   - If the login is unsuccessful, an error message is shown.
     *   - The username and password fields are cleared in both cases.
     *
     * @param url           The location used to resolve relative paths for the root object, or null if the location is not known.
     * @param resourceBundle The resources used to localize the root object, or null if the root object was not localized.
     */
    override fun initialize(url: URL?, resourceBundle: ResourceBundle?) {
        updateLang()

        loginButton.setOnAction { event ->
            loginButtonOnAction(event)
        }
    }

    private fun loginButtonOnAction(event: ActionEvent) {
        val translation = Language.translation

        val result = Alert(Alert.AlertType.CONFIRMATION).apply {
            title = translation.getTraduction(Language.lang, "text.confirmation.title")
            headerText = translation.getTraduction(Language.lang, "text.confirmation.header")
            contentText = translation.getTraduction(Language.lang, "text.confirmation.content").replace("{username}", usernameTextField.text)

        }.showAndWait()

        if (!result.isPresent || result.get() != ButtonType.OK) {
            usernameTextField.text = ""
            passwordTextField.text = ""
            return
        }

        if(usernameTextField.text.isEmpty() || passwordTextField.text.isEmpty()) {
            Alert(Alert.AlertType.ERROR, translation.getTraduction(Language.lang, "text.loginError")).showAndWait()
            return
        }
        if(usernameTextField.text.length > 25) {
            Alert(Alert.AlertType.ERROR, translation.getTraduction(Language.lang, "text.usernameSizeError")).showAndWait()
            return
        }
        if(DatabaseManager.login(usernameTextField.text, passwordTextField.text)) {

            val user = User(usernameTextField.text, passwordTextField.text)

            val fxmlLoader = FXMLLoader()
            fxmlLoader.location = javaClass.getResource("/fxml/main.fxml")
            val root:Parent = fxmlLoader.load()

            primaryStage!!.scene = Scene(root)
            primaryStage!!.show()

            val mainController = fxmlLoader.getController<MainController>()
            mainController.setupData(user, primaryStage!!)

        } else {
            Alert(Alert.AlertType.ERROR, translation.getTraduction(Language.lang, "text.badLoginError")).showAndWait()
            usernameTextField.text = ""
            passwordTextField.text = ""
        }
    }

    private fun updateLang() {
        val translation = Language.translation
        usernameLabel.text = translation.getTraduction(Language.lang, "text.username")
        passwordLabel.text = translation.getTraduction(Language.lang, "text.password")
        loginButton.text = translation.getTraduction(Language.lang, "text.login")
    }
}