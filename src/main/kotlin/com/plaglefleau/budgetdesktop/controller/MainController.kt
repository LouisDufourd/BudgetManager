package com.plaglefleau.budgetdesktop.controller

import com.plaglefleau.budgetdesktop.database.models.DatabaseTransactionModel
import com.plaglefleau.budgetdesktop.database.models.User
import com.plaglefleau.budgetdesktop.managers.DatabaseManager
import com.plaglefleau.budgetdesktop.managers.TransactionManager
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.util.Callback
import java.io.File
import java.nio.file.Paths
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import kotlin.reflect.full.memberProperties
import kotlin.system.exitProcess

class MainController {

    lateinit var databaseTransactionModelTableView: TableView<DatabaseTransactionModel>
    lateinit var fluctuationTextField: TextField
    lateinit var totalCreditTextField: TextField
    lateinit var totalDebitTextField: TextField
    lateinit var beforeDatePicker: DatePicker
    lateinit var afterDatePicker: DatePicker
    lateinit var loadFileButton: MenuItem
    lateinit var quitButton: MenuItem
    lateinit var showAll: RadioMenuItem
    lateinit var onlyDebits: RadioMenuItem
    lateinit var onlyCredits: RadioMenuItem

    private lateinit var toggleGroup: ToggleGroup

    private var databaseManager: DatabaseManager = DatabaseManager("")
    private val transactionManager = TransactionManager()

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
        databaseManager = DatabaseManager(login.password)
        this.primaryStage = primaryStage

        primaryStage.minWidth = 1000.0
        primaryStage.minHeight = 600.0
        primaryStage.height = 600.0
        primaryStage.width = 1000.0
        primaryStage.isResizable = true

        val transactionList = getTransactions()
        setColumns(databaseTransactionModelTableView, DatabaseTransactionModel::class)
        databaseTransactionModelTableView.items.setAll(transactionList)
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
        return FXCollections.observableArrayList(getTransactions())
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
    private fun calendarFromLocalDate(localeDate: LocalDate): Calendar {
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

        val transactions = getTransactionsBasedOnSelectedDate()

        transactions.forEach {
            totalDebit += it.debit ?: 0.0
            totalCredit += it.credit ?: 0.0
        }

        totalDebitTextField.text = decimalFormat.format(totalDebit)
        totalCreditTextField.text = decimalFormat.format(totalCredit)
        fluctuationTextField.text = decimalFormat.format(totalCredit - totalDebit)
    }

    /**
     * Retrieves a list of transactions based on the selected date criteria.
     *
     * This method retrieves the transactions from the database using the databaseManager.
     * It checks the beforeDatePicker and afterDatePicker values to determine the date range.
     * If both values are null, it retrieves all transactions for the given username.
     * If only the beforeDatePicker value is not null, it retrieves transactions that occurred before the specified date.
     * If only the afterDatePicker value is not null, it retrieves transactions that occurred after the specified date.
     * If both values are not null, it retrieves transactions that fall between the specified dates.
     * The returned list of transactions is of type List<DatabaseTransactionModel>.
     *
     * @return The list of transactions based on the selected date criteria.
     */
    private fun getTransactionsBasedOnSelectedDate() : List<DatabaseTransactionModel> {
        return if (afterDatePicker.value == null && beforeDatePicker.value == null) {
            databaseManager.getTransactions(login.username)
        } else if (afterDatePicker.value == null && beforeDatePicker.value != null) {
            databaseManager.getTransactionsBefore(
                login.username,
                calendarFromLocalDate(beforeDatePicker.value)
            )
        } else if (afterDatePicker.value != null && beforeDatePicker.value == null) {
            databaseManager.getTransactionsAfter(
                login.username,
                calendarFromLocalDate(afterDatePicker.value)
            )
        } else {
            databaseManager.getTransactionsBetween(
                login.username,
                calendarFromLocalDate(beforeDatePicker.value),
                calendarFromLocalDate(afterDatePicker.value)
            )
        }
    }

    /**
     * Retrieves a list of transactions based on the selected date criteria.
     *
     * This method retrieves the transactions from the database using the databaseManager.
     * It checks the beforeDatePicker and afterDatePicker values to determine the date range.
     * If both values are null, it retrieves all transactions for the given username.
     * If only the beforeDatePicker*/
    private fun getTransactions(): List<DatabaseTransactionModel> {
        val transactions = getTransactionsBasedOnSelectedDate()

        return if(onlyCredits.isSelected) {
            transactions.filter {
                it.credit != 0.0 && it.debit == 0.0
            }
        } else if (onlyDebits.isSelected) {
            transactions.filter {
                it.debit != 0.0 && it.credit == 0.0
            }
        } else {
            transactions
        }
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
            val column = TableColumn<T, String>(prop.name.capitalize())

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

                    if(prop.name != "username") {
                        column.cellValueFactory = Callback { cellData ->
                            val value = prop.get(cellData.value) as? String
                            SimpleStringProperty(value!!.replace("\n", "").replace("\r", "").replace("\"","").replace(Regex("\\s+"), " ").trim())
                        }
                        tableView.columns.add(column)
                    }
                }
                else -> {
                    column.cellValueFactory = PropertyValueFactory<T, String>(prop.name)
                    tableView.columns.add(column)
                }
            }

        }
    }
}