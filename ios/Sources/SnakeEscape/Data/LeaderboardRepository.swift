import Foundation
import Combine

public struct LeaderboardEntry: Identifiable, Equatable {
    public var id: String { playerName.lowercased() }
    public let rank: Int
    public let playerName: String
    public let bestScore: Int64
    public let levelsCleared: Int
    public let updatedAt: Int64
    public let isCurrentUser: Bool

    public var formattedScore: String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        return formatter.string(from: NSNumber(value: bestScore)) ?? "\(bestScore)"
    }

    public init(
        rank: Int,
        playerName: String,
        bestScore: Int64,
        levelsCleared: Int,
        updatedAt: Int64,
        isCurrentUser: Bool = false
    ) {
        self.rank = rank
        self.playerName = playerName
        self.bestScore = bestScore
        self.levelsCleared = levelsCleared
        self.updatedAt = updatedAt
        self.isCurrentUser = isCurrentUser
    }
}

public final class LeaderboardRepository: ObservableObject {
    public static let shared = LeaderboardRepository()

    private let preferences: UserPreferencesRepository
    @Published public private(set) var leaderboard: [LeaderboardEntry] = []

    public init(preferences: UserPreferencesRepository = .shared) {
        self.preferences = preferences
        loadInitialData()
    }

    private func loadInitialData() {
        let mockList = [
            LeaderboardEntry(rank: 1, playerName: "SlitherGod", bestScore: 48500, levelsCleared: 35, updatedAt: 1700000000000),
            LeaderboardEntry(rank: 2, playerName: "ViperKing", bestScore: 42100, levelsCleared: 31, updatedAt: 1700001000000),
            LeaderboardEntry(rank: 3, playerName: "NeonPython", bestScore: 37800, levelsCleared: 28, updatedAt: 1700002000000),
            LeaderboardEntry(rank: 4, playerName: "SpeedyG", bestScore: 33400, levelsCleared: 25, updatedAt: 1700003000000),
            LeaderboardEntry(rank: 5, playerName: "PuzzleQueen", bestScore: 29500, levelsCleared: 22, updatedAt: 1700004000000),
            LeaderboardEntry(rank: 6, playerName: "CobraKai", bestScore: 25100, levelsCleared: 19, updatedAt: 1700005000000),
            LeaderboardEntry(rank: 7, playerName: "VenomX", bestScore: 21800, levelsCleared: 16, updatedAt: 1700006000000),
            LeaderboardEntry(rank: 8, playerName: "GoldenAsp", bestScore: 18200, levelsCleared: 14, updatedAt: 1700007000000),
            LeaderboardEntry(rank: 9, playerName: "AquaBoa", bestScore: 15400, levelsCleared: 12, updatedAt: 1700008000000),
            LeaderboardEntry(rank: 10, playerName: "TurboTail", bestScore: 12600, levelsCleared: 10, updatedAt: 1700009000000)
        ]
        self.leaderboard = mockList
    }

    public func startRealtimeListener(currentUsername: String) {
        // Refresh with current user highlighted
        self.leaderboard = self.leaderboard.map { entry in
            var updated = entry
            let isMe = entry.playerName.caseInsensitiveCompare(currentUsername) == .orderedSame
            return LeaderboardEntry(
                rank: entry.rank,
                playerName: entry.playerName,
                bestScore: entry.bestScore,
                levelsCleared: entry.levelsCleared,
                updatedAt: entry.updatedAt,
                isCurrentUser: isMe
            )
        }
    }

    public func recordRunEnd(summary: RunSummary, uid: String, username: String) {
        preferences.updateBestScore(score: summary.totalScore, levels: summary.levelsCleared)

        // Inject or update user in leaderboard
        var currentList = leaderboard.filter { $0.playerName.caseInsensitiveCompare(username) != .orderedSame }
        let myEntry = LeaderboardEntry(
            rank: 0,
            playerName: username,
            bestScore: max(summary.totalScore, preferences.bestScore),
            levelsCleared: summary.levelsCleared,
            updatedAt: Int64(Date().timeIntervalSince1970 * 1000),
            isCurrentUser: true
        )
        currentList.append(myEntry)

        // Sort descending by score, ascending by updatedAt
        currentList.sort {
            if $0.bestScore != $1.bestScore {
                return $0.bestScore > $1.bestScore
            }
            return $0.updatedAt < $1.updatedAt
        }

        // Re-index ranks 1..N
        let reindexed = currentList.prefix(50).enumerated().map { index, entry in
            LeaderboardEntry(
                rank: index + 1,
                playerName: entry.playerName,
                bestScore: entry.bestScore,
                levelsCleared: entry.levelsCleared,
                updatedAt: entry.updatedAt,
                isCurrentUser: entry.isCurrentUser
            )
        }

        DispatchQueue.main.async {
            self.leaderboard = Array(reindexed)
        }
    }
}
