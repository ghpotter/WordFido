package com.gregoryhpotter.textlistscanner.ui.camera

import android.Manifest
import android.content.res.ColorStateList
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import android.view.HapticFeedbackConstants
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.gregoryhpotter.textlistscanner.R
import com.gregoryhpotter.textlistscanner.databinding.FragmentCameraBinding
import com.gregoryhpotter.textlistscanner.feedback.AudioFeedbackTone
import com.gregoryhpotter.textlistscanner.feedback.FeedbackManager
import com.gregoryhpotter.textlistscanner.ocr.MlKitTextAnalyzer
import com.gregoryhpotter.textlistscanner.ocr.OcrResultProcessor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CameraViewModel by viewModels()

    @Inject lateinit var ocrProcessor: OcrResultProcessor
    @Inject lateinit var feedbackManager: FeedbackManager

    private lateinit var cameraExecutor: ExecutorService
    private val coordinateMapper = CoordinateMapper()
    private var textAnalyzer: MlKitTextAnalyzer? = null
    private var previousMatchedWords: Set<String> = emptySet()
    private var stableHighlights: Map<Int, HighlightData> = emptyMap()
    private var camera: Camera? = null
    private var torchController: TorchController? = null

    // -------------------------------------------------------------------------
    // Permission handling
    // -------------------------------------------------------------------------

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else Toast.makeText(requireContext(),
            "Camera permission is required", Toast.LENGTH_LONG).show()
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.root.isHapticFeedbackEnabled = true
        binding.previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
        setupSettingsButton()
        feedbackManager.onWordsCleared()

        if (hasCameraPermission()) startCamera()
        else requestPermissionLauncher.launch(Manifest.permission.CAMERA)

        binding.fabWordList.setOnClickListener {
            findNavController().navigate(R.id.action_camera_to_wordlist)
        }

        observeUiState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        torchController?.release()
        torchController = null
        camera = null
        textAnalyzer?.shutdown()
        cameraExecutor.shutdown()
        _binding = null
    }

    // -------------------------------------------------------------------------
    // Settings button and popup menu
    // -------------------------------------------------------------------------

    private fun setupSettingsButton() {
        binding.buttonSettings.setOnClickListener {
            showSettingsPopup()
        }
    }

    private fun showSettingsPopup() {
        val popup = PopupMenu(requireContext(), binding.buttonSettings)
        popup.menuInflater.inflate(R.menu.menu_camera, popup.menu)
        syncPopupMenuCheckedStates(popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_case_sensitive -> {
                    val newValue = !item.isChecked
                    item.isChecked = newValue
                    viewModel.setCaseSensitive(newValue)
                    true
                }
                R.id.action_whole_word -> {
                    val newValue = !item.isChecked
                    item.isChecked = newValue
                    viewModel.setWholeWord(newValue)
                    true
                }
                R.id.action_haptic -> {
                    val newValue = !item.isChecked
                    item.isChecked = newValue
                    viewModel.setHapticEnabled(newValue)
                    feedbackManager.setHapticEnabled(newValue)
                    true
                }
                R.id.action_audio -> {
                    val newValue = !item.isChecked
                    item.isChecked = newValue
                    viewModel.setAudioEnabled(newValue)
                    feedbackManager.setAudioEnabled(newValue)
                    true
                }
                R.id.action_audio_sound -> {
                    showAudioTonePicker()
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    private fun syncPopupMenuCheckedStates(menu: android.view.Menu) {
        val state = viewModel.uiState.value
        menu.findItem(R.id.action_case_sensitive)?.isChecked = state.caseSensitive
        menu.findItem(R.id.action_whole_word)?.isChecked = state.wholeWord
        menu.findItem(R.id.action_haptic)?.isChecked = state.hapticEnabled
        menu.findItem(R.id.action_audio)?.isChecked = state.audioEnabled
        menu.findItem(R.id.action_audio_sound)?.title =
            getString(R.string.audio_sound_label, getString(state.audioTone.labelResId))
    }

    private fun showAudioTonePicker() {
        val tones = AudioFeedbackTone.values()
        val selectedIndex = tones.indexOf(viewModel.uiState.value.audioTone)
        val toneNames = tones.map { getString(it.labelResId) }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.audio_sound_title)
            .setSingleChoiceItems(toneNames, selectedIndex) { dialog, which ->
                val tone = tones[which]
                viewModel.setAudioTone(tone)
                feedbackManager.setAudioTone(tone)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // -------------------------------------------------------------------------
    // Camera setup
    // -------------------------------------------------------------------------

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindCamera(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCamera(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.previewView.surfaceProvider)
        }

        val analyzer = MlKitTextAnalyzer(
            processor = ocrProcessor,
            onResults = { matches -> viewModel.onOcrResults(matches) },
            wordListProvider = { viewModel.uiState.value.wordList },
            caseSensitive = viewModel.uiState.value.caseSensitive,
            wholeWord = viewModel.uiState.value.wholeWord
        ).also { textAnalyzer = it }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(cameraExecutor, analyzer) }

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
            setupTorchButton()
        } catch (e: Exception) {
            Toast.makeText(requireContext(),
                "Failed to start camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTorchButton() {
        val c = camera ?: return
        val controller = TorchController(c.cameraInfo, c.cameraControl).also { torchController = it }
        binding.fabTorch.visibility = if (controller.hasFlash) View.VISIBLE else View.GONE
        binding.fabTorch.setOnClickListener {
            controller.toggle()
            binding.fabTorch.backgroundTintList =
                if (controller.enabled) ColorStateList.valueOf(0xFFFFC107.toInt()) else null
        }
    }

    // -------------------------------------------------------------------------
    // UI state observation
    // -------------------------------------------------------------------------

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateOverlay(state)
                    updateFeedback(state)
                    textAnalyzer?.updateSettings(
                        caseSensitive = state.caseSensitive,
                        wholeWord = state.wholeWord
                    )
                }
            }
        }
    }


    private fun updateOverlay(state: CameraUiState) {
        val previewWidth = binding.previewView.width
        val previewHeight = binding.previewView.height
        if (previewWidth == 0 || previewHeight == 0) return

        if (state.activeMatches.isEmpty()) {
            stableHighlights = emptyMap()
            binding.overlayView.clearHighlights()
            return
        }

        val newHighlights = state.activeMatches.mapIndexedNotNull { index, match ->
            val box = match.boundingBox ?: return@mapIndexedNotNull null
            val mapped = coordinateMapper.mapToPreview(
                boundingBox = MapperRect(
                    left = box.left.toFloat(),
                    top = box.top.toFloat(),
                    right = box.right.toFloat(),
                    bottom = box.bottom.toFloat(),
                    cornerPoints = box.cornerPoints.map { MapperPoint(it.x.toFloat(), it.y.toFloat()) }
                ),
                imageWidth = match.imageWidth,
                imageHeight = match.imageHeight,
                previewWidth = previewWidth,
                previewHeight = previewHeight,
                rotationDegrees = match.rotationDegrees
            )
            val paddingPx = 8 * resources.displayMetrics.density
            val padded = expandRect(mapped, paddingPx)
            index to HighlightData(rect = padded, color = match.entry.color)
        }.toMap()

        // Only update a word's box if it's new or has moved substantially
        val merged = newHighlights.mapValues { (idx, newData) ->
            val existing = stableHighlights[idx]
            if (existing == null || hasMovedSignificantly(existing.rect, newData.rect)) newData
            else existing
        }
        stableHighlights = merged

        binding.overlayView.updateHighlights(stableHighlights.values.toList())
    }

    private fun hasMovedSignificantly(old: MapperRect, new: MapperRect, threshold: Float = 20f): Boolean {
        return Math.abs(old.left - new.left) > threshold ||
                Math.abs(old.top - new.top) > threshold
    }


    private fun updateFeedback(state: CameraUiState) {
        val currentWords = state.activeMatches.map { it.entry.text }.toSet()

        val wordsLeft = previousMatchedWords - currentWords
        if (wordsLeft.isNotEmpty()) feedbackManager.onWordsCleared()

        currentWords.forEach { word ->
            if (feedbackManager.onWordDetected(word) && state.hapticEnabled) {
                binding.root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }

        previousMatchedWords = currentWords
    }


    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun expandRect(rect: MapperRect, padding: Float): MapperRect {
        if (rect.cornerPoints.size == 4) {
            // If we have corner points, expand them relative to the center
            val centerX = (rect.cornerPoints[0].x + rect.cornerPoints[1].x + rect.cornerPoints[2].x + rect.cornerPoints[3].x) / 4f
            val centerY = (rect.cornerPoints[0].y + rect.cornerPoints[1].y + rect.cornerPoints[2].y + rect.cornerPoints[3].y) / 4f

            val expandedPoints = rect.cornerPoints.map { p ->
                val dx = p.x - centerX
                val dy = p.y - centerY
                val length = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                if (length == 0f) p
                else {
                    val scale = (length + padding) / length
                    MapperPoint(centerX + dx * scale, centerY + dy * scale)
                }
            }
            return rect.copy(
                left = rect.left - padding,
                top = rect.top - padding,
                right = rect.right + padding,
                bottom = rect.bottom + padding,
                cornerPoints = expandedPoints
            )
        }
        return MapperRect(
            left = rect.left - padding,
            top = rect.top - padding,
            right = rect.right + padding,
            bottom = rect.bottom + padding
        )
    }

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}