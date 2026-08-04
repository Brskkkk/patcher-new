package com.idk.gamecloner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var pickButton: Button
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var patcher: ApkPatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pickButton = findViewById(R.id.pickButton)
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)

        patcher = ApkPatcher(this)

        pickButton.setOnClickListener {
            launchApkPicker()
        }
    }

    private fun launchApkPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/vnd.android.package-archive"
        }
        apkPickerLauncher.launch(intent)
    }

    private val apkPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val apkUri = result.data?.data ?: return@registerForActivityResult
                patchApk(apkUri)
            }
        }

    private fun patchApk(apkUri: Uri) {
        lifecycleScope.launch {
            try {
                statusText.text = "🔧 Patching APK..."
                progressBar.visibility = android.view.View.VISIBLE
                pickButton.isEnabled = false

                val inputFile = File(cacheDir, "input.apk")
                contentResolver.openInputStream(apkUri)?.use { input ->
                    inputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val outputFile = File(getExternalFilesDir(null), "game-patched.apk")
                patcher.patch(inputFile, outputFile)

                statusText.text = "✅ Patching complete!\n\nInstall: game-patched.apk\nLocation: ${outputFile.path}"
                Toast.makeText(this@MainActivity, "APK patched successfully!", Toast.LENGTH_LONG).show()

                inputFile.delete()

            } catch (e: Exception) {
                statusText.text = "❌ Error: ${e.message}"
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = android.view.View.GONE
                pickButton.isEnabled = true
            }
        }
    }
}
