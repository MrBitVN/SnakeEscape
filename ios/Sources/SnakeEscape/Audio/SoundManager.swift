import Foundation
import AVFoundation

public final class SoundManager: ObservableObject {

    public static let shared = SoundManager()

    private var tapPlayer: AVAudioPlayer?
    private var escapePlayer: AVAudioPlayer?
    private var blockedPlayer: AVAudioPlayer?
    private var winPlayer: AVAudioPlayer?
    private var bgmPlayer: AVAudioPlayer?

    public var isSoundEnabled: Bool = true
    public var isMusicEnabled: Bool = true {
        didSet {
            if isMusicEnabled {
                resumeMusic()
            } else {
                pauseMusic()
            }
        }
    }

    public init() {
        configureAudioSession()
        prepareSynthesizedAudio()
    }

    private func configureAudioSession() {
        #if os(iOS)
        do {
            try AVAudioSession.sharedInstance().setCategory(.ambient, mode: .default)
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            print("AudioSession setup error: \(error)")
        }
        #endif
    }

    private func prepareSynthesizedAudio() {
        let sampleRate = 22050

        // 1. sfx_tap: short crisp click (50ms)
        let tapCount = Int(Double(sampleRate) * 0.05)
        let tapSamples = (0..<tapCount).map { i -> Int16 in
            let t = Double(i) / Double(sampleRate)
            let env = exp(-t * 90.0)
            let freq = 850.0 - t * 3000.0
            let wave = sin(2.0 * .pi * max(100.0, freq) * t)
            return Int16(clamping: Int((wave * env * 24000.0)))
        }
        tapPlayer = makePlayer(samples: tapSamples, sampleRate: sampleRate)

        // 2. sfx_escape: cheerful rising chime (350ms)
        let escCount = Int(Double(sampleRate) * 0.35)
        let escSamples = (0..<escCount).map { i -> Int16 in
            let t = Double(i) / Double(sampleRate)
            let progress = t / 0.35
            let env = sqrt(sin(progress * .pi)) * exp(-progress * 1.5)
            let freq = 420.0 + 700.0 * (progress * progress)
            let wave = (sin(2.0 * .pi * freq * t) + 0.35 * sin(4.0 * .pi * freq * t)) / 1.35
            return Int16(clamping: Int((wave * env * 26000.0)))
        }
        escapePlayer = makePlayer(samples: escSamples, sampleRate: sampleRate)

        // 3. sfx_blocked: low thud / bonk (200ms)
        let blkCount = Int(Double(sampleRate) * 0.20)
        let blkSamples = (0..<blkCount).map { i -> Int16 in
            let t = Double(i) / Double(sampleRate)
            let progress = t / 0.20
            let env = exp(-progress * 12.0)
            let freq = 170.0 - progress * 90.0
            let wave = sin(2.0 * .pi * freq * t)
            let clipped = max(-0.8, min(0.8, wave)) / 0.8
            return Int16(clamping: Int((clipped * env * 26000.0)))
        }
        blockedPlayer = makePlayer(samples: blkSamples, sampleRate: sampleRate)

        // 4. sfx_win: 4-note victory fanfare (C5, E5, G5, C6) (800ms)
        let winCount = Int(Double(sampleRate) * 0.80)
        let notes = [523.25, 659.25, 783.99, 1046.50]
        let winSamples = (0..<winCount).map { i -> Int16 in
            let t = Double(i) / Double(sampleRate)
            let noteIdx = min(Int(t / 0.16), 3)
            let noteT = t - Double(noteIdx) * 0.16
            let freq = notes[noteIdx]
            let env = exp(-noteT * (noteIdx == 3 ? 4.5 : 9.0))
            let wave = (sin(2.0 * .pi * freq * noteT) + 0.25 * sin(4.0 * .pi * freq * noteT)) / 1.25
            return Int16(clamping: Int((wave * env * 24000.0)))
        }
        winPlayer = makePlayer(samples: winSamples, sampleRate: sampleRate)

        // 5. bgm_loop: 6-second calm pentatonic ambient loop
        let bgmCount = Int(Double(sampleRate) * 6.0)
        let melody: [(Double, Double)] = [
            (0.0, 261.63), (0.75, 329.63), (1.50, 392.00), (2.25, 440.00),
            (3.00, 523.25), (3.75, 440.00), (4.50, 392.00), (5.25, 329.63)
        ]
        let bgmSamples = (0..<bgmCount).map { i -> Int16 in
            let t = Double(i) / Double(sampleRate)
            let pad = (sin(2.0 * .pi * 130.81 * t) + sin(2.0 * .pi * 196.00 * t) * 0.6) * 0.22
            var melSum = 0.0
            for (mTime, mFreq) in melody where t >= mTime {
                let dt = t - mTime
                let env = exp(-dt * 3.5)
                melSum += (sin(2.0 * .pi * mFreq * dt) + 0.2 * sin(4.0 * .pi * mFreq * dt)) * env * 0.35
            }
            let fadeTime = 0.05
            let fade = t < fadeTime ? (t / fadeTime) : (t > (6.0 - fadeTime) ? (6.0 - t) / fadeTime : 1.0)
            let mix = (pad + melSum) * fade
            return Int16(clamping: Int((mix * 18000.0)))
        }
        bgmPlayer = makePlayer(samples: bgmSamples, sampleRate: sampleRate)
        bgmPlayer?.numberOfLoops = -1
        bgmPlayer?.volume = 0.35
    }

