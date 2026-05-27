package com.gregoryhpotter.textlistscanner.ui.color

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.annotation.ColorInt
import androidx.core.view.setPadding
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.gregoryhpotter.textlistscanner.R

class ColorPickerDialog : DialogFragment() {

    var onColorSelected: ((Int) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val grid = GridLayout(context).apply {
            columnCount = 4
            rowCount = 3
            setPadding(32)
        }

        ColorPalette.colors.forEach { color ->
            val swatch = View(context).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 120
                    height = 120
                    setMargins(12, 12, 12, 12)
                }
                setBackgroundColor(color)
                setOnClickListener {
                    onColorSelected?.invoke(color)
                    dismiss()
                }
            }
            grid.addView(swatch)
        }

        return MaterialAlertDialogBuilder(context)
            .setTitle("Pick a color")
            .setView(grid)
            .setNegativeButton("Cancel", null)
            .create()
    }

    companion object {
        const val TAG = "ColorPickerDialog"
    }
}