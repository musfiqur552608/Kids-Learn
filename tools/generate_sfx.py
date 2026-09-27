#!/usr/bin/env python3
"""
Generates the short, friendly sound effects bundled in app/src/main/res/raw.

Why generate them instead of shipping recordings?
  * Keeps the repo binary-light and the APK small (every file is < 40 KB).
  * Guarantees the tones are *gentle*: for a 3-8 year old audience nothing here
    is a harsh buzzer. The "try again" cue is a soft, low, non-jarring blip
    rather than the usual descending error tone.
  * No network / no third-party licensing needed.

Run:  python3 tools/generate_sfx.py
Output: 22050 Hz, mono, 16-bit PCM WAV.
"""
import math
import os
import struct
import wave

SAMPLE_RATE = 22050
OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")


def env_ad(n, attack=0.01, release=0.25, total=1.0):
    """Attack/release envelope in [0,1]. Avoids the click of a hard gate."""
    a = max(1, int(attack * SAMPLE_RATE))
    r = max(1, int(release * SAMPLE_RATE))
    total_n = int(total * SAMPLE_RATE)
    out = []
    for i in range(total_n):
        if i < a:
            v = i / a
        elif i > total_n - r:
            v = max(0.0, (total_n - i) / r)
        else:
            v = 1.0
        out.append(v)
    # soft-clip-ish rounding of the tail so release is exponential, not linear
    return out


def tone(freq, dur, amp=0.55, harmonics=(1.0, 0.25, 0.12), attack=0.008, release=0.18,
         detune=0.0, vibrato=0.0):
    n = int(dur * SAMPLE_RATE)
    env = env_ad(n, attack, release, dur)
    out = []
    for i in range(n):
        t = i / SAMPLE_RATE
        f = freq * (1.0 + detune * t)
        if vibrato:
            f *= 1.0 + vibrato * math.sin(2 * math.pi * 6.0 * t)
        s = 0.0
        for k, h in enumerate(harmonics, start=1):
            s += h * math.sin(2 * math.pi * f * k * t)
        out.append(s * env[i] * amp)
    return out


def mix(*tracks):
    n = max(len(t) for t in tracks)
    out = [0.0] * n
    for t in tracks:
        for i, v in enumerate(t):
            out[i] += v
    return out


def seq(*parts):
    """Concatenate (note, start_seconds) pairs into one buffer."""
    total = 0.0
    placed = []
    for start, buf in parts:
        placed.append((start, buf))
        total = max(total, start + len(buf) / SAMPLE_RATE)
    n = int(total * SAMPLE_RATE) + 1
    out = [0.0] * n
    for start, buf in placed:
        off = int(start * SAMPLE_RATE)
        for i, v in enumerate(buf):
            if off + i < n:
                out[off + i] += v
    return out


def pad(buf, dur):
    n = int(dur * SAMPLE_RATE)
    return buf + [0.0] * max(0, n - len(buf))


def normalise(buf, peak=0.72):
    m = max((abs(v) for v in buf), default=1.0)
    if m == 0:
        return buf
    k = peak / m
    return [v * k for v in buf]


def save(name, buf):
    buf = normalise(buf)
    path = os.path.join(OUT_DIR, name)
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SAMPLE_RATE)
        w.writeframes(b"".join(
            struct.pack("<h", max(-32768, min(32767, int(v * 32767)))) for v in buf
        ))
    print(f"  {name:24s} {os.path.getsize(path):>6d} bytes  {len(buf)/SAMPLE_RATE:.2f}s")


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    print("Writing sound effects to res/raw:")

    # C5 E5 G5 -> bright major triad, the classic "yes!" for small children.
    save("sfx_correct.wav", seq(
        (0.00, tone(523.25, 0.13, amp=0.5, release=0.10)),
        (0.09, tone(659.25, 0.13, amp=0.5, release=0.10)),
        (0.18, tone(783.99, 0.34, amp=0.55, release=0.28, vibrato=0.004)),
    ))

    # Deliberately soft and *low* - a sad buzzer would feel like punishment.
    save("sfx_try_again.wav", seq(
        (0.00, tone(392.00, 0.11, amp=0.34, attack=0.02, release=0.09)),
        (0.10, tone(329.63, 0.26, amp=0.34, attack=0.02, release=0.22)),
    ))

    # Tiny UI "pop" for taps.
    save("sfx_tap.wav", tone(880.0, 0.09, amp=0.42, attack=0.002, release=0.08,
                             harmonics=(1.0, 0.4, 0.2)))

    # Sparkle for earning a star: quick rising pair with a shimmer tail.
    save("sfx_star.wav", seq(
        (0.00, tone(1046.50, 0.11, amp=0.40, release=0.09)),
        (0.07, tone(1318.51, 0.11, amp=0.40, release=0.09)),
        (0.14, tone(1760.00, 0.30, amp=0.42, release=0.26, vibrato=0.008)),
    ))

    # Short fanfare for finishing a lesson / module.
    save("sfx_module_complete.wav", seq(
        (0.00, tone(523.25, 0.15, amp=0.45, release=0.12)),
        (0.11, tone(659.25, 0.15, amp=0.45, release=0.12)),
        (0.22, tone(783.99, 0.15, amp=0.45, release=0.12)),
        (0.33, tone(1046.50, 0.55, amp=0.55, release=0.45, vibrato=0.005)),
        (0.33, tone(783.99, 0.55, amp=0.25, release=0.45)),
    ))

    # Two-tone chime for a badge unlock.
    save("sfx_badge_unlocked.wav", seq(
        (0.00, tone(783.99, 0.20, amp=0.42, release=0.16)),
        (0.16, tone(1174.66, 0.42, amp=0.45, release=0.34, vibrato=0.006)),
    ))

    # Whoosh used when a new screen slides in.
    save("sfx_page_turn.wav", tone(520.0, 0.26, amp=0.30, attack=0.05, release=0.18,
                                  harmonics=(1.0, 0.15), detune=1.6))

    # Gentle tick for the timed-quiz countdown.
    save("sfx_tick.wav", tone(1200.0, 0.06, amp=0.30, attack=0.001, release=0.05,
                              harmonics=(1.0, 0.3)))


if __name__ == "__main__":
    main()
