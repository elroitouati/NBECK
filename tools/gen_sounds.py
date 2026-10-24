# Generates the game sounds as WAV into <out_dir>; convert to OGG with ffmpeg into
# app/src/main/res/raw/snd_*.ogg (see bottom of file for the exact command).
# Usage: python3 tools/gen_sounds.py <out_dir>
import numpy as np, wave, sys, os

SR = 44100
out = sys.argv[1]
rng = np.random.default_rng(7)

def save(name, x, peak=0.85):
    x = x / (np.max(np.abs(x)) + 1e-9) * peak
    pcm = (np.clip(x, -1, 1) * 32767).astype(np.int16)
    with wave.open(os.path.join(out, name + ".wav"), "w") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())

def t(d): return np.arange(int(SR * d)) / SR
def midi(n): return 440.0 * 2 ** ((n - 69) / 12)

def adsr(n, a=0.01, d=0.08, s=0.7, r=0.12):
    e = np.full(n, s)
    A, D, R = int(a * SR), int(d * SR), int(r * SR)
    e[:A] = np.linspace(0, 1, A)
    e[A:A + D] = np.linspace(1, s, D)
    e[-R:] *= np.linspace(1, 0, R) ** 1.5
    return e

def reverb(x, secs=1.2, mix=0.25):
    ir = rng.standard_normal(int(SR * secs)) * np.exp(-np.linspace(0, 7, int(SR * secs)))
    ir[:int(0.01 * SR)] = 0
    n = len(x) + len(ir)
    wet = np.fft.irfft(np.fft.rfft(x, n) * np.fft.rfft(ir, n), n)[: len(x) + int(SR * secs * 0.6)]
    dry = np.concatenate([x, np.zeros(len(wet) - len(x))])
    return dry + mix * wet / (np.max(np.abs(wet)) + 1e-9) * np.max(np.abs(x))

def brass(f, dur, vib=5.0):
    tt = t(dur)
    ph = 2 * np.pi * f * tt + 0.004 * f / vib * np.sin(2 * np.pi * vib * tt) * np.clip(tt / 0.15, 0, 1)
    tone = sum((0.9 ** k) / k * np.sin(k * ph) for k in range(1, 9))  # bright, saw-ish but soft
    return tone * adsr(len(tt), a=0.02, d=0.1, s=0.75, r=min(0.15, dur * 0.4))

def organ(f, dur):
    tt = t(dur)
    tone = sum(a * np.sin(2 * np.pi * f * m * tt) for m, a in [(1, 1), (2, .55), (3, .3), (4, .22), (6, .12), (8, .08)])
    return tone * adsr(len(tt), a=0.005, d=0.05, s=0.85, r=0.06)

def place(buf, start, x):
    i = int(start * SR); buf[i:i + len(x)] += x[: len(buf) - i]

# --- flip: short woody tick (unchanged character, slightly softer)
tt = t(0.07)
flip = np.sin(2 * np.pi * 1700 * tt) * np.exp(-tt * 95) + 0.5 * np.sin(2 * np.pi * 850 * tt) * np.exp(-tt * 60) \
    + 0.15 * rng.standard_normal(len(tt)) * np.exp(-tt * 220)
save("flip", flip, 0.7)

# --- win: arena-organ "charge" arpeggio into a held major chord with brass
win = np.zeros(int(SR * 2.4))
notes = [67, 72, 76, 79]            # G4 C5 E5 G5
for i, n in enumerate(notes):
    place(win, i * 0.11, organ(midi(n), 0.13) * 0.8)
for n in (72, 76, 79, 84):          # C major, held
    place(win, 0.46, brass(midi(n), 1.25) * (0.55 if n == 84 else 0.4))
place(win, 0.46, organ(midi(48), 1.2) * 0.35)   # low C foundation
save("win", reverb(win, 1.4, 0.3))

# --- lose: friendly "wah-wah-wah-waaah" trombone slide down
lose = np.zeros(int(SR * 2.3))
seq = [(65, 0.0, 0.32), (64, 0.36, 0.32), (63, 0.72, 0.32)]   # F4 E4 Eb4
for n, st, d in seq:
    place(lose, st, brass(midi(n - 12), d, vib=0.1) * 0.7)
tt = t(1.0)                                                    # final D4 sagging, with wobble
f = midi(62 - 12) * (1 - 0.03 * np.clip((tt - 0.3) / 0.7, 0, 1))
ph = 2 * np.pi * np.cumsum(f) / SR + 0.25 * np.sin(2 * np.pi * 6 * tt) * np.clip((tt - 0.15) / 0.3, 0, 1)
tail = sum((0.85 ** k) / k * np.sin(k * ph) for k in range(1, 8)) * adsr(len(tt), a=0.03, d=0.1, s=0.8, r=0.35)
place(lose, 1.08, tail * 0.75)
save("lose", reverb(lose, 0.9, 0.18))

# --- crowd cheer for achievements (unchanged)
def lp(x, a):
    y = np.zeros_like(x); s = 0.0
    for i, v in enumerate(x): s += a * (v - s); y[i] = s
    return y
tt = t(1.8); c = np.zeros(len(tt))
for k in range(14):
    nn = rng.standard_normal(len(tt)); a = rng.uniform(0.05, 0.2)
    c += (lp(nn, a) - lp(nn, a / 4)) * (1 + 0.4 * np.sin(2 * np.pi * rng.uniform(2, 6) * tt + rng.uniform(0, 6)))
swell = np.clip(tt / 0.35, 0, 1) * np.clip((1.8 - tt) / 0.8, 0, 1)
save("crowd", c * swell)

# for f in flip win lose crowd; do ffmpeg -y -i $OUT/$f.wav -c:a libvorbis -q:a 5 app/src/main/res/raw/snd_$f.ogg; done
