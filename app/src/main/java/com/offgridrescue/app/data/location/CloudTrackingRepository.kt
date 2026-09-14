package com.offgridrescue.app.data.location

import com.offgridrescue.app.domain.CloudSession
import com.offgridrescue.app.domain.SyncResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import android.util.Log

interface CloudTrackingRepository {
    suspend fun createSession(session: CloudSession): SyncResult
    suspend fun syncLocation(sessionId: String, record: LocationRecordEntity): SyncResult
    suspend fun syncCheckIn(sessionId: String, record: CheckInRecordEntity, ownerUid: String): SyncResult
}

class FirebaseTrackingRepository : CloudTrackingRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val tag = "FirebaseTrackingRepo"

    override suspend fun createSession(session: CloudSession): SyncResult {
        val docRef = firestore.collection("emergencies").document(session.cloudSessionId)
        return try {
            docRef.set(session.toMap()).await()
            waitForConfirmation(docRef)
        } catch (e: Exception) {
            classifyException(e)
        }
    }

    override suspend fun syncLocation(sessionId: String, record: LocationRecordEntity): SyncResult {
        val docRef = firestore.collection("emergencies")
            .document(sessionId)
            .collection("locations")
            .document(record.id)
            
        return try {
            val locationMap = mapOf(
                "lat" to record.latitude,
                "lng" to record.longitude,
                "acc" to record.accuracy,
                "timestamp" to Timestamp(record.timestamp / 1000, ((record.timestamp % 1000) * 1000000).toInt())
            )
            
            docRef.set(locationMap).await()
            waitForConfirmation(docRef)
        } catch (e: Exception) {
            classifyException(e)
        }
    }

    override suspend fun syncCheckIn(sessionId: String, record: CheckInRecordEntity, ownerUid: String): SyncResult {
        val docRef = firestore.collection("emergency_check_ins").document(record.id)
        return try {
            val checkInMap = mapOf(
                "ownerUid" to ownerUid,
                "cloudSessionId" to sessionId,
                "status" to record.status.name,
                "timestamp" to Timestamp(record.timestamp / 1000, ((record.timestamp % 1000) * 1000000).toInt())
            )

            docRef.set(checkInMap).await()
            waitForConfirmation(docRef)
        } catch (e: Exception) {
            classifyException(e)
        }
    }

    private suspend fun waitForConfirmation(docRef: DocumentReference): SyncResult = callbackFlow {
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(classifyException(error))
                close()
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists() && !snapshot.metadata.hasPendingWrites()) {
                trySend(SyncResult.SERVER_CONFIRMED)
                close()
            }
        }
        awaitClose { listener.remove() }
    }.first()

    private fun classifyException(e: Exception): SyncResult {
        return when (e) {
            is FirebaseFirestoreException -> {
                when (e.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> SyncResult.PERMISSION_DENIED
                    FirebaseFirestoreException.Code.UNAVAILABLE -> SyncResult.PENDING_OFFLINE
                    else -> SyncResult.UNKNOWN_ERROR
                }
            }
            is java.io.IOException -> SyncResult.NETWORK_ERROR
            else -> SyncResult.UNKNOWN_ERROR
        }
    }
}
