package com.example.qrshieldapp

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "FirebaseRepository"

    private fun historyCollection(uid: String) = db.collection("users").document(uid).collection("history")

    suspend fun saveHistoryItem(uid: String, item: HistoryItem): String? {
        return try {
            val data = hashMapOf(
                "url" to item.url,
                "isMalicious" to item.isMalicious,
                "mlScore" to item.mlScore,
                "apiResult" to item.apiResult,
                "geminiCategory" to item.geminiCategory,
                "geminiSummary" to item.geminiSummary,
                "userId" to uid,
                "timestamp" to Timestamp.now()
            )
            val ref = historyCollection(uid).add(data).await()
            Log.d(TAG, "Saved history doc: ${ref.id}")
            ref.id
        } catch (e: Exception) {
            Log.e(TAG, "saveHistoryItem error: ${e.message}", e)
            null
        }
    }

    fun startHistoryListener(uid: String, onUpdate: (List<HistoryItem>) -> Unit): ListenerRegistration {
        val q = historyCollection(uid).orderBy("timestamp", Query.Direction.DESCENDING).limit(200)
        val reg = q.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "History listener error", error)
                onUpdate(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(HistoryItem::class.java)?.apply { id = doc.id }
                }
                onUpdate(items)
            } else {
                onUpdate(emptyList())
            }
        }
        return reg
    }
}
