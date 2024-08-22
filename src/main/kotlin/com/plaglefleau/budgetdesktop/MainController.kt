package com.plaglefleau.budgetdesktop

import com.plaglefleau.budgetdesktop.database.models.DatabaseTransactionModel
import com.plaglefleau.budgetdesktop.managers.DatabaseManager
import com.plaglefleau.budgetdesktop.managers.TransactionManager
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.util.Callback
import java.io.File
import java.net.URL
import java.nio.file.Paths
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.full.memberProperties
import kotlin.system.exitProcess

class MainController : Initializable {

    lateinit var databaseTransactionModelTableView: TableView<DatabaseTransactionModel>
    lateinit var fluctuationTextField: TextField
    lateinit var totalCreditTextField: TextField
    lateinit var totalDebitTextField: TextField
    lateinit var dateDirectionSwitch: CheckBox
    lateinit var datePicker: DatePicker
    lateinit var loadFileButton: MenuItem
    lateinit var quitButton: MenuItem

    private val databaseManager = DatabaseManager()
    private val transactionManager = TransactionManager()

    var primaryStage: Stage? = null

    override fun initialize(url: URL?, resource: ResourceBundle?) {
        val transactionList = databaseManager.getTransactions()
        createColumns(databaseTransactionModelTableView, DatabaseTransactionModel::class)
        databaseTransactionModelTableView.items.setAll(transactionList)
        setTotalCreditDebitAndFluctuation()

        loadFileButton.setOnAction {
            val fileChooser = FileChooser()
            fileChooser.title = "Choose a CSV file"
            fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("CSV Files", "*.csv"))
            fileChooser.initialDirectory = getDownloadsFolder()
            val chosenFile = fileChooser.showOpenDialog(primaryStage)

            if (chosenFile != null) {
                val transactions = transactionManager.parseTransactions(chosenFile)
                databaseManager.uploadTransactions(transactions)
                databaseTransactionModelTableView.items = getFilteredList()
                setTotalCreditDebitAndFluctuation()
            }
        }

        datePicker.setOnAction {
            databaseTransactionModelTableView.items = getFilteredList()
            setTotalCreditDebitAndFluctuation()
        }

        quitButton.setOnAction {
            exitProcess(0)
        }

        dateDirectionSwitch.setOnMouseClicked {
            if(dateDirectionSwitch.isSelected) {
                dateDirectionSwitch.text = "After"
            } else {
                dateDirectionSwitch.text = "Before"
            }

            databaseTransactionModelTableView.items = getFilteredList()
            setTotalCreditDebitAndFluctuation()
        }
    }

    private fun getFilteredList(): ObservableList<DatabaseTransactionModel> {
        val transactions = FXCollections.observableList(databaseManager.getTransactions())

        return if (datePicker.value == null) {
            // Return all items if no date is selected
            FXCollections.observableArrayList(transactions)
        } else {

            val calendar = Calendar.getInstance()
            calendar.time = Date.from(datePicker.value.atStartOfDay(calendar.timeZone.toZoneId()).toInstant())

            if (dateDirectionSwitch.isSelected) {
                FXCollections.observableList(databaseManager.getTransactionsAfter(calendar))
            } else {
                FXCollections.observableList(databaseManager.getTransactionsBefore(calendar))
            }
        }
    }

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

    private fun setTotalCreditDebitAndFluctuation() {
        val decimalFormat = DecimalFormat("#0.00€")

        var totalDebit = 0.0
        var totalCredit = 0.0

        databaseTransactionModelTableView.items.forEach {
            totalDebit += it.debit ?: 0.0
            totalCredit += it.credit ?: 0.0
        }

        totalDebitTextField.text = decimalFormat.format(totalDebit)
        totalCreditTextField.text = decimalFormat.format(totalCredit)
        fluctuationTextField.text = decimalFormat.format(totalCredit - totalDebit)
    }

    private fun <T : Any> createColumns(tableView: TableView<T>, kClass: kotlin.reflect.KClass<T>) {
        kClass.memberProperties.forEach { prop ->
            val column = TableColumn<T, String>(prop.name.capitalize())

            // Custom cell value factory for Calendar
            if (prop.returnType.classifier == Calendar::class) {
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
            } else if (prop.returnType.classifier == Double::class) {
                val df = DecimalFormat("#0.00€")
                column.cellValueFactory = Callback { cellData ->
                    val value = prop.get(cellData.value) as? Double
                    SimpleStringProperty(df.format(value))
                }
                tableView.columns.add(column)
            } else if (prop.returnType.classifier == String::class) {
                column.cellValueFactory = Callback { cellData ->
                    val value = prop.get(cellData.value) as? String
                    SimpleStringProperty(value!!.replace("\n", "").replace("\r", "").replace("\"","").replace(Regex("\\s+"), " ").trim())
                }
                tableView.columns.add(column)
            }else {
                column.cellValueFactory = PropertyValueFactory<T, String>(prop.name)
                tableView.columns.add(column)
            }

        }
    }
}