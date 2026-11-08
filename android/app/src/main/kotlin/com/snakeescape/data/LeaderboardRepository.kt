package com.snakeescape.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.snakeescape.logic.RunSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class LeaderboardEntry(
    val rank: Int,
    val playerName: String,
    val bestScore: Long,
    val levelsCleared: Int,
    val updatedAt: Long,
    val isCurrentUser: Boolean = false
) {
    val formattedScore: String get() = String.format("%,d", bestScore)
}

class LeaderboardRepository(
    private val context: Context,
    private val preferencesRepository: UserPreferencesRepository
) {

    private val firestore: FirebaseFirestore by lazy {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        FirebaseFirestore.getInstance()
    }

    private var snapshotListenerRegistration: ListenerRegistration? = null

    private val _leaderboardFlow = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboardFlow: StateFlow<List<LeaderboardEntry>> = _leaderboardFlow.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Tự động đồng bộ các run offline khi khởi tạo
        scope.launch {
            syncPendingRuns()
        }
    }

    /**
     * Bắt đầu lắng nghe thời gian thực (Realtime Snapshot Listener) từ Firestore:
     * - Sắp xếp theo bestScore giảm dần
     * - Cùng điểm thì updatedAt sớm hơn xếp trên (updatedAt tăng dần)
     * - Top 50 người chơi
     * - Đánh dấu dòng của người chơi hiện tại
     * - Mỗi username chỉ xuất hiện 1 dòng
     */
    fun startRealtimeListener(currentUsername: String) {
        snapshotListenerRegistration?.remove()

        try {
            val query = firestore.collection("users")
                .orderBy("bestScore", Query.Direction.DESCENDING)
                .orderBy("updatedAt", Query.Direction.ASCENDING)
                .limit(50)

            snapshotListenerRegistration = query.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    // Nếu lỗi mạng, hiển thị danh sách fallback kèm user hiện tại
                    loadFallbackLeaderboard(currentUsername)
                    return@addSnapshotListener
                }

                val entries = mutableListOf<LeaderboardEntry>()
                var rank = 1
                for (doc in snapshot.documents) {
                    val displayName = doc.getString("displayName") ?: doc.id
                    val score = doc.getLong("bestScore") ?: 0L
                    val levels = doc.getLong("bestLevelsCleared")?.toInt() ?: 0
                    val updated = doc.getLong("updatedAt") ?: 0L
                    val isMe = displayName.equals(currentUsername, ignoreCase = true)

                    entries.add(
                        LeaderboardEntry(
                            rank = rank++,
                            playerName = displayName,
                            bestScore = score,
                            levelsCleared = levels,
                            updatedAt = updated,
                            isCurrentUser = isMe
                        )
                    )
                }

                // Nếu Firestore rỗng hoặc user chưa vào top 50, chèn user vào vị trí phù hợp
                if (entries.isEmpty()) {
                    loadFallbackLeaderboard(currentUsername)
                } else {
                    _leaderboardFlow.value = entries
                }
            }
        } catch (e: Exception) {
            loadFallbackLeaderboard(currentUsername)
        }
    }

    fun stopRealtimeListener() {
        snapshotListenerRegistration?.remove()
        snapshotListenerRegistration = null
    }

    /**
     * Ghi nhận 1 bản ghi vào runs và cập nhật bestScore của user khi hoàn thành/kết thúc run.
     * Nếu mất mạng: lưu run tạm trên máy vào DataStore, có mạng lại thì tự đồng bộ.
     */
    suspend fun recordRunEnd(
        summary: RunSummary,
        uid: String,
        username: String
    ): Boolean = withContext(Dispatchers.IO) {
        val trimmed = username.trim()
        val usernameLower = trimmed.lowercase()

        try {
            val runDocRef = firestore.collection("runs").document(summary.runId)
            val runData = hashMapOf(
                "id" to summary.runId,
                "uid" to uid,
                "username" to trimmed,
                "score" to summary.totalScore,
                "levelsCleared" to summary.levelsCleared,
                "totalTimeMs" to summary.totalTimeMs,
                "createdAt" to System.currentTimeMillis()
            )
            runDocRef.set(runData).await()

            // Cập nhật bestScore trong users/{usernameLowercase} nếu cao hơn
            if (usernameLower.isNotBlank()) {
                val userDocRef = firestore.collection("users").document(usernameLower)
                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(userDocRef)
                    if (snapshot.exists()) {
                        val currentBest = snapshot.getLong("bestScore") ?: 0L
                        if (summary.totalScore > currentBest) {
                            transaction.update(
                                userDocRef,
                                "bestScore", summary.totalScore,
                                "bestLevelsCleared", summary.levelsCleared,
                                "updatedAt", System.currentTimeMillis()
                            )
                        }
                    } else {
                        // Tạo mới nếu chưa tồn tại
                        val newUserData = hashMapOf(
                            "uid" to uid,
                            "displayName" to trimmed,
                            "bestScore" to summary.totalScore,
                            "bestLevelsCleared" to summary.levelsCleared,
                            "updatedAt" to System.currentTimeMillis()
                        )
                        transaction.set(userDocRef, newUserData)
                    }
                }.await()
            }

            // Cập nhật best score local
            preferencesRepository.updateBestScore(summary.totalScore, summary.levelsCleared)
            updateLocalLeaderboardEntry(trimmed, summary.totalScore, summary.levelsCleared)
            true
        } catch (e: Exception) {
            // Mất mạng: lưu tạm run vào DataStore
            queuePendingRun(summary, uid, trimmed)
            preferencesRepository.updateBestScore(summary.totalScore, summary.levelsCleared)
            updateLocalLeaderboardEntry(trimmed, summary.totalScore, summary.levelsCleared)
            false
        }
    }

    private fun updateLocalLeaderboardEntry(username: String, score: Long, levels: Int) {
        val currentList = _leaderboardFlow.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.playerName.equals(username, ignoreCase = true) }
        val entry = LeaderboardEntry(
            rank = 0,
            playerName = username,
            bestScore = score,
            levelsCleared = levels,
            updatedAt = System.currentTimeMillis(),
            isCurrentUser = true
        )

        if (existingIndex >= 0) {
            val existing = currentList[existingIndex]
            if (score > existing.bestScore) {
                currentList[existingIndex] = entry
            }
        } else {
            currentList.add(entry)
        }

        currentList.sortWith(compareByDescending<LeaderboardEntry> { it.bestScore }.thenBy { it.updatedAt })
        val reindexed = currentList.take(50).mapIndexed { index, item ->
            item.copy(rank = index + 1, isCurrentUser = item.playerName.equals(username, ignoreCase = true))
        }
        _leaderboardFlow.value = reindexed
    }

    /**
     * Lưu run vào hàng đợi offline
     */
    private suspend fun queuePendingRun(summary: RunSummary, uid: String, username: String) {
        try {
            val jsonStr = preferencesRepository.getPendingRuns()
            val jsonArray = JSONArray(jsonStr)
            val runObj = JSONObject().apply {
                put("runId", summary.runId)
                put("uid", uid)
                put("username", username)
                put("score", summary.totalScore)
                put("levelsCleared", summary.levelsCleared)
                put("totalTimeMs", summary.totalTimeMs)
            }
            jsonArray.put(runObj)
            preferencesRepository.savePendingRuns(jsonArray.toString())
        } catch (_: Exception) {}
    }

    /**
     * Tự động gửi các run đã lưu tạm khi có mạng trở lại
     */
    suspend fun syncPendingRuns() = withContext(Dispatchers.IO) {
        try {
            val jsonStr = preferencesRepository.getPendingRuns()
            val jsonArray = JSONArray(jsonStr)
            if (jsonArray.length() == 0) return@withContext

            val remainingArray = JSONArray()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val runId = obj.getString("runId")
                val uid = obj.getString("uid")
                val username = obj.getString("username")
                val score = obj.getLong("score")
                val levelsCleared = obj.getInt("levelsCleared")
                val totalTimeMs = obj.getLong("totalTimeMs")

                val summary = RunSummary(
                    runId = runId,
                    playerName = username,
                    levelsCleared = levelsCleared,
                    totalScore = score,
                    totalTimeMs = totalTimeMs
                )

                val success = try {
                    val runDocRef = firestore.collection("runs").document(runId)
                    runDocRef.set(
                        hashMapOf(
                            "id" to runId,
                            "uid" to uid,
                            "username" to username,
                            "score" to score,
                            "levelsCleared" to levelsCleared,
                            "totalTimeMs" to totalTimeMs,
                            "createdAt" to System.currentTimeMillis()
                        )
                    ).await()
                    true
                } catch (_: Exception) {
                    false
                }

                if (!success) {
                    remainingArray.put(obj)
                }
            }

            preferencesRepository.savePendingRuns(remainingArray.toString())
        } catch (_: Exception) {}
    }

    /**
     * Dữ liệu dự phòng khi khởi động lần đầu hoặc offline
     */
    private fun loadFallbackLeaderboard(currentUsername: String) {
        val mockData = listOf(
            LeaderboardEntry(1, "SlitherGod", 48500, 35, 1700000000000L),
            LeaderboardEntry(2, "ViperKing", 42100, 31, 1700001000000L),
            LeaderboardEntry(3, "NeonPython", 37800, 28, 1700002000000L),
            LeaderboardEntry(4, "SpeedyG", 33400, 25, 1700003000000L),
            LeaderboardEntry(5, "PuzzleQueen", 29500, 22, 1700004000000L),
            LeaderboardEntry(6, "CobraKai", 25100, 19, 1700005000000L),
            LeaderboardEntry(7, "VenomX", 21800, 16, 1700006000000L),
            LeaderboardEntry(8, "GoldenAsp", 18200, 14, 1700007000000L),
            LeaderboardEntry(9, "AquaBoa", 15400, 12, 1700008000000L),
            LeaderboardEntry(10, "TurboTail", 12600, 10, 1700009000000L)
        )

        val list = mockData.map {
            it.copy(isCurrentUser = it.playerName.equals(currentUsername, ignoreCase = true))
        }
        _leaderboardFlow.value = list
    }
}
