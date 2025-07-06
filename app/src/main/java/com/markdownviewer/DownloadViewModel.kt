package com.markdownviewer

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DownloadViewModel : ViewModel() {
    private val _downloadComplete = MutableLiveData<Uri>()
    val downloadComplete: LiveData<Uri> = _downloadComplete

    private var downloadedContent: String? = null
    private var downloadedUri: Uri? = null

    fun notifyDownloadComplete(fileUri: Uri, content: String) {
        downloadedUri = fileUri
        downloadedContent = content
        _downloadComplete.postValue(fileUri)
    }

}

