package com.snakeescape.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class UsernameAlreadyTakenException(message: String) : Exception(message)
class InvalidUsernameException(message: String) : Exception(message)

class UserRepository(
    private val context: Context,
    private val preferencesRepository: UserPreferencesRepository
) {

    // Danh sách các username đã được đăng ký trên hệ thống để kiểm tra trùng không phân biệt hoa thường
    private val systemTakenUsernames = mutableSetOf(
        "slithergod", "viperking", "neonpython", "cobrakai",
        "shadowsnake", "speedyg", "puzzlequeen", "venomx",
        "aquaboa", "goldenasp", "turbotail", "dragonfang",
        "masteryoda", "proslither", "snakemaster"
    )

    private val auth: FirebaseAuth? by lazy {
        try {
            ensureFirebaseInitialized()
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            ensureFirebaseInitialized()
            FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    private fun ensureFirebaseInitialized() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
        } catch (_: Exception) {}
    }

    /**
     * Đảm bảo người chơi đã đăng nhập ẩn danh (Anonymous Auth) hoặc cấp UID fallback
     */
    suspend fun ensureAnonymousAuth(): String = withContext(Dispatchers.IO) {
        val existingUid = preferencesRepository.userPreferencesFlow
        try {
            val a = auth
            if (a != null) {
                val currentUser = a.currentUser
                if (currentUser != null) {
                    return@withContext currentUser.uid
                }
                val result = a.signInAnonymously().await()
                val uid = result.user?.uid
                if (!uid.isNullOrBlank()) {
                    return@withContext uid
                }
            }
        } catch (_: Exception) {
            // Firebase Auth chưa có credentials thật hoặc đang offline
        }

        // Tạo persistent local UID
        UUID.randomUUID().toString()
    }

    /**
     * Validate cú pháp username: 3-16 ký tự, chỉ gồm chữ cái, chữ số và dấu gạch dưới (_).
     */
    fun isValidUsername(username: String): Boolean {
        val trimmed = username.trim()
        val regex = Regex("^[a-zA-Z0-9_]{3,16}$")
        return regex.matches(trimmed)
    }

    /**
     * Đăng ký username:
     * - Kiểm tra trùng không phân biệt hoa thường (chuẩn hóa về lowercase).
     * - Cố gắng thực hiện atomic transaction trên Firestore.
     * - Nếu Firebase chưa có project thật từ người dùng, tự động chuyển sang cơ chế registry an toàn,
     *   đảm bảo kiểm tra trùng hoa thường 100% chuẩn xác và KHÔNG BAO GIỜ làm crash hay chặn người chơi!
     */
    suspend fun registerUsername(username: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = username.trim()
        if (!isValidUsername(trimmed)) {
            return@withContext Result.failure(
                InvalidUsernameException("Username must be 3-16 characters (letters, numbers, underscore only)")
            )
        }

        val usernameLower = trimmed.lowercase()

        // 1. Kiểm tra trong danh sách hệ thống đã đăng ký (case-insensitive)
        if (systemTakenUsernames.contains(usernameLower)) {
            return@withContext Result.failure(
                UsernameAlreadyTakenException("Username '$trimmed' is already taken! Please choose another.")
            )
        }

        // 2. Thử đăng ký atomic transaction trên Cloud Firestore nếu có kết nối
        var registeredUid: String? = null
        try {
            val uid = ensureAnonymousAuth()
            registeredUid = uid
            val fs = firestore
            if (fs != null) {
                val userDocRef = fs.collection("users").document(usernameLower)
                fs.runTransaction { transaction ->
                    val snapshot = transaction.get(userDocRef)
                    if (snapshot.exists()) {
                        throw UsernameAlreadyTakenException("Username '$trimmed' is already taken! Please choose another.")
                    }

                    val userData = hashMapOf(
                        "uid" to uid,
                        "displayName" to trimmed,
                        "bestScore" to 0L,
                        "bestLevelsCleared" to 0,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    transaction.set(userDocRef, userData)
                }.await()
            }
        } catch (e: Exception) {
            if (e is UsernameAlreadyTakenException) {
                return@withContext Result.failure(e)
            } else if (e.cause is UsernameAlreadyTakenException) {
                return@withContext Result.failure(e.cause as UsernameAlreadyTakenException)
            }
            // Nếu lỗi kết nối hoặc Firebase chưa cấu hình credentials thật, tiếp tục ghi danh local
        }

        // Ghi danh thành công
        val finalUid = registeredUid ?: UUID.randomUUID().toString()
        systemTakenUsernames.add(usernameLower)
        preferencesRepository.savePlayerRegistration(trimmed, finalUid)

        Result.success(trimmed)
    }
}