    private func makePlayer(samples: [Int16], sampleRate: Int) -> AVAudioPlayer? {
        var data = Data()
        let byteRate = sampleRate * 2
        let dataSize = samples.count * 2
        let totalSize = 36 + dataSize

        // RIFF header
        data.append(contentsOf: [0x52, 0x49, 0x46, 0x46]) // "RIFF"
        var totalSizeLittle = UInt32(totalSize).littleEndian
        data.append(Data(bytes: &totalSizeLittle, count: 4))
        data.append(contentsOf: [0x57, 0x41, 0x56, 0x45]) // "WAVE"
        data.append(contentsOf: [0x66, 0x6D, 0x74, 0x20]) // "fmt "

        var subchunk1Size: UInt32 = 16
        var audioFormat: UInt16 = 1 // PCM
        var numChannels: UInt16 = 1 // Mono
        var sampleRateUInt = UInt32(sampleRate).littleEndian
        var byteRateUInt = UInt32(byteRate).littleEndian
        var blockAlign: UInt16 = 2
        var bitsPerSample: UInt16 = 16

        data.append(Data(bytes: &subchunk1Size, count: 4))
        data.append(Data(bytes: &audioFormat, count: 2))
        data.append(Data(bytes: &numChannels, count: 2))
        data.append(Data(bytes: &sampleRateUInt, count: 4))
        data.append(Data(bytes: &byteRateUInt, count: 4))
        data.append(Data(bytes: &blockAlign, count: 2))
        data.append(Data(bytes: &bitsPerSample, count: 2))

        data.append(contentsOf: [0x64, 0x61, 0x74, 0x61]) // "data"
        var dataSizeLittle = UInt32(dataSize).littleEndian
        data.append(Data(bytes: &dataSizeLittle, count: 4))

        // Audio PCM samples
        samples.withUnsafeBytes { data.append(contentsOf: $0) }

        return try? AVAudioPlayer(data: data)
    }

    public func playTap() {
        guard isSoundEnabled else { return }
        tapPlayer?.currentTime = 0
        tapPlayer?.play()
    }

    public func playEscape() {
        guard isSoundEnabled else { return }
        escapePlayer?.currentTime = 0
        escapePlayer?.play()
    }

    public func playBlocked() {
        guard isSoundEnabled else { return }
        blockedPlayer?.currentTime = 0
        blockedPlayer?.play()
    }

    public func playWin() {
        guard isSoundEnabled else { return }
        winPlayer?.currentTime = 0
        winPlayer?.play()
    }

    public func startMusic() {
        guard isMusicEnabled else { return }
        if bgmPlayer?.isPlaying != true {
            bgmPlayer?.play()
        }
    }

    public func pauseMusic() {
        bgmPlayer?.pause()
    }

    public func resumeMusic() {
        if isMusicEnabled {
            startMusic()
        }
    }
}
