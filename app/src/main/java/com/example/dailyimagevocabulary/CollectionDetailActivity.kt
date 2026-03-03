package com.example.dailyimagevocabulary

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CollectionDetailActivity : AppCompatActivity() {

    private val images = mutableListOf<ImageEntity>()
    private lateinit var adapter: ImageAdapter
    private var collectionId: Int = 0
    private var collectionName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collection_detail)

        collectionId = intent.getIntExtra("collectionId", 0)
        collectionName = intent.getStringExtra("collectionName") ?: ""

        // Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = collectionName

        val recycler = findViewById<RecyclerView>(R.id.recyclerImages)
        val fab = findViewById<FloatingActionButton>(R.id.fabAddImage)

        adapter = ImageAdapter(images)
        recycler.layoutManager = GridLayoutManager(this, 2)
        recycler.adapter = adapter

        loadImages()

        fab.setOnClickListener {
            pickImages()
        }
    }

    // =========================
    // LOAD IMAGES FROM DB
    // =========================
    private fun loadImages() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@CollectionDetailActivity)
            val items = db.dao().getImagesByCollection(collectionId)

            withContext(Dispatchers.Main) {
                images.clear()
                images.addAll(items)
                adapter.notifyDataSetChanged()
            }
        }
    }

    // =========================
    // PICK IMAGES (MULTIPLE)
    // =========================
    private val pickImagesLauncher =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) {
                for (uri in uris) {
                    saveImage(uri)
                }
            }
        }

    private fun pickImages() {
        pickImagesLauncher.launch(arrayOf("image/*"))
    }

    // =========================
    // SAVE IMAGE (COPY + DB)
    // =========================
    private fun saveImage(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {

            val path = copyImageToInternalStorage(uri)
            val fileName = File(path).name

            val image = ImageEntity(
                collectionId = collectionId,
                filePath = path,
                fileName = fileName
            )

            val db = AppDatabase.getDatabase(this@CollectionDetailActivity)
            db.dao().insertImage(image)

            loadImages()
        }
    }

    // =========================
    // COPY FILE TO INTERNAL STORAGE
    // =========================
    private fun copyImageToInternalStorage(uri: Uri): String {

        val extension = getFileExtension(uri)
        val originalFileName = getOriginalFileName(uri) ?: "image_${System.currentTimeMillis()}"

        val input = contentResolver.openInputStream(uri)

        val dir = File(filesDir, "images/collection_$collectionId")
        if (!dir.exists()) dir.mkdirs()

        val fileName = "${originalFileName}.${extension}"
        val file = File(dir, fileName)

        input.use { inp ->
            file.outputStream().use { out ->
                inp?.copyTo(out)
            }
        }

        return file.absolutePath
    }

    // =========================
    // GET ORIGINAL FILE NAME
    // =========================
    private fun getOriginalFileName(uri: Uri): String? {
        // Try to get display name from content resolver
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    val displayName = it.getString(displayNameIndex)
                    if (!displayName.isNullOrBlank()) {
                        // Remove extension from display name
                        return displayName.substringBeforeLast(".")
                    }
                }
            }
        }
        
        // Fallback: try to get from URI path
        val path = uri.path
        if (!path.isNullOrBlank()) {
            val fileName = File(path).name
            if (fileName.isNotBlank()) {
                return fileName.substringBeforeLast(".")
            }
        }
        
        return null
    }

    // =========================
    // GET FILE EXTENSION (IMPORTANT)
    // =========================
    private fun getFileExtension(uri: Uri): String {

        val mime = contentResolver.getType(uri)

        return when (mime) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/avif" -> "avif"
            else -> "jpg"
        }
    }

    // =========================
    // BACK BUTTON
    // =========================
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
