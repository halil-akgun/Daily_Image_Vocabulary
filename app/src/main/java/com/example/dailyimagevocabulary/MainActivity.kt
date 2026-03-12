package com.example.dailyimagevocabulary

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: CollectionAdapter
    private val collections = mutableListOf<Pair<CollectionEntity, Int>>()
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, start services
            startNotificationServices()
        } else {
            // Permission denied, show dialog instead of toast
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewCollections)
        val fab = findViewById<FloatingActionButton>(R.id.fabAddCollection)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Collections"

        adapter = CollectionAdapter(collections) { collection ->
            val intent = Intent(this, CollectionDetailActivity::class.java)
            intent.putExtra("collectionId", collection.id)
            intent.putExtra("collectionName", collection.name)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadCollections()

        fab.setOnClickListener {
            showAddCollectionDialog()
        }
        
        // Check notification permission first
        checkNotificationPermission()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh collections when returning from other activities
        loadCollections()
    }
    
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    startNotificationServices()
                }
                else -> {
                    // Request permission
                    showPermissionRationaleDialog()
                }
            }
        } else {
            // Android version below 13, no need for permission
            startNotificationServices()
        }
    }
    
    private fun showPermissionRationaleDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Notification Permission Required")
            .setMessage("This app needs notification permission to show you daily vocabulary images. Please grant the permission to continue.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("Cancel") { _, _ ->
                // Show info dialog instead of toast
                showPermissionDeniedDialog()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showPermissionDeniedDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Permission Denied")
            .setMessage("Without notification permission, you won't receive daily vocabulary reminders. You can enable it later in Settings.")
            .setPositiveButton("OK") { _, _ ->
                // Just close the dialog
            }
            .setCancelable(true)
            .show()
    }
    
    private fun startNotificationServices() {
        // Check battery optimization
        checkBatteryOptimization()
        
        // Schedule daily notifications
        WorkManagerScheduler.scheduleDailyNotifications(this)
        
        // Start persistent notification service
        PersistentNotificationService.startService(this)
    }
    
    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = packageName
            
            val isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName)
            
            if (!isIgnoringBatteryOptimizations) {
                showBatteryOptimizationDialog()
            }
        }
    }
    
    private fun showBatteryOptimizationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Battery Optimization")
            .setMessage("For reliable notifications, please disable battery optimization for this app.")
            .setPositiveButton("Settings") { _, _ ->
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = Uri.parse("package:$packageName")
                }
                startActivityForResult(intent, 1001)
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_select_collection -> {
                showCollectionSelectionDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showCollectionSelectionDialog() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@MainActivity)
            val collections = db.dao().getCollections()
            
            withContext(Dispatchers.Main) {
                val collectionNames = collections.map { it.name }.toTypedArray()
                val currentSelection = getSharedPreferences("app", MODE_PRIVATE)
                    .getInt("selectedCollectionId", 0)
                
                var selectedIndex = collections.indexOfFirst { it.id == currentSelection }
                if (selectedIndex == -1) selectedIndex = 0
                
                androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("Select Collection for Notifications")
                    .setSingleChoiceItems(collectionNames, selectedIndex) { dialog, which ->
                        val selectedId = collections[which].id
                        getSharedPreferences("app", MODE_PRIVATE)
                            .edit()
                            .putInt("selectedCollectionId", selectedId)
                            .putInt("index", 0) // Reset index when collection changes
                            .apply()
                        
                        // Refresh notification immediately
                        refreshNotification()
                        
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }
    
    private fun refreshNotification() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@MainActivity)
            val dao = db.dao()
            
            val prefs = getSharedPreferences("app", MODE_PRIVATE)
            val collectionId = prefs.getInt("selectedCollectionId", 0)
            
            val images = dao.getImagesByCollection(collectionId)
            if (images.isNotEmpty()) {
                val index = prefs.getInt("index", 0)
                val image = images.getOrNull(index) ?: images.first()
                
                // Update notification
                NotificationHelper.showNotification(this@MainActivity, image)
                
                // Also update persistent service
                PersistentNotificationService.stopService(this@MainActivity)
                PersistentNotificationService.startService(this@MainActivity)
            }
        }
    }

    private fun loadCollections() {
        lifecycleScope.launch(Dispatchers.IO) {

            val db = AppDatabase.getDatabase(this@MainActivity)
            val dao = db.dao()

            val items = dao.getCollections()

            val listWithCount = mutableListOf<Pair<CollectionEntity, Int>>()

            for (c in items) {
                val count = dao.getImageCount(c.id) // <-- BURASI ÖNEMLİ
                listWithCount.add(Pair(c, count))
            }

            withContext(Dispatchers.Main) {
                collections.clear()
                collections.addAll(listWithCount)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun showAddCollectionDialog() {
        val editText = EditText(this)
        editText.hint = "Collection name"

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("New Collection")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) addCollectionToDb(name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addCollectionToDb(name: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@MainActivity)
            db.dao().insertCollection(CollectionEntity(name = name))

            loadCollections() // reload
        }
    }
}
