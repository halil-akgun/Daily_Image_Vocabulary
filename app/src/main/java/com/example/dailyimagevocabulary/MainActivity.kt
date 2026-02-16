package com.example.dailyimagevocabulary

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: CollectionAdapter
    private val collections = mutableListOf<Pair<CollectionEntity, Int>>()

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
