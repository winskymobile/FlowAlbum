package com.example.flowalbum

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.flowalbum.transformationMap
import java.io.File
import kotlin.random.Random

class MainActivity : FragmentActivity() {

    companion object {
        private const val PREFS_NAME = "FlowAlbumPrefs"
        private const val KEY_PHOTO_DIR_URI = "photoDirUri"
    }

    private lateinit var viewPager: ViewPager2
    private val permissionsRequestCode = 101

    private var photos: List<Uri> = emptyList()
    private var isAutoPlay = false
    private var isShuffle = false
    private val autoPlayHandler = Handler(Looper.getMainLooper())
    private lateinit var autoPlayRunnable: Runnable
    private var autoPlayDelay = 3000L // 3 seconds

    private val transformationNames = listOf("None", "Depth", "Zoom Out", "Fade Out", "Random")
    private var currentTransformationIndex = 0

    private val selectDirectoryLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // Persist access permissions.
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            // Save the selected directory URI.
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_PHOTO_DIR_URI, uri.toString()).apply()

            // Reload photos from the newly selected directory.
            loadPhotos()
            Toast.makeText(this, "Folder selected successfully", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.photos_viewpager)

        if (checkPermissions()) {
            loadPhotos()
        } else {
            requestPermissions()
        }

        setupAutoPlay()
        setTransformation()
    }

    private fun setTransformation() {
        val currentTransformationName = transformationNames[currentTransformationIndex]
        if (currentTransformationName == "Random") {
            // Apply a random transformation each time for auto-play, handled in the runnable.
            // For manual scrolls, it will have no transformation.
            viewPager.setPageTransformer(null)
        } else {
            val transformer = transformationMap[currentTransformationName]
            viewPager.setPageTransformer(transformer)
        }
        Toast.makeText(this, "Transition: $currentTransformationName", Toast.LENGTH_SHORT).show()
    }

    private fun cycleTransformation() {
        currentTransformationIndex = (currentTransformationIndex + 1) % transformationNames.size
        setTransformation()
    }

    private fun setupAutoPlay() {
        autoPlayRunnable = Runnable {
            if (photos.isNotEmpty()) {
                if (transformationNames[currentTransformationIndex] == "Random") {
                    // Select a random transformer from the map (excluding the "None" option)
                    val randomTransformer = transformationMap.values.shuffled().first { it != transformationMap["None"] }
                    viewPager.setPageTransformer(randomTransformer)
                }

                val nextItem = if (isShuffle) {
                    Random.nextInt(photos.size)
                } else {
                    (viewPager.currentItem + 1) % photos.size
                }
                viewPager.currentItem = nextItem
            }
            autoPlayHandler.postDelayed(autoPlayRunnable, autoPlayDelay)
        }
    }

    private fun startAutoPlay() {
        if (!isAutoPlay) {
            isAutoPlay = true
            autoPlayHandler.postDelayed(autoPlayRunnable, autoPlayDelay)
            Toast.makeText(this, "Auto-play started", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopAutoPlay() {
        if (isAutoPlay) {
            isAutoPlay = false
            autoPlayHandler.removeCallbacks(autoPlayRunnable)
            // Restore the selected transformation for manual scrolling
            setTransformation()
            Toast.makeText(this, "Auto-play stopped", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleShuffle() {
        isShuffle = !isShuffle
        val shuffleMode = if (isShuffle) "Shuffle" else "Sequential"
        Toast.makeText(this, "Playback mode: $shuffleMode", Toast.LENGTH_SHORT).show()
    }

    private fun checkPermissions(): Boolean {
        // This permission is still needed for the fallback directory.
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            permissionsRequestCode
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionsRequestCode && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadPhotos()
        }
    }

    private fun loadPhotos() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dirUriString = prefs.getString(KEY_PHOTO_DIR_URI, null)

        photos = if (dirUriString != null) {
            loadPhotosFromUri(Uri.parse(dirUriString))
        } else {
            loadPhotosFromDefaultDirectory()
        }

        val adapter = PhotoAdapter(photos)
        viewPager.adapter = adapter
        setTransformation() // Re-apply transformation

        if (photos.isEmpty()) {
            val message = if (dirUriString != null) {
                "No images found in the selected folder. Press the 'Settings' key to choose another folder."
            } else {
                "No images found in default folder. Press the 'Settings' key to choose a folder."
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun loadPhotosFromUri(directoryUri: Uri): List<Uri> {
        val photoList = mutableListOf<Uri>()
        val documentFile = DocumentFile.fromTreeUri(this, directoryUri)
        documentFile?.listFiles()?.forEach { file ->
            if (file.isFile && file.type?.startsWith("image/") == true) {
                photoList.add(file.uri)
            }
        }
        return photoList.sortedBy { it.path } 
    }

    private fun loadPhotosFromDefaultDirectory(): List<Uri> {
        val pictureDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val photoDirectory = File(pictureDir, "FlowAlbum")
        if (!photoDirectory.exists()) {
            photoDirectory.mkdirs()
        }

        if (!photoDirectory.exists() || !photoDirectory.isDirectory) {
            return emptyList()
        }

        val imageFiles = photoDirectory.listFiles { file ->
            file.isFile && (file.extension.equals("jpg", ignoreCase = true) ||
                    file.extension.equals("jpeg", ignoreCase = true) ||
                    file.extension.equals("png", ignoreCase = true))
        }

        return imageFiles?.map { Uri.fromFile(it) }?.sorted() ?: emptyList()
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                cycleTransformation()
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                stopAutoPlay()
                viewPager.currentItem = viewPager.currentItem - 1
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                stopAutoPlay()
                viewPager.currentItem = viewPager.currentItem + 1
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (isAutoPlay) {
                    stopAutoPlay()
                } else {
                    startAutoPlay()
                }
                return true
            }
            KeyEvent.KEYCODE_MENU -> {
                toggleShuffle()
                return true
            }
            KeyEvent.KEYCODE_SETTINGS -> {
                // Launch the directory picker.
                selectDirectoryLauncher.launch(null)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
        stopAutoPlay() // Stop auto-play when the app is not in the foreground
    }

    override fun onBackPressed() {
        showExitConfirmationDialog()
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.exit_confirmation_title)
            .setMessage(R.string.exit_confirmation_message)
            .setPositiveButton(R.string.exit) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }
}