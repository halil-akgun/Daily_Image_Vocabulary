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
        
        // Check auto-start on first launch or when needed
        checkAutoStartIfNeeded()
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
        AlertDialog.Builder(this)
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
        AlertDialog.Builder(this)
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

    private fun checkAutoStart() {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()

        when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || brand.contains("xiaomi") || brand.contains("redmi") -> {
                showAutoStartDialog("Settings → Apps → Permissions → Background Autostart → Enable ${getString(R.string.app_name)}")
            }
            manufacturer.contains("huawei") || brand.contains("huawei") || manufacturer.contains("honor") || brand.contains("honor") -> {
                showAutoStartDialog("Settings → Apps → ${getString(R.string.app_name)} → Protected Apps → Enable")
            }
            manufacturer.contains("samsung") || brand.contains("samsung") -> {
                showAutoStartDialog("Settings → Apps → ${getString(R.string.app_name)} → Battery → Allow background activity")
            }
            manufacturer.contains("oneplus") || brand.contains("oneplus") || manufacturer.contains("oppo") || brand.contains("oppo") ||
            manufacturer.contains("realme") || brand.contains("realme") -> {
                showAutoStartDialog("Settings → Battery → App Optimization → ${getString(R.string.app_name)} → Don't optimize")
            }
            manufacturer.contains("vivo") || brand.contains("vivo") -> {
                showAutoStartDialog("Settings → iManager → App Auto-start → ${getString(R.string.app_name)} → Enable")
            }
            manufacturer.contains("asus") || brand.contains("asus") -> {
                showAutoStartDialog("Settings → Power Manager → Auto-start Manager → ${getString(R.string.app_name)} → Enable")
            }
        }
    }

    private fun showAutoStartDialog(instructions: String) {
        AlertDialog.Builder(this)
            .setTitle("Enable Auto Start")
            .setMessage(
                "To ensure daily vocabulary notifications work reliably, please allow this app to run in background and enable Auto-start.\n\n" +
                        "How to enable it:\n$instructions"
            )
            .setPositiveButton("Open Settings") { _, _ ->
                AutoStartHelper.openAutoStartSettings(this)
            }
            .setNegativeButton("Later", null)
            .setNeutralButton("Don't show again") { _, _ ->
                val prefs = getSharedPreferences("auto_start_prefs", MODE_PRIVATE)
                prefs.edit().putBoolean("permanently_dismissed", true).apply()
            }
            .setCancelable(false)
            .show()
    }

    private fun openManufacturerSettings() {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val intent = when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                // Xiaomi Auto-start settings - try multiple approaches
                try {
                    // Try 1: Direct auto-start permission
                    Intent().apply {
                        action = "com.miui.securitycenter.action.AppPermissionEditor"
                        putExtra("extra_pkgname", packageName)
                        putExtra("extra_permission_name", "auto_start")
                    }
                } catch (_: Exception) {
                    try {
                        // Try 2: Security center main
                        Intent().apply {
                            action = "com.miui.securitycenter.action.MAIN"
                            putExtra("open_page", "app_permission_editor")
                            putExtra("package_name", packageName)
                        }
                    } catch (_: Exception) {
                        // Try 3: General app settings
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    }
                }
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                // Huawei Protected Apps
                Intent().apply {
                    action = "huawei.intent.action.HWAPPS"
                    putExtra("packageName", packageName)
                }
            }
            manufacturer.contains("samsung") -> {
                // Samsung Battery optimization
                Intent().apply {
                    action = "android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
                    data = Uri.parse("package:$packageName")
                }
            }
            manufacturer.contains("oneplus") || manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
                // OnePlus/Oppo/Realme App optimization
                Intent().apply {
                    action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                    data = Uri.parse("package:$packageName")
                }
            }
            manufacturer.contains("vivo") -> {
                // Vivo iManager
                Intent().apply {
                    action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                    data = Uri.parse("package:$packageName")
                }
            }
            manufacturer.contains("asus") -> {
                // Asus Auto-start Manager
                Intent().apply {
                    action = "com.asus.mobilemanager.action.MAIN"
                }
            }
            else -> {
                // Fallback to general app settings
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
            }
        }

        try {
            startActivity(intent)
        } catch (_: Exception) {
            // Fallback to general app settings if specific intent fails
            openAppSettings()
        }
    }

    private fun checkAutoStartIfNeeded() {
        val prefs = getSharedPreferences("auto_start_prefs", MODE_PRIVATE)
        val permanentlyDismissed = prefs.getBoolean("permanently_dismissed", false)
        
        // Don't show if user permanently dismissed
        if (permanentlyDismissed) {
            return
        }

        checkAutoStart()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
    
    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
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
                
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Select Collection for Notifications")
                    .setSingleChoiceItems(collectionNames, selectedIndex) { dialog, which ->
                        val selectedId = collections[which].id
                        getSharedPreferences("app", MODE_PRIVATE)
                            .edit()
                            .putInt("selectedCollectionId", selectedId)
                            .putInt("index", 0) // Reset index when collection changes
                            .apply()
                        
                        // Refresh notification immediately
                        refreshNotification(this@MainActivity)
                        
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    companion object {
        fun refreshNotification(context: Context) {
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val image = NotificationHelper.getCurrentImage(context)
                if (image != null) {
                    NotificationHelper.showNotification(context, image)
                    
                    // Also update persistent service
                    PersistentNotificationService.stopService(context)
                    PersistentNotificationService.startService(context)
                }
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

        AlertDialog.Builder(this)
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
