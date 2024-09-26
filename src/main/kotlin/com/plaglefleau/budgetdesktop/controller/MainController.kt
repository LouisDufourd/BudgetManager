package com.plaglefleau.budgetdesktop.controller

import com.plaglefleau.budgetdesktop.Language
import com.plaglefleau.budgetdesktop.database.models.DatabaseTransactionModel
import com.plaglefleau.budgetdesktop.database.models.User
import com.plaglefleau.budgetdesktop.managers.DatabaseManager
import com.plaglefleau.budgetdesktop.managers.TransactionManager
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.util.Callback
import java.io.File
import java.nio.file.Paths
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import kotlin.math.log
import kotlin.reflect.full.memberProperties
import kotlin.system.exitProcess

class MainController {

    lateinit var editMenu: Menu
    lateinit var fileMenu: Menu

    lateinit var remainingLabel: Label
    lateinit var creditLabel: Label
    lateinit var debitLabel: Label
    lateinit var afterLabel: Label
    lateinit var beforeLabel: Label

    lateinit var clearData: MenuItem
    lateinit var openChartsWindow: MenuItem
    lateinit var loadFileButton: MenuItem
    lateinit var quitButton: MenuItem

    lateinit var showAll: RadioMenuItem
    lateinit var onlyDebits: RadioMenuItem
    lateinit var onlyCredits: RadioMenuItem

    lateinit var mainVBox: VBox
    lateinit var databaseTransactionModelTableView: TableView<DatabaseTransactionModel>
    lateinit var accountFilterComboBox: ComboBox<String>
    private lateinit var toggleGroup: ToggleGroup
    lateinit var beforeDatePicker: DatePicker
    lateinit var afterDatePicker: DatePicker
    lateinit var fluctuationTextField: TextField
    lateinit var totalCreditTextField: TextField
    lateinit var totalDebitTextField: TextField

    private var databaseManager: DatabaseManager = DatabaseManager("", "")
    private var transactionManager = TransactionManager("", "")
    private val translation = Language.translation

    private var login: User = User("","")
    private var primaryStage: Stage? = null

    /**
     * Sets up the data for the application.
     *
     * @param login The user's login information.
     * @param primaryStage The main stage of the application.
     */
    fun setupData(login: User, primaryStage: Stage) {
        this.login = login

        databaseManager = DatabaseManager(login.username, login.password)
        transactionManager = TransactionManager(login.username, login.password)

        setAccountFilterComboBox()

        updateLanguage()

        this.primaryStage = primaryStage

        primaryStage.minWidth = mainVBox.width
        primaryStage.minHeight = mainVBox.height
        primaryStage.isResizable = true
        setColumns(databaseTransactionModelTableView, DatabaseTransactionModel::class)
        databaseTransactionModelTableView.items = getFilteredList()
        setTotalCreditDebitAndFluctuation()

        toggleGroup = ToggleGroup()
        toggleGroup.toggles.setAll(showAll, onlyDebits, onlyCredits)
        showAll.isSelected = true

        setupListeners()
    }

    /**
     * Sets up the listeners for the UI components.
     *
     * - The selected toggle property of the toggle group is listened, and when it changes, the filtered list of transactions is updated in the databaseTransactionModelTableView.
     * - The loadFileButton's setOnAction event is listened, and when triggered, a file chooser dialog is opened to choose a CSV file. If a file is chosen, the transactions are parsed
     *  from the file and uploaded to the database. The filtered list of transactions is then updated in the databaseTransactionModelTableView, and the total credit, debit, and fluct
     * uation values are updated.
     * - The quitButton's setOnAction event is listened, and when triggered, the application is exited.
     * - The beforeDatePicker's setOnAction event is listened, and when triggered, the filtered list of transactions is updated in the databaseTransactionModelTableView, and the total
     *  credit, debit, and fluctuation values are updated.
     * - The afterDatePicker's setOnAction event is listened, and when triggered, the filtered list of transactions is updated in the databaseTransactionModelTableView, and the total
     *  credit, debit, and fluctuation values are updated.
     */
    private fun setupListeners() {
        toggleGroup.selectedToggleProperty().addListener { _, _, _ ->
            databaseTransactionModelTableView.items = getFilteredList()
        }

        loadFileButton.setOnAction {
            val fileChooser = FileChooser()
            fileChooser.title = "Choose a CSV file"
            fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("CSV Files", "*.csv"))
            fileChooser.initialDirectory = getDownloadsFolder()
            val chosenFile = fileChooser.showOpenDialog(primaryStage)

            if (chosenFile != null) {
                val transactions = transactionManager.parseTransactions(chosenFile, login.username)
                databaseManager.uploadTransactions(transactions)
                databaseTransactionModelTableView.items = getFilteredList()
                setTotalCreditDebitAndFluctuation()
                setAccountFilterComboBox()

            }
        }

