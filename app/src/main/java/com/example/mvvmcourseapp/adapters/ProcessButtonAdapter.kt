package com.example.mvvmcourseapp.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mvvmcourseapp.R
import com.example.mvvmcourseapp.UIhelper.LangButton
import com.example.mvvmcourseapp.UIhelper.ProcessButton

class ProcessButtonAdapter(private val onItemClick: (ProcessButton) -> Unit
) : RecyclerView.Adapter<ProcessButtonAdapter.ProcessButtonViewHolder>(){

    @SuppressLint("NotifyDataSetChanged")
    var data: List<ProcessButton> = emptyList()
        set(newValue) {
            field = newValue
            notifyDataSetChanged()
        }

    class ProcessButtonViewHolder(
        processButton: View,
        private val onItemClick: (ProcessButton) -> Unit
    ) : RecyclerView.ViewHolder(processButton) {
        private val processButtonImage: ImageView = processButton.findViewById(R.id.process_button_image)
        private val processButtonText: TextView = processButton.findViewById(R.id.process_button_text)
        private val processButtonCount:TextView = processButton.findViewById(R.id.process_button_count_text)

        fun bind(processButton: ProcessButton) {
            processButtonImage.setImageResource(processButton.image)
            processButtonText.text = processButton.title
            processButtonCount.text=processButton.cardsCount

            itemView.setOnClickListener {
                onItemClick(processButton)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProcessButtonViewHolder {
        val langButton = LayoutInflater.from(parent.context)
            .inflate(R.layout.process_menu_item, parent, false)
        return ProcessButtonViewHolder(langButton, onItemClick)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ProcessButtonViewHolder, position: Int) {
        holder.bind(data[position])
    }
}