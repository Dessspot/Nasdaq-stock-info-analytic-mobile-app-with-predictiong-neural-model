package com.example.tapochka

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog

class ApiKeyActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api_key)

        dbHelper = DatabaseHelper(this)

        val newsApiKeyInput = findViewById<EditText>(R.id.newsApiKeyInput)
        val stockApiKeyInput = findViewById<EditText>(R.id.stockApiKeyInput)
        val addNewsApiKeyButton = findViewById<Button>(R.id.addNewsApiKeyButton)
        val addStockApiKeyButton = findViewById<Button>(R.id.addStockApiKeyButton)
        val resetDatabaseButton = findViewById<Button>(R.id.resetDatabaseButton)

        addNewsApiKeyButton.setOnClickListener {
            val apiKey = newsApiKeyInput.text.toString()
            dbHelper.addApiKey("NewsAPI", apiKey)
            newsApiKeyInput.text.clear()
        }

        addStockApiKeyButton.setOnClickListener {
            val apiKey = stockApiKeyInput.text.toString()
            dbHelper.addApiKey("StockAPI", apiKey)
            stockApiKeyInput.text.clear()
        }

        resetDatabaseButton.setOnClickListener {
            clearDatabaseWithConfirmation()
        }
    }
    fun clearDatabaseWithConfirmation() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.apply {
            setTitle("Подтверждение")
            setMessage("Вы уверены, что хотите очистить базу данных?")
            setPositiveButton("Да") { dialogInterface: DialogInterface, i: Int ->
                // Если пользователь нажал "Да", то очищаем базу данных
                dbHelper.clearDatabase()
                // Добавьте здесь любую другую логику, которая должна выполниться после очистки базы данных
            }
            setNegativeButton("Нет") { dialogInterface: DialogInterface, i: Int ->
                // Если пользователь нажал "Нет", то закрываем диалоговое окно без выполнения дополнительных действий
                dialogInterface.dismiss()
            }
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }
}
