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

class LangButtonAdapter(
    private val onItemClick: (LangButton) -> Unit
) : RecyclerView.Adapter<LangButtonAdapter.LangButtonViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    var data: List<LangButton> = emptyList()
        set(newValue) {
            field = newValue
            notifyDataSetChanged()
        }

    class LangButtonViewHolder(
        langButton: View,
        private val onItemClick: (LangButton) -> Unit
    ) : RecyclerView.ViewHolder(langButton) {
        private val buttonImage: ImageView = langButton.findViewById(R.id.button_image)
        private val buttonText: TextView = langButton.findViewById(R.id.button_text)


        fun bind(langButton: LangButton) {
            buttonImage.setImageResource(langButton.image)
            buttonText.text = langButton.name
            itemView.setOnClickListener {
                onItemClick(langButton)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LangButtonViewHolder {
        val langButton = LayoutInflater.from(parent.context)
            .inflate(R.layout.lang_menu_item, parent, false)
        return LangButtonViewHolder(langButton, onItemClick)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: LangButtonViewHolder, position: Int) {
        holder.bind(data[position])
    }
}