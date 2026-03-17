package com.example.dailyimagevocabulary

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ImageDetailActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var titleText: TextView
    private lateinit var previousButton: Button
    private lateinit var nextButton: Button
    private lateinit var deleteButton: Button
    
    private var currentImage: ImageEntity? = null
    private var allImages: List<ImageEntity> = emptyList()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_detail)

        // Test toast
        android.widget.Toast.makeText(this, "ImageDetailActivity opened!", android.widget.Toast.LENGTH_SHORT).show()

        imageView = findViewById(R.id.imageView)
        titleText = findViewById(R.id.titleText)
        previousButton = findViewById(R.id.previousButton)
        nextButton = findViewById(R.id.nextButton)
        deleteButton = findViewById(R.id.deleteButton)

        // Get image data from intent
        val imageId = intent.getIntExtra("image_id", 0)
        val imagePath = intent.getStringExtra("image_path") ?: ""
        val imageName = intent.getStringExtra("image_name") ?: ""

        // Debug info
        android.util.Log.d("ImageDetail", "Received: id=$imageId, path=$imagePath, name=$imageName")

        // Check if we have valid data
        if (imagePath.isEmpty() || !File(imagePath).exists()) {
            android.util.Log.e("ImageDetail", "Invalid image path or file doesn't exist")
            finish()
            return
        }

        // Load image
        loadImage(imagePath, imageName)

        // Set up buttons
        previousButton.setOnClickListener {
            changeToPreviousImage()
        }

        nextButton.setOnClickListener {
            changeToNextImage()
        }

        deleteButton.setOnClickListener {
            showDeleteConfirmation()
        }

        // Load all images for navigation
        loadAllImages()
    }

    private fun loadImage(imagePath: String, imageName: String) {
        titleText.text = imageName.substringBeforeLast(".")
        
        val bitmap = BitmapFactory.decodeFile(imagePath)
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap)
        }
    }

    private fun loadAllImages() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@ImageDetailActivity)
            val dao = db.dao()
            
            val prefs = getSharedPreferences("app", MODE_PRIVATE)
            val collectionId = prefs.getInt("selectedCollectionId", 0)
            
            allImages = dao.getImagesByCollection(collectionId)
            
            // Find current image index
            val imageId = intent.getIntExtra("image_id", 0)
            currentIndex = allImages.indexOfFirst { it.id == imageId }
            if (currentIndex == -1) currentIndex = 0
            
            currentImage = allImages.getOrNull(currentIndex)
        }
    }

    private fun changeToNextImage() {
        if (allImages.isNotEmpty()) {
            currentIndex = (currentIndex + 1) % allImages.size
            currentImage = allImages[currentIndex]
            
            currentImage?.let { image ->
                loadImage(image.filePath, image.fileName)
                
                // Update notification
                NotificationHelper.showNotification(this@ImageDetailActivity, image)
                
                // Update shared preferences
                getSharedPreferences("app", MODE_PRIVATE)
                    .edit()
                    .putInt("index", currentIndex)
                    .apply()
            }
        }
    }

    private fun changeToPreviousImage() {
        if (allImages.isNotEmpty()) {
            currentIndex = if (currentIndex == 0) allImages.size - 1 else currentIndex - 1
            currentImage = allImages[currentIndex]
            
            currentImage?.let { image ->
                loadImage(image.filePath, image.fileName)
                
                // Update notification
                NotificationHelper.showNotification(this@ImageDetailActivity, image)
                
                // Update shared preferences
                getSharedPreferences("app", MODE_PRIVATE)
                    .edit()
                    .putInt("index", currentIndex)
                    .apply()
            }
        }
    }

    private fun showDeleteConfirmation() {
        currentImage?.let { image ->
            AlertDialog.Builder(this)
                .setTitle("Delete Image")
                .setMessage("Are you sure you want to delete this image?")
                .setPositiveButton("Delete") { _, _ ->
                    deleteImage(image)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun deleteImage(image: ImageEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@ImageDetailActivity)
            val dao = db.dao()
            
            // Delete from database
            dao.deleteImage(image)
            
            // Delete file
            try {
                File(image.filePath).delete()
            } catch (_: Exception) {
                // Handle file deletion error
            }
            
            // Reload images
            allImages = dao.getImagesByCollection(image.collectionId)
            
            runOnUiThread {
                if (allImages.isEmpty()) {
                    // No more images, close activity
                    finish()
                } else {
                    // Adjust currentIndex if necessary (if deleted item was before current index)
                    if (currentIndex >= allImages.size) {
                        currentIndex = 0 // Wrap around if we were at the end
                    }
                    
                    currentImage = allImages.getOrNull(currentIndex)
                    currentImage?.let { img ->
                        loadImage(img.filePath, img.fileName)
                        NotificationHelper.showNotification(this@ImageDetailActivity, img)
                        
                        // Update shared preferences with new index
                        getSharedPreferences("app", MODE_PRIVATE)
                            .edit()
                            .putInt("index", currentIndex)
                            .apply()
                    }
                }
            }
        }
    }
}
