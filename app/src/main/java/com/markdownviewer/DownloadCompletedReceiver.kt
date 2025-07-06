package com.markdownviewer

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

class DownloadCompletedReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE && context != null) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)

            if (id != -1L) {
                val downloadManager =
                    context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val query = DownloadManager.Query().setFilterById(id)
                val cursor = downloadManager.query(query)

                if (cursor.moveToFirst()) {
                    val statusColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val uriColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)

                    if (statusColumnIndex != -1 && uriColumnIndex != -1) {
                        val status = cursor.getInt(statusColumnIndex)

                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            val uriString = cursor.getString(uriColumnIndex)
                            val fileUri = Uri.parse(uriString)
                            val localUriColumnIndex =
                                cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            if (localUriColumnIndex != -1) {
                                val localUriString = cursor.getString(localUriColumnIndex)
                                Log.d("DownloadReceiver", "File saved at: $localUriString")
                            }

                            val content = readFileFromUri(context, fileUri)
                            Log.d(
                                "DownloadReceiver",
                                "File content read successfully, length: ${content.length}"
                            )

                            try {
                                val app = context.applicationContext as App
                                app.downloadViewModel.notifyDownloadComplete(fileUri, content)
                            } catch (e: Exception) {
                                Log.e("DownloadReceiver", "Error notifying ViewModel", e)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun readFileFromUri(context: Context, uri: Uri): String {
        val contentResolver: ContentResolver = context.contentResolver
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
}