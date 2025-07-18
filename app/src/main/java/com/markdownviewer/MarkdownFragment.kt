package com.markdownviewer

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.markdownviewer.databinding.FragmentMarkdownBinding
import java.io.BufferedReader
import java.io.InputStreamReader

class MarkdownFragment : Fragment() {
    private var _binding: FragmentMarkdownBinding? = null
    private val binding get() = _binding!!
    private var currentMarkdown: String = ""

    companion object {
        private const val ARG_FILE_URI = "file_uri"
        
        fun newInstance(fileUri: Uri): MarkdownFragment {
            val fragment = MarkdownFragment()
            val args = Bundle()
            args.putParcelable(ARG_FILE_URI, fileUri)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarkdownBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val fileUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable<Uri>(ARG_FILE_URI, Uri::class.java)
        } else {
            arguments?.getParcelable<Uri>(ARG_FILE_URI)
        }

        val editButton = binding.editButton
        val editText = binding.editTextMarkdown
        val mdView = binding.mdView
        if (fileUri != null) {
            try {
                currentMarkdown = readFileFromUri(fileUri)
                if (currentMarkdown.isNotEmpty()) {
                    mdView.setMarkdown(currentMarkdown)
                    editText.setText(currentMarkdown)
                    val app = requireActivity().application as App
                    app.downloadViewModel.notifyDownloadComplete(fileUri, currentMarkdown)
                } else {
                    Toast.makeText(context, "File error", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "File read error:: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "File URI not found", Toast.LENGTH_SHORT).show()
        }

        editButton.setOnClickListener {
            mdView.visibility = View.GONE
            editText.visibility = View.VISIBLE
            editButton.visibility = View.GONE
            editText.setText(currentMarkdown)
            binding.saveButton.visibility = View.VISIBLE
        }

        binding.saveButton.setOnClickListener {
            val newText = editText.text.toString()
            currentMarkdown = newText
            mdView.setMarkdown(newText)
            mdView.visibility = View.VISIBLE
            editText.visibility = View.GONE
            binding.saveButton.visibility = View.GONE
            editButton.visibility = View.VISIBLE
        }
    }

    private fun readFileFromUri(uri: Uri): String {
        val contentResolver: ContentResolver = requireContext().contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        
        return inputStream?.use { stream ->
            val reader = BufferedReader(InputStreamReader(stream))
            val stringBuilder = StringBuilder()
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line).append('\n')
            }
            
            stringBuilder.toString()
        } ?: ""
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    fun updateContent(fileUri: Uri) {
        try {
            currentMarkdown = readFileFromUri(fileUri)
            if (currentMarkdown.isNotEmpty()) {
                binding.mdView.setMarkdown(currentMarkdown)
                binding.editTextMarkdown.setText(currentMarkdown)
            } else {
                Toast.makeText(context, "File error", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "File read error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}