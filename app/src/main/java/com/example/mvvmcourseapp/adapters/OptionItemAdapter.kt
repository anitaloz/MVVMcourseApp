package com.example.mvvmcourseapp.adapters

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.BounceInterpolator
import android.view.animation.CycleInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mvvmcourseapp.R
import com.example.mvvmcourseapp.UIhelper.LangButton
import com.example.mvvmcourseapp.UIhelper.OptionItem
import com.example.mvvmcourseapp.adapters.LangButtonAdapter.LangButtonViewHolder

class OptionItemAdapter(
    private val onItemClick: (OptionItem, Int) -> Unit
): RecyclerView.Adapter<OptionItemAdapter.OptionItemViewHolder>() {

    private var _data: List<OptionItem> = emptyList()
    private var selectedPosition: Int = -1
    private var showFeedback: Boolean = false

    @SuppressLint("NotifyDataSetChanged")
    fun setData(newData: List<OptionItem>) {
        _data = newData
        selectedPosition = -1
        showFeedback = false
        notifyDataSetChanged()
    }

    fun setSelectedPosition(position: Int, feedback:Boolean) {
        if (position != -1 && feedback) {
            val oldPosition = selectedPosition
            selectedPosition = position
            showFeedback = true

            if (oldPosition != -1) {
                notifyItemChanged(oldPosition)
            }
            notifyItemChanged(position)
        }
    }

    class OptionItemViewHolder(
        itemView: View,
        private val onItemClick: (OptionItem, Int) -> Unit
    ): RecyclerView.ViewHolder(itemView) {
        private val optionItemText: TextView = itemView.findViewById(R.id.option_item_text)
        private val optionItemImage: ImageView = itemView.findViewById(R.id.option_item_image)
        private val optionItemWrap: ViewGroup = itemView.findViewById(R.id.option_item_wrap)

        fun bind(optionItem: OptionItem, position: Int, isSelected: Boolean, showFeedback: Boolean) {
            optionItemText.text = optionItem.optionText
            adjustTextSize(optionItemText)

            // Сбрасываем все анимации
            itemView.clearAnimation()
            optionItemImage.clearAnimation()
            optionItemImage.scaleX = 1f
            optionItemImage.scaleY = 1f

            if (isSelected && showFeedback) {
                // Показываем фидбек с анимацией
                showFeedbackWithAnimation(optionItem)
            } else {
                // Обычное состояние
                optionItemImage.setImageResource(optionItem.optionImage)
                optionItemImage.clearColorFilter()
            }

            itemView.setOnClickListener {
                onItemClick(optionItem, position)
            }
        }

        private fun showFeedbackWithAnimation(optionItem: OptionItem) {
            val iconRes = if (optionItem.option.correct) {
                R.drawable.check_circle_24px
            } else {
                R.drawable.cancel_28
            }

            val colorRes = if (optionItem.option.correct) {
                R.color.green
            } else {
                R.color.red
            }

            val color = ContextCompat.getColor(itemView.context, colorRes)

            if (optionItem.option.correct) {
                // Анимация для правильного ответа - масштабирование иконки
                optionItemImage.animate()
                    .scaleX(1.3f)
                    .scaleY(1.3f)
                    .setDuration(200)
                    .withEndAction {
                        optionItemImage.setImageResource(iconRes)
                        optionItemImage.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                        optionItemImage.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .start()
                    }
                    .start()
            } else {
                // Анимация для неправильного ответа - тряска всего элемента
                optionItemImage.setImageResource(iconRes)
                optionItemImage.setColorFilter(color, PorterDuff.Mode.SRC_IN)

                // Трясем весь itemView (весь элемент optionItem)
                val shakeAnimation = AnimationUtils.loadAnimation(itemView.context, R.anim.shake)
                itemView.startAnimation(shakeAnimation)
            }
        }

        private fun adjustTextSize(textView: TextView) {
            if (textView.text.length > 20) {
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            }
            else textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.option_item, parent, false)
        return OptionItemViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: OptionItemViewHolder, position: Int) {
        val optionItem = _data[position]
        val isSelected = position == selectedPosition
        holder.bind(optionItem, position, isSelected, showFeedback)
    }

    override fun getItemCount(): Int = _data.size
}