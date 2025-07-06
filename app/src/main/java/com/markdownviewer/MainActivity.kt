package com.markdownviewer

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            val selectFileFragment = SelectFileFragment()
            val markdownFragment = MarkdownFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, selectFileFragment, "SELECT_FILE")
                .add(R.id.fragment_container, markdownFragment, "MARKDOWN")
                .hide(markdownFragment)
                .commit()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

                if (currentFragment is MarkdownFragment) {
                    supportFragmentManager.beginTransaction()
                        .hide(currentFragment)
                        .show(supportFragmentManager.findFragmentByTag("SELECT_FILE")!!)
                        .commit()
                } else {
                    finish()
                }
            }
        })
    }
}