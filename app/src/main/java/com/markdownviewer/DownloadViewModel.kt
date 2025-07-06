package com.markdownviewer

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DownloadViewModel : ViewModel() {
    private val _downloadComplete = MutableLiveData<Uri>()
    val downloadComplete: LiveData<Uri> = _downloadComplete

    private val _fileSelected = MutableLiveData<Uri>()
    val fileSelected: LiveData<Uri> = _fileSelected

    private var downloadedContent: String? = null
    private var downloadedUri: Uri? = null

    fun notifyDownloadComplete(fileUri: Uri, content: String) {
        downloadedUri = fileUri
        downloadedContent = content
        _downloadComplete.postValue(fileUri)
    }
    fun notifyFileSelected(fileUri: Uri) {
        _fileSelected.postValue(fileUri)
    }

    fun getDownloadedContent(): String? {
        return downloadedContent
    }

    fun getDownloadedUri(): Uri? {
        return downloadedUri
    }
}

