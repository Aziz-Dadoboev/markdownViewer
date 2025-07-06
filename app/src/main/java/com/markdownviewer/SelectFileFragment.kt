package com.markdownviewer

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.markdownviewer.databinding.FragmentSelectFileBinding

class SelectFileFragment : Fragment() {
    private var _binding: FragmentSelectFileBinding? = null
    private val binding get() = _binding!!
    private var downloadId: Long = 0

    private val downloadViewModel: DownloadViewModel by lazy {
        (requireActivity().application as App).downloadViewModel
    }

    private val selectFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            navigateToMarkdown(uri)
        } else {
            Toast.makeText(context, "No file was chosen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToMarkdown(uri: Uri) {
        val markdownFragment = parentFragmentManager.findFragmentByTag("MARKDOWN") as? MarkdownFragment
            ?: MarkdownFragment.newInstance(uri)

        parentFragmentManager.beginTransaction()
            .hide(this)
            .show(markdownFragment)
            .commit()

        markdownFragment.updateContent(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectFileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        downloadViewModel.downloadComplete.observe(viewLifecycleOwner) { fileUri ->
            if (fileUri != null) {
                binding.progressBar.visibility = View.GONE
                navigateToMarkdown(fileUri)
            }
        }

        binding.downloadBtn.setOnClickListener {
            val url = binding.outlinedTextField.editText?.text?.toString()
            if (!url.isNullOrEmpty()) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(context, "Permission needed", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
                
                binding.progressBar.visibility = View.VISIBLE
                downloadId = downloadFile(url)
            } else {
                Toast.makeText(context, "Download error", Toast.LENGTH_SHORT).show()
            }
        }

        binding.selectBtn.setOnClickListener {
            selectFileLauncher.launch("text/*")
        }
    }

    private fun downloadFile(url: String): Long {
        val fileName = Uri.parse(url).lastPathSegment ?: "markdown-file.md"
        
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Markdown file")
            .setDescription("Downloading markdown file")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            .setAllowedOverRoaming(false)
            .setMimeType("text/markdown")

        val downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)
        
        return downloadId
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}