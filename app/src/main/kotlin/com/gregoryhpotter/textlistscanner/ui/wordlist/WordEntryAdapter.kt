package com.gregoryhpotter.textlistscanner.ui.wordlist

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gregoryhpotter.textlistscanner.data.model.WordEntry
import com.gregoryhpotter.textlistscanner.databinding.ItemWordEntryBinding

class WordEntryAdapter(
    private val onToggle: (WordEntry) -> Unit,
    private val onDelete: (WordEntry) -> Unit,
    private val onColorTap: (WordEntry) -> Unit,
) : ListAdapter<WordEntry, WordEntryAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWordEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemWordEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: WordEntry) {
            binding.textWord.text = entry.text
            binding.textWord.alpha = if (entry.enabled) 1.0f else 0.4f

            val swatch = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(entry.color)
            }
            binding.viewColorSwatch.background = swatch

            binding.switchEnabled.isChecked = entry.enabled
            binding.switchEnabled.setOnCheckedChangeListener(null)
            binding.switchEnabled.setOnCheckedChangeListener { _, _ ->
                onToggle(entry)
            }

            binding.viewColorSwatch.setOnClickListener {
                onColorTap(entry)
            }

            binding.buttonDelete.setOnClickListener {
                onDelete(entry)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<WordEntry>() {
            override fun areItemsTheSame(old: WordEntry, new: WordEntry) =
                old.text == new.text

            override fun areContentsTheSame(old: WordEntry, new: WordEntry) =
                old == new
        }
    }
}