        quitButton.setOnAction {
            exitProcess(0)
        }

        beforeDatePicker.setOnAction {
            databaseTransactionModelTableView.items = getFilteredList()
            setTotalCreditDebitAndFluctuation()
        }

        afterDatePicker.setOnAction {
            databaseTransactionModelTableView.items = getFilteredList()
            setTotalCreditDebitAndFluctuation()
        }

        accountFilterComboBox.setOnAction {
            databaseTransactionModelTableView.items = getFilteredList()
            setTotalCreditDebitAndFluctuation()
        }

        openChartsWindow.setOnAction {
            val fxmlLoader = FXMLLoader(javaClass.getResource("/fxml/charts.fxml"))

            val scene = Scene(fxmlLoader.load())
            val stage = Stage()


            stage.title = "Charts"
            stage.scene = scene
            stage.show()

            val controller = fxmlLoader.getController<ChartsController>()
            controller.setValue(login.username, login.password)
        }

        clearData.setOnAction {
            val result = Alert(Alert.AlertType.CONFIRMATION, translation.getTraduction(Language.lang, "text.alert.clearData.confirmation")).showAndWait()
            if(result.isPresent && result.get() == ButtonType.OK) {
                databaseManager.clearForUser(login.username)
                databaseTransactionModelTableView.items = getFilteredList()
                setTotalCreditDebitAndFluctuation()
                setAccountFilterComboBox()
            }
        }
    }

    /**
     * Retrieves the filtered list of transactions to display in the UI.
     *
     * This method retrieves the list of transactions using the getTransactions() method. It then filters the list based on the selected toggle in the toggle group. If onlyCredits
     *  is selected, it filters the transactions to include only those with a non-zero credit and zero debit. If onlyDebits is selected, it filters the transactions to include only
     *  those with a non-zero debit and zero credit. Otherwise, it returns the original list of transactions. The filtered list is then wrapped in an ObservableList using the FXCollections
     * .observableArrayList() method and returned.
     *
     * @return The filtered list of transactions as an ObservableList.
     */
    private fun getFilteredList(): ObservableList<DatabaseTransactionModel> {
        return FXCollections.observableArrayList(transactionManager.getTransactions(
            accountFilterComboBox.value ?: translation.getTraduction(Language.lang, "comboBox.all"),
            onlyCredits.isSelected,
            onlyDebits.isSelected,
            calendarFromLocalDate(beforeDatePicker.value),
            calendarFromLocalDate(afterDatePicker.value)
        ).map { transaction ->
            if(transaction.customDescription!!.isNotEmpty()) {
                transaction.copy(description = transaction.customDescription!!)
            } else {
                transaction
            }
        })
    }

    /**
     * Retrieves the Downloads folder for the current user.
     *
     * @return The File object representing the Downloads folder, or null if it does not exist or is not a directory.
     */
    private fun getDownloadsFolder(): File? {
        // Determine the user's Downloads directory
        val userHome = System.getProperty("user.home")
        val downloadsFolderPath = Paths.get(userHome, "Downloads").toString()
        val downloadsFolder = File(downloadsFolderPath)

        return if (downloadsFolder.exists() && downloadsFolder.isDirectory) {
            downloadsFolder
        } else {
            null
        }
    }

    /**
     * Converts a LocalDate object to a Calendar object.
     *
     * @param localeDate The LocalDate object to convert.
     * @return The converted Calendar object.
     */
    private fun calendarFromLocalDate(localeDate: LocalDate?): Calendar? {
        if(localeDate == null) {
            return null
        }

        val calendar = Calendar.getInstance()
        calendar.time = Date.from(localeDate.atStartOfDay(calendar.timeZone.toZoneId()).toInstant())
        return calendar
    }

    /**
     * Calculates and sets the total credit, debit, and fluctuation values based on the transactions retrieved
     * from the getTransactionsBasedOnSelectedDate() method. The total credit and debit values are calculated
     * by summing the credit and debit amounts of each transaction, respectively. The fluctuation value is
     * calculated by subtracting the total debit from the total credit. The decimal format "#0.00€" is used to
     * format the values before setting them in the corresponding text fields.
     */
    private fun setTotalCreditDebitAndFluctuation() {
        val decimalFormat = DecimalFormat("#0.00€")

        var totalDebit = 0.0
        var totalCredit = 0.0

        val transactions = transactionManager.getTransactions(
            accountFilterComboBox.value ?: translation.getTraduction(Language.lang, "comboBox.all"),
            onlyCredits.isSelected,
            onlyDebits.isSelected,
            calendarFromLocalDate(beforeDatePicker.value),
            calendarFromLocalDate(afterDatePicker.value)
        )

        transactions.forEach {
            totalDebit += it.debit ?: 0.0
            totalCredit += it.credit ?: 0.0
        }

        totalDebitTextField.text = decimalFormat.format(totalDebit)
        totalCreditTextField.text = decimalFormat.format(totalCredit)
        fluctuationTextField.text = decimalFormat.format(totalCredit - totalDebit)
    }

    /**
     * Clears the existing columns in the TableView and sets up new columns based on the given class type.
     *
     * @param tableView The TableView on which to set up the columns.
     * @param kClass The Kotlin class representing the type of the TableView items.
     * @param <T> The type of the TableView items.
     */
    private fun <T : Any> setColumns(tableView: TableView<T>, kClass: kotlin.reflect.KClass<T>) {
        tableView.columns.clear()
        kClass.memberProperties.forEach { prop ->
            val column = TableColumn<T, String>(prop.name.replace(Regex("([A-Z])"), " $1").uppercase())

            // Custom cell value factory for Calendar
            when (prop.returnType.classifier) {
                Calendar::class -> {
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy")

                    column.cellValueFactory = Callback { cellData ->
                        val calendar = prop.get(cellData.value) as? Calendar
                        SimpleStringProperty(calendar?.let { dateFormat.format(it.time) } ?: "")
                    }

                    column.comparator = Comparator { string1, string2 ->
                        val date1 = dateFormat.parse(string1)
                        val date2 = dateFormat.parse(string2)

                        if(date1.before(date2)) -1 else if (date1.after(date2)) 1 else 0
                    }

                    tableView.columns.addFirst(column)
                }
                Double::class -> {
                    val df = DecimalFormat("#0.00€")
                    column.cellValueFactory = Callback { cellData ->
                        val value = prop.get(cellData.value) as? Double
                        SimpleStringProperty(df.format(value))
                    }
                    tableView.columns.add(column)
                }
                String::class -> {

                    if (prop.name != "username" && prop.name != "customDescription") {
                        column.cellValueFactory = Callback { cellData ->
                            val value = prop.get(cellData.value) as? String
                            SimpleStringProperty(value!!.replace("\n", "").replace("\r", "").replace("\"", "").replace(Regex("\\s+"), " ").trim())
                        }

                        if (prop.name == "description") {
                            // Set a custom cell factory to detect right-clicks on "description" column
                            column.setCellFactory {
                                object : TableCell<T, String>() {
                                    init {
                                        // Add a mouse click event listener to the cell
                                        this.addEventFilter(MouseEvent.MOUSE_CLICKED) { event ->
                                            if (event.button == MouseButton.SECONDARY) { // Right-click detection
                                                val cellValue = item // Get the value of the cell
                                                val rowValue = tableRow.item // Get the value of the entire row

                                                println("Right-click detected on description column")
                                                println("Cell value: $cellValue")
                                                if(rowValue != null && rowValue is DatabaseTransactionModel) {
                                                    val contextMenu = ContextMenu()
                                                    val changeDescriptionItem = MenuItem(translation.getTraduction(Language.lang, "text.menu.changeDescription"))
                                                    changeDescriptionItem.setOnAction {
                                                        val dialog = TextInputDialog(cellValue)
                                                        dialog.title = translation.getTraduction(Language.lang, "text.menu.changeDescription")
                                                        dialog.contentText = translation.getTraduction(Language.lang, "text.menu.changeDescription.description")

                                                        val result = dialog.showAndWait()
                                                        result.ifPresent { newDescription ->
                                                            // Update the description in the model
                                                            (rowValue as? DatabaseTransactionModel)?.let {
                                                                it.customDescription = newDescription
                                                                // Optionally update the TableView or other UI components

                                                                databaseManager.updateTransactionDescription(it.id, it.customDescription!!)

                                                                databaseTransactionModelTableView.items = getFilteredList()
                                                                setTotalCreditDebitAndFluctuation()
                                                            }
                                                        }
                                                    }
                                                    contextMenu.items.add(changeDescriptionItem)
                                                    contextMenu.show(this, event.screenX, event.screenY)
                                                }

                                                // Handle your right-click event here
                                                event.consume() // Stop further processing if needed
                                            }
                                        }
                                    }

                                    override fun updateItem(item: String?, empty: Boolean) {
                                        super.updateItem(item, empty)
                                        text = if (empty) null else item
                                    }
                                }
                            }
                        }

                        tableView.columns.add(column)
                    }
                }
                Int::class -> {}
                else -> {
                    column.cellValueFactory = PropertyValueFactory(prop.name)
                    tableView.columns.add(column)
                }
            }

        }
    }

    /**
     * Updates the language of the UI components.
     *
     * This function sets the text of various UI components in the application to display the translated strings from the translation object. It retrieves the translated strings using
     *  the `getTraduction` method of the `translation` object, passing in the current language (`Language.lang`) and the corresponding translation keys for each UI component.
     *
     * The UI components that are updated include:
     * - `beforeLabel`, `afterLabel`, `debitLabel`, `creditLabel`, and `remainingLabel` labels, which display translated text related to financial transactions.
     * - `fileMenu` menu item, `loadFileButton` button, and `quitButton` button, which display translated text related to file operations.
     * - `editMenu` menu item, `showAll`, `onlyCredits`, and `onlyDebits` radio buttons, which display translated text related to filtering options.
     *
     * This function does not take any parameters and does not return any values.
     */
    private fun updateLanguage() {
        fileMenu.text = translation.getTraduction(Language.lang, "text.file")
        editMenu.text = translation.getTraduction(Language.lang, "text.edit")

        beforeLabel.text = translation.getTraduction(Language.lang, "datePicker.before")
        afterLabel.text = translation.getTraduction(Language.lang, "datePicker.after")
        debitLabel.text = translation.getTraduction(Language.lang, "text.debit")
        creditLabel.text = translation.getTraduction(Language.lang, "text.credit")
        remainingLabel.text = translation.getTraduction(Language.lang, "text.remaining")


        loadFileButton.text = translation.getTraduction(Language.lang, "text.loadFile")
        quitButton.text = translation.getTraduction(Language.lang, "text.quit")
        openChartsWindow.text = translation.getTraduction(Language.lang, "text.openChartsWindow")
        clearData.text = translation.getTraduction(Language.lang, "text.clearData")

        showAll.text = translation.getTraduction(Language.lang, "text.showAll")
        onlyCredits.text = translation.getTraduction(Language.lang, "text.onlyCredits")
        onlyDebits.text = translation.getTraduction(Language.lang, "text.onlyDebits")

    }

    /**
     * Prompts the user to input an account name using a text dialog.
     *
     * @return An Optional containing the account name if the user provides one, or an empty Optional if the user cancels the dialog.
     */
    private fun askAccount(): Optional<String> {
        val dialog = TextInputDialog("")
        dialog.title = translation.getTraduction(Language.lang, "text.dialog.chooseFile.title")
        dialog.headerText = translation.getTraduction(Language.lang, "text.dialog.chooseFile.headerText")
        dialog.contentText = translation.getTraduction(Language.lang, "text.dialog.chooseFile.contentText")

        return dialog.showAndWait()
    }

    /**
     * Sets up the account filter combo box with available account options.
     *
     * This method retrieves the list of available accounts associated with the current user's
     * login information and populates the account filter combo box with these accounts.
     * Additionally, an option labeled "all" is added to the beginning of the list to allow
     * the user to view transactions from all accounts.
     *
     * The combo box's items property is updated with the retrieved and formatted list of accounts,
     * and its default value is set to the "all" option.
     */
    private fun setAccountFilterComboBox() {
        val accounts = mutableListOf(databaseManager.getAccounts(login.username))
        val all = translation.getTraduction(Language.lang, "comboBox.all")
        accounts.addFirst(listOf(all))

        accountFilterComboBox.items = FXCollections.observableArrayList(accounts.flatten())
        accountFilterComboBox.value = all
    }
}