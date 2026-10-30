package com.example.ragassistant

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(
    private val messages: MutableList<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val container: LinearLayout =
            view.findViewById(R.id.messageContainer)

        val sender: TextView =
            view.findViewById(R.id.tvSender)

        val message: TextView =
            view.findViewById(R.id.tvMessage)

        val source: TextView =
            view.findViewById(R.id.tvSource)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MessageViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)

        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: MessageViewHolder,
        position: Int
    ) {

        val item = messages[position]

        holder.message.text = item.message

        if (item.isUser) {

            holder.sender.text = "You"

            holder.container.gravity = Gravity.END

            holder.message.setTextColor(Color.WHITE)

            val background = GradientDrawable()
            background.setColor(Color.rgb(79, 70, 229))
            background.cornerRadius = 28f

            holder.message.background = background

            holder.source.visibility = View.GONE

        } else {

            holder.sender.text = "RAG Assistant"

            holder.container.gravity = Gravity.START

            holder.message.setTextColor(Color.rgb(17, 24, 39))

            val background = GradientDrawable()
            background.setColor(Color.WHITE)
            background.cornerRadius = 28f

            holder.message.background = background

            if (!item.source.isNullOrBlank()) {

                holder.source.text =
                    "Source: ${item.source}"

                holder.source.visibility =
                    View.VISIBLE

            } else {

                holder.source.visibility =
                    View.GONE
            }
        }
    }

    override fun getItemCount(): Int =
        messages.size
}