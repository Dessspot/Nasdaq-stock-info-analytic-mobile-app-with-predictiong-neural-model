package com.example.tapochka

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Color
import androidx.appcompat.app.AlertDialog

class QuotesAdapter(
    private var quotes: List<Quote>,
    private val listener: OnItemClickListener,
    private val longClickListener: OnItemLongClickListener
) : RecyclerView.Adapter<QuotesAdapter.QuoteViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(symbol: String)
    }

    interface OnItemLongClickListener {
        fun onItemLongClick(symbol: String, action: String)
    }

    class QuoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvSymbol: TextView = view.findViewById(R.id.tvSymbol)
        val tvPriceChange: TextView = view.findViewById(R.id.tvPriceChange)
        val tvCount: TextView = view.findViewById(R.id.tvCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_quote, parent, false)
        return QuoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        val quote = quotes[position]
        holder.tvName.text = quote.name
        holder.tvSymbol.text = quote.symbol
        holder.tvPriceChange.text = "${quote.price} (${quote.changesPercentage}%)"
        holder.tvPriceChange.setTextColor(if (quote.changesPercentage >= 0) Color.GREEN else Color.RED)
        holder.tvCount.text = "${quote.count} шт."

        // Обработка клика по элементу
        holder.itemView.setOnClickListener {
            listener.onItemClick(quote.symbol)
        }

        // Обработка долгого нажатия на элемент
        holder.itemView.setOnLongClickListener {
            showContextMenu(holder.itemView.context, quote.symbol)
            true
        }
    }

    override fun getItemCount() = quotes.size

    fun updateData(newQuotes: List<Quote>) {
        quotes = newQuotes
        notifyDataSetChanged()
    }

    // Функция для отображения контекстного меню
    private fun showContextMenu(context: Context, symbol: String) {
        val items = arrayOf("Удалить", "Изменить")
        AlertDialog.Builder(context)
            .setTitle("Выберите действие")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> longClickListener.onItemLongClick(symbol, "delete")
                    1 -> longClickListener.onItemLongClick(symbol, "edit")
                }
            }
            .show()
    }
}
