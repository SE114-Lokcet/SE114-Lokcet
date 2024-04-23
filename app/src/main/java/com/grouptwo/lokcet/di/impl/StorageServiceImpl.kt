package com.grouptwo.lokcet.di.impl

import android.icu.text.SimpleDateFormat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.grouptwo.lokcet.data.model.UploadImage
import com.grouptwo.lokcet.di.service.StorageService
import com.grouptwo.lokcet.utils.DataState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class StorageServiceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : StorageService {
    @OptIn(ExperimentalCoroutinesApi::class)
    override val images: Flow<List<UploadImage>> get() = emptyFlow()
    override suspend fun uploadImage(
        imageUpload: ByteArray,
        imageCaption: String,
        visibleUserIds: List<String>? // null if visible to all
    ): Flow<DataState<Unit>> = flow {
        emit(DataState.Loading)
        try {
            val storageService = storage.reference
            // Create image name
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageName = "IMG_${auth.currentUser?.uid}_$timestamp"
            // Create image reference
            val imageRef = storageService.child("images/$imageName")
            // Upload image to Firebase Storage
            imageRef.putBytes(imageUpload).await()
            // Get download URL
            val downloadUrl = imageRef.downloadUrl.await()
            // Create image object
            val image = UploadImage(
                userId = auth.currentUser?.uid.orEmpty(),
                imageUrl = downloadUrl.toString(),
                imageCaption = imageCaption,
                visibleUserIds = visibleUserIds,
                createdAt = FieldValue.serverTimestamp()
            )
            // Save image to Firestore
            firestore.collection("images").add(image).await()
            // Add image to user's uploadImageList
            firestore.collection("users").document(auth.currentUser?.uid.orEmpty())
                .update("uploadImageList", FieldValue.arrayUnion(image.imageUrl)).await()
            emit(DataState.Success(Unit))
        } catch (e: Exception) {
            // Handle any other exceptions
            emit(DataState.Error(e))
        }
    }

    override suspend fun getImages(): List<UploadImage> {
//        return emptyList()
        // Get friends list from current user

        val friends =
            firestore.collection("users").document(auth.currentUser?.uid.orEmpty()).get()
                .await()
                .get("friends") as List<String>
        // Get images from friends and sort by createdAt field to descending order
        return firestore.collection("images")
            .whereIn("userId", friends)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(UploadImage::class.java)
    }

    override suspend fun deleteImage(imageId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun getImagesUploadByUser(userId: String): List<UploadImage> {
        TODO("Not yet implemented")
    }

}