package com.example.qrshieldapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class HistoryActivity : AppCompatActivity() {

    private val TAG = "HistoryActivity"
    private var listener: ListenerRegistration? = null
    private lateinit var adapter: HistoryAdapter
    private lateinit var rvHistory: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        rvHistory = findViewById(R.id.rvHistory)
        rvHistory.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(emptyList()) { item ->
            // onClick: open ResultActivity or direct URL
            val intent = Intent(this, ResultActivity::class.java)
            intent.putExtra("SCANNED_URL", item.url)
            intent.putExtra("IS_MALICIOUS", item.isMalicious)
            startActivity(intent)
        }
        rvHistory.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.d(TAG, "User not signed in - history empty")
            adapter.update(emptyList())
            return
        }
        val uid = user.uid
        try {
            listener = FirebaseRepository().startHistoryListener(uid) { items ->
                runOnUiThread {
                    adapter.update(items)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start history listener: ${e.message}", e)
        }
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
        listener = null
    }
}
