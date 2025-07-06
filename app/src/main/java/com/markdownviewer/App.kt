package com.markdownviewer

import android.app.Application

class App : Application() {
    lateinit var downloadViewModel: DownloadViewModel
        private set

    override fun onCreate() {
        super.onCreate()
        downloadViewModel = DownloadViewModel()
    }
} 