package com.gregoryhpotter.textlistscanner.ui.wordlist

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.GridLayout
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts
import com.gregoryhpotter.textlistscanner.data.model.WordEntry
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.gregoryhpotter.textlistscanner.databinding.FragmentWordListBinding
import com.gregoryhpotter.textlistscanner.ui.color.ColorPalette
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WordListFragment : Fragment() {

    private var _binding: FragmentWordListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WordListViewModel by viewModels()
    private lateinit var adapter: WordEntryAdapter

    // Default color for manually added words — cycles through a simple palette
    private var nextColorIndex = 0

    // -------------------------------------------------------------------------
    // File import
    // -------------------------------------------------------------------------

    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val text = requireContext().contentResolver
                    .openInputStream(uri)
                    ?.bufferedReader()
                    ?.readText() ?: return@let
                viewModel.importFromText(text)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWordListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupAddWord()
        setupImport()
        observeUiState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    private fun setupRecyclerView() {
        adapter = WordEntryAdapter(
            onToggle = { entry -> viewModel.toggleWord(entry.text) },
            onDelete = { entry -> viewModel.removeWord(entry.text) },
            onColorTap = { entry -> showColorPicker(entry) }
        )
        binding.recyclerWords.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerWords.adapter = adapter
    }

    private fun setupAddWord() {
        binding.buttonAdd.setOnClickListener {
            val text = binding.editWordInput.text?.toString() ?: return@setOnClickListener
            if (text.isBlank()) {
                Toast.makeText(requireContext(), "Enter a word first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val color = ColorPalette.colors[nextColorIndex % ColorPalette.colors.size]
            nextColorIndex++
            viewModel.addWord(text, color)
            binding.editWordInput.text?.clear()
        }
    }

    private fun showColorPicker(entry: WordEntry) {
        val context = requireContext()
        var dialog: AlertDialog? = null
        val grid = GridLayout(context).apply {
            rowCount = 2
            columnCount = 3
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(4))
        }

        ColorPalette.colors.forEach { color ->
            val swatch = View(context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(dpToPx(48), dpToPx(48)).apply {
                    setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    viewModel.updateColor(entry.text, color)
                    dialog?.dismiss()
                }
            }
            grid.addView(swatch)
        }

        dialog = AlertDialog.Builder(context)
            .setTitle("Select color")
            .setView(grid)
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun setupImport() {
        binding.buttonImport.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            importFileLauncher.launch(intent)
        }
    }

    // -------------------------------------------------------------------------
    // Observation
    // -------------------------------------------------------------------------

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.words)
                    binding.progressBar.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE
                    state.error?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}