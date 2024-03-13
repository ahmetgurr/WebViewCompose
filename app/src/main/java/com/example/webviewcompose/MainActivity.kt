package com.example.webviewcompose

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private var camFileData: String? = null
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private val fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val intent = result.data
        val resultCode = result.resultCode
        if (resultCode == RESULT_OK) {
            val resultData = intent?.data
            val resultClipData = intent?.clipData
            val results = when {
                resultClipData != null -> {
                    val uris = mutableListOf<Uri>()
                    for (i in 0 until resultClipData.itemCount) {
                        uris.add(resultClipData.getItemAt(i).uri)
                    }
                    uris.toTypedArray()
                }
                resultData != null -> arrayOf(resultData)
                camFileData != null -> arrayOf(Uri.parse(camFileData))
                else -> null
            }
            fileCallback?.onReceiveValue(results)
            fileCallback = null
        } else {
            fileCallback?.onReceiveValue(null)
            fileCallback = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WebViewScreen()
        }
    }

    @Composable
    fun WebViewScreen() {
        var webView: WebView? = null

        Surface(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        loadUrl("https://www.simpleimageresizer.com/image-converter")
                        webView = this
                        webChromeClient = object : WebChromeClient() {
                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                fileCallback = filePathCallback
                                showFileChooser()
                                return true
                            }
                        }
                    }
                },
                update = { it ->
                    webView = it
                }
            )
        }
    }

    private fun showFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val packageManager = packageManager
        val captureActivities = packageManager.queryIntentActivities(captureIntent, PackageManager.MATCH_DEFAULT_ONLY)
        for (activity in captureActivities) {
            val intentCamera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                null
            }
            if (photoFile != null) {
                val photoUri = FileProvider.getUriForFile(this, "$packageName.provider", photoFile)
                intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                camFileData = photoUri.toString()
                intentCamera.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                intentCamera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            }
        }

        val chooserIntent = Intent(Intent.ACTION_CHOOSER).apply {
            putExtra(Intent.EXTRA_INTENT, intent)
            putExtra(Intent.EXTRA_TITLE, "File chooser")
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(captureIntent))
        }

        fileChooserLauncher.launch(chooserIntent)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }
}
