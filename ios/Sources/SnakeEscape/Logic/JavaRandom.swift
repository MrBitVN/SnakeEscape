import Foundation

/// Thuật toán sinh số ngẫu nhiên 48-bit Linear Congruential Generator (LCG)
/// tương thích 100% với `java.util.Random(seed)` của Android / Kotlin.
/// Giúp bảo đảm toàn bộ 200 màn chơi sinh ra trên iOS giống hệt 1:1 với bản Android!
public struct JavaRandom {
    private var seed: UInt64
    private static let multiplier: UInt64 = 0x5DEECE66D
    private static let addend: UInt64 = 0xB
    private static let mask: UInt64 = (1 << 48) - 1

    public init(seed: Int64) {
        self.seed = (UInt64(bitPattern: seed) ^ Self.multiplier) & Self.mask
    }

    private mutating func next(bits: Int) -> Int32 {
        seed = (seed &* Self.multiplier &+ Self.addend) & Self.mask
        return Int32(seed >> (48 - bits))
    }

    public mutating func nextInt(_ bound: Int) -> Int {
        precondition(bound > 0, "bound must be positive")
        if (bound & (bound - 1)) == 0 {
            // Lũy thừa của 2
            let n = Int64(bound)
            let b = Int64(next(bits: 31))
            return Int((n * b) >> 31)
        }

        var bits: Int32
        var val: Int32
        repeat {
            bits = next(bits: 31)
            val = bits % Int32(bound)
        } while bits - val + Int32(bound - 1) < 0

        return Int(val)
    }

    public mutating func nextDouble() -> Double {
        let high = Int64(next(bits: 26)) << 27
        let low = Int64(next(bits: 27))
        return Double(high + low) / Double(Int64(1) << 53)
    }

    /// Hoán vị mảng ngẫu nhiên (Fisher-Yates) tương tự `Collections.shuffle(list, random)`
    public mutating func shuffle<T>(_ array: inout [T]) {
        if array.count <= 1 { return }
        for i in stride(from: array.count - 1, through: 1, by: -1) {
            let j = nextInt(i + 1)
            if i != j {
                array.swapAt(i, j)
            }
        }
    }
}
