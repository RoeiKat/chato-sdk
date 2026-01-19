package com.chato.sdk.ui

import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chato.sdk.R
import com.chato.sdk.databinding.ItemChatoMessageBinding
import com.chato.sdk.ui.model.ChatoMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private var customerBubbleColor: Int? = null
) : RecyclerView.Adapter<ChatAdapter.VH>() {

    private val items = mutableListOf<ChatoMessage>()
    private val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun setCustomerBubbleColor(color: Int?) {
        customerBubbleColor = color
        notifyDataSetChanged()
    }

    fun submitList(newList: List<ChatoMessage>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    fun submitAppend(msg: ChatoMessage) {
        items.add(msg)
        notifyItemInserted(items.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemChatoMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], fmt, customerBubbleColor)
    }

    override fun getItemCount(): Int = items.size

    class VH(private val b: ItemChatoMessageBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(m: ChatoMessage, fmt: SimpleDateFormat, customerColor: Int?) {
            val ctx = b.root.context
            b.bubble.text = m.text
            b.meta.text = fmt.format(Date(m.at))

            val isCustomer = m.from == "customer"

            val bg = GradientDrawable().apply {
                cornerRadius = 18f
                setColor(
                    if (isCustomer) {
                        customerColor ?: ContextCompat.getColor(ctx, R.color.chato_primary)
                    } else {
                        ContextCompat.getColor(ctx, R.color.chato_gray)
                    }
                )
            }
            b.bubble.background = bg
            b.root.gravity = if (isCustomer) Gravity.END else Gravity.START
        }
    }
}
