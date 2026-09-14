package com.offgridrescue.app.data.location

import com.offgridrescue.app.domain.CheckInStatus
import com.offgridrescue.app.domain.SosType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import android.util.Log
import com.google.firebase.Timestamp
import java.time.Instant

data class CloudIncidentUpdate(
    val sourceDeviceId: String,
    val sosType: String,
    val checkInStatus: String?,
    val lat: Double?,
    val lng: Double?,
    val accuracy: Float?,
    val timestamp: Instant?
)

interface ResponderRepository {
    fun isAuthorized(): Flow<Boolean>
    fun observeAllIncidents(): Flow<List<CloudIncidentUpdate>>
}

class FirebaseResponderRepository : ResponderRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val tag = "FirebaseResponderRepo"

    override fun isAuthorized(): Flow<Boolean> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(false)
            awaitClose()
            return@callbackFlow
        }

        val listener = firestore.collection("responders").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(false)
                    return@addSnapshotListener
                }
                trySend(snapshot != null && snapshot.exists())
            }
        awaitClose { listener.remove() }
    }

    override fun observeAllIncidents(): Flow<List<CloudIncidentUpdate>> = callbackFlow {
        val listener = firestore.collection("emergencies")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error observing emergencies", error)
                    return@addSnapshotListener
                }
                
                val updates = snapshot?.documents?.mapNotNull { doc ->
                    val sId = doc.getString("sourceDeviceId") ?: return@mapNotNull null
                    val sos = doc.getString("sosType") ?: "GENERAL"
                    
                    // Note: In a real app, we'd fetch sub-collections or use a flat schema for dashboarding.
                    // For MVP Step 15, we'll assume basic metadata is in the document for high-level view.
                    // (Actual location/check-in fetching logic would go here)
                    
                    CloudIncidentUpdate(
                        sourceDeviceId = sId,
                        sosType = sos,
                        checkInStatus = null, // Placeholder: would be fetched from check-ins collection
                        lat = null,
                        lng = null,
                        accuracy = null,
                        timestamp = doc.getTimestamp("startedAt")?.toDate()?.toInstant()
                    )
                } ?: emptyList()
                
                trySend(updates)
            }
        awaitClose { listener.remove() }
    }
}
