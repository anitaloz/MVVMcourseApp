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
import com.example.mvvmcourseapp.UIhelper.LangLvlView

class LangLvlAdapter() : RecyclerView.Adapter<LangLvlAdapter.LangLvlViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    var data: List<LangLvlView> = emptyList()
        set(newValue) {
            field = newValue
            notifyDataSetChanged()
        }

    class LangLvlViewHolder(
        langLvlView: View
    ) : RecyclerView.ViewHolder(langLvlView) {
        private val textView: TextView = langLvlView.findViewById(R.id.setting_of_level)
        private val lvlView: TextView = langLvlView.findViewById(R.id.lvl)


        fun bind(langLvlView: LangLvlView) {
            textView.text="Уровень подготовки ${langLvlView.langName}"
            lvlView.text = langLvlView.lvl.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LangLvlViewHolder {
        val langLvlView = LayoutInflater.from(parent.context)
            .inflate(R.layout.lang_lvl_item_layout, parent, false)
        return LangLvlViewHolder(langLvlView)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: LangLvlViewHolder, position: Int) {
        holder.bind(data[position])
    }
}