package com.plaglefleau.budgetdesktop.controller

import com.plaglefleau.budgetdesktop.Language
import com.plaglefleau.budgetdesktop.database.models.DatabaseTransactionModel
import com.plaglefleau.budgetdesktop.managers.DatabaseManager
import com.plaglefleau.budgetdesktop.managers.TransactionManager
import javafx.collections.FXCollections
import javafx.fxml.Initializable
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.chart.XYChart.Series
import javafx.scene.control.ComboBox
import javafx.scene.control.DatePicker
import javafx.scene.control.Label
import javafx.scene.control.Tooltip
import java.net.URL
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*


class ChartsController: Initializable {
    lateinit var afterDatePickerLabel: Label
    lateinit var beforeDatePickerLabel: Label
    lateinit var accountFilterLabel: Label

    lateinit var accountFilterComboBox: ComboBox<String>
    lateinit var beforeDatePicker: DatePicker
    lateinit var afterDatePicker: DatePicker
    lateinit var lineChart: LineChart<String, Double>

    lateinit var xAxis: CategoryAxis
    lateinit var yAxis: NumberAxis

    private var databaseManager: DatabaseManager = DatabaseManager("","")
    private var transactionManager: TransactionManager = TransactionManager("","")
    private val translation = Language.translation
    private var username: String = ""

    private val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy")
    private val decimalFormat = DecimalFormat("#0.##€")

    fun setValue(username: String, password: String) {
        this.databaseManager = DatabaseManager(username, password)
        this.transactionManager = TransactionManager(username, password)

        val accounts = mutableListOf(databaseManager.getAccounts(username))
        accounts.addFirst(listOf(translation.getTraduction(Language.lang, "comboBox.all")))
        accountFilterComboBox.items = FXCollections.observableArrayList(accounts.flatten())
        accountFilterComboBox.value = accounts.flatten().first()

        this.username = username

        setCharts()
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {

        xAxis.label = translation.getTraduction(Language.lang, "text.date")
        xAxis.tickLabelRotation = 90.0

        yAxis.label = translation.getTraduction(Language.lang, "text.amount")

        updateLang()
        setupListener()
    }

    private fun setupListener() {
        accountFilterComboBox.setOnAction { setCharts() }

        beforeDatePicker.setOnAction { setCharts() }

        afterDatePicker.setOnAction { setCharts() }
    }

    private fun setCharts() {
        val accounts = databaseManager.getAccounts(username)

        val seriesList = mutableListOf<Series<String, Double>>()

        if(accountFilterComboBox.value == translation.getTraduction(Language.lang, "comboBox.all")) {
            accounts.forEach { account ->
                seriesList.add(getSeries(account))
            }
        } else {
            seriesList.add(getSeries(accountFilterComboBox.value))
        }

        lineChart.data.setAll(seriesList)

        seriesList.forEach { series ->
            series.data.forEach { data ->
                val tooltip = Tooltip("Date : ${data.xValue},\n Amount : ${decimalFormat.format(data.yValue)}")
                Tooltip.install(data.node, tooltip)
                data.node.setOnMouseEntered {
                    data.node.style = "-fx-background-color: #ff0000;"
                }
                data.node.setOnMouseExited {
                    data.node.style = ""
                }
            }
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

    private fun getTransactions(account: String) : List<DatabaseTransactionModel> {
        val transactions = transactionManager.getTransactions(
            account,
            onlyDebits = false,
            onlyCredits = false,
            beforeDate = calendarFromLocalDate(beforeDatePicker.value),
            afterDate = calendarFromLocalDate(afterDatePicker.value)
        )

        return transactions.sortedBy { it.date }
    }

    private fun getSeries(account: String) : Series<String, Double> {
        val transactions = getTransactions(account)

        val series = Series<String, Double>()

        series.name = account

        val transactionsByDate = mutableMapOf<Calendar, Double>()

        transactions.forEach { transaction ->
            var total = transactionsByDate[transaction.date] ?: 0.0
            if(transaction.debit != null && transaction.credit == null) {
                total -= transaction.debit
            } else if (transaction.credit != null && transaction.debit == null) {
                total += transaction.credit
            } else if (transaction.credit != null && transaction.debit != null){
                total -= transaction.debit
                total += transaction.credit
            }
            transactionsByDate[transaction.date] = total
        }

        transactionsByDate.forEach { entry ->
            series.data.add(XYChart.Data(simpleDateFormat.format(entry.key.time), entry.value))
        }

        series.data.forEach { data ->
            val tooltip = Tooltip("Date : ${data.xValue},\n Amount : ${decimalFormat.format(data.yValue)}")
            Tooltip.install(data.node, tooltip)
        }

        return series
    }

    private fun updateLang() {
        accountFilterLabel.text = translation.getTraduction(Language.lang, "comboBox.account")
        beforeDatePickerLabel.text = translation.getTraduction(Language.lang, "datePicker.before")
        afterDatePickerLabel.text = translation.getTraduction(Language.lang, "datePicker.after")
    }
}