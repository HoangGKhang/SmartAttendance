package com.example.smartattendance.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

@Serializable
data class UserFaceEmbedding(
    @SerialName("embedding_id")
    val embeddingId: String? = null,

    @SerialName("user_id")
    val userId: String,

    val embedding: JsonArray,

    @SerialName("model_name")
    val modelName: String? = "face_model.tflite",

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
)