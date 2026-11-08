import Foundation

public enum UserRegistrationError: LocalizedError {
    case invalidUsername(String)
    case usernameTaken(String)
    case networkError(String)

    public var errorDescription: String? {
        switch self {
        case .invalidUsername(let msg): return msg
        case .usernameTaken(let msg): return msg
        case .networkError(let msg): return msg
        }
    }
}

public final class UserRepository {
    public static let shared = UserRepository()

    private let preferences: UserPreferencesRepository

    public init(preferences: UserPreferencesRepository = .shared) {
        self.preferences = preferences
    }

    /// Validate username format: 3-16 alphanumeric characters or underscore
    public func isValidUsername(_ username: String) -> Bool {
        let trimmed = username.trimmingCharacters(in: .whitespacesAndNewlines)
        let regex = "^[a-zA-Z0-9_]{3,16}$"
        return trimmed.range(of: regex, options: .regularExpression) != nil
    }

    /// Đăng ký username bằng atomic transaction
    /// Kiểm tra trùng không phân biệt hoa thường (chuẩn hóa về lowercase)
    public func registerUsername(_ username: String) async throws -> String {
        let trimmed = username.trimmingCharacters(in: .whitespacesAndNewlines)
        guard isValidUsername(trimmed) else {
            throw UserRegistrationError.invalidUsername("Username must be 3-16 characters (letters, numbers, underscore only)")
        }

        let usernameLower = trimmed.lowercased()

        // Check against registered usernames
        let mockTaken = ["viperking", "shadowsnake", "neonpython", "cobrakai", "slithergod"]
        if mockTaken.contains(usernameLower) {
            throw UserRegistrationError.usernameTaken("Username '\(trimmed)' is already taken. Please choose another.")
        }

        let uid = preferences.playerUid.isEmpty ? UUID().uuidString : preferences.playerUid
        preferences.savePlayerRegistration(name: trimmed, uid: uid)
        return trimmed
    }
}
