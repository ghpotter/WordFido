package com.gregoryhpotter.textlistscanner.ui.wordlist

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.GridLayout
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts
import com.gregoryhpotter.textlistscanner.data.model.WordEntry
import com.gregoryhpotter.textlistscanner.data.model.WordProfile
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.gregoryhpotter.textlistscanner.R
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
    private lateinit var profileAdapter: ArrayAdapter<String>
    private var profileList: List<WordProfile> = emptyList()
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
        applyWindowInsets()
        setupRecyclerView()
        setupAddWord()
        setupImport()
        setupSearch()
        setupProfileSpinner()
        observeUiState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    private fun applyWindowInsets() {
        val dp16 = (16 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(dp16, systemBars.top + dp16, dp16, dp16)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupRecyclerView() {
        adapter = WordEntryAdapter(
            onToggle = { entry -> viewModel.toggleWord(entry.text) },
            onDelete = { entry -> showDeleteSnackbar(entry) },
            onColorTap = { entry -> showColorPicker(entry) }
        )
        binding.recyclerWords.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerWords.adapter = adapter
    }

    private fun setupAddWord() {
        binding.buttonAdd.setOnClickListener {
            val text = binding.editWordInput.text?.toString() ?: return@setOnClickListener
            if (text.isBlank()) return@setOnClickListener
            val color = ColorPalette.colors[nextColorIndex % ColorPalette.colors.size]
            nextColorIndex++
            viewModel.addWord(text, color)
            binding.editWordInput.text?.clear()
        }
    }

    private fun setupImport() {
        binding.buttonImport.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            importFileLauncher.launch(intent)
        }
    }

    private fun setupSearch() {
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
        })
    }

    private fun setupProfileSpinner() {
        profileAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item)
        profileAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerProfile.adapter = profileAdapter

        binding.spinnerProfile.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = profileList.getOrNull(position) ?: return
                if (selected.id != viewModel.uiState.value.activeProfileId) {
                    viewModel.switchProfile(selected.id)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.buttonNewProfile.setOnClickListener { showNewProfileDialog() }
        binding.buttonDeleteProfile.setOnClickListener { showDeleteProfileDialog() }
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

                    updateProfileSpinner(state.profiles, state.activeProfileId)
                    binding.buttonDeleteProfile.isEnabled = state.profiles.size > 1
                }
            }
        }
    }

    private fun updateProfileSpinner(profiles: List<WordProfile>, activeId: Long) {
        if (profiles == profileList) return
        profileList = profiles
        profileAdapter.clear()
        profileAdapter.addAll(profiles.map { it.name })
        profileAdapter.notifyDataSetChanged()

        val idx = profiles.indexOfFirst { it.id == activeId }
        if (idx >= 0 && binding.spinnerProfile.selectedItemPosition != idx) {
            binding.spinnerProfile.setSelection(idx)
        }
    }

    // -------------------------------------------------------------------------
    // Undo deletion
    // -------------------------------------------------------------------------

    private fun showDeleteSnackbar(entry: WordEntry) {
        viewModel.removeWord(entry)
        Snackbar.make(binding.root, R.string.word_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) { viewModel.undoDelete() }
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(bar: Snackbar?, event: Int) {
                    if (event != DISMISS_EVENT_ACTION) viewModel.confirmDelete()
                }
            })
            .show()
    }

    // -------------------------------------------------------------------------
    // Profile dialogs
    // -------------------------------------------------------------------------

    private fun showNewProfileDialog() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.new_list_hint)
            setSingleLine()
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.new_list_title)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotBlank()) viewModel.createProfile(name)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showDeleteProfileDialog() {
        val active = profileList.find { it.id == viewModel.uiState.value.activeProfileId } ?: return
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_list_title)
            .setMessage(getString(R.string.delete_list_message, active.name))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.deleteProfile(active.id)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // -------------------------------------------------------------------------
    // Color picker
    // -------------------------------------------------------------------------

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
}
