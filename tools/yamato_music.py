# YAMATO 뽑기 연출 음악 (오리지널, 직접 합성). 시간표는 GachaFx 의 YAMATO 구간과 맞춘다.
#   0.00  칼집에서 뽑는 금속음 + 낮은 충격 (화면이 멈추고 푸르게)
#   0.25~2.30  16분음표 현악 오스티나토, SLASH_TIMES 마다 베는 소리 (화면에 베기 선)
#   2.30~2.90  정적 (높은 이명음만)
#   2.90  칼집에 넣는 '딸깍'
#   3.00  거대한 타격 + 유리 깨짐 + 장엄한 화음 (화면이 깨진다)
#   3.00~8.60  북·현악·합창 화음 진행, 끝에서 사라짐
import sys
import numpy as np
import wave

SR = 44100
DUR = 8.6
N = int(SR * DUR)
L = np.zeros(N)
R = np.zeros(N)
rng = np.random.default_rng(7)
BPM = 140
SIX = 60 / BPM / 4  # 16분음표 길이

SLASH_TIMES = [0.32, 0.53, 0.75, 0.86, 1.07, 1.18, 1.39, 1.50, 1.61, 1.71, 1.82, 1.93, 2.03, 2.14]


def t_arr(n):
    return np.arange(n) / SR


def add(sig, at, pan=0.0, gain=1.0):
    i = int(at * SR)
    if i >= N:
        return
    n = min(len(sig), N - i)
    l = np.cos((pan + 1) * np.pi / 4)
    r = np.sin((pan + 1) * np.pi / 4)
    L[i:i + n] += sig[:n] * gain * l * 1.414
    R[i:i + n] += sig[:n] * gain * r * 1.414


def env(n, a, d, s=0.0, rel=0.0):
    """attack a 초, decay d 초 (지수)"""
    t = t_arr(n)
    e = np.minimum(1, t / max(a, 1e-4)) * (s + (1 - s) * np.exp(-np.maximum(0, t - a) / max(d, 1e-4)))
    if rel > 0:
        e *= np.clip((t[-1] - t) / rel, 0, 1)
    return e


def lowpass(x, cutoff):
    # 1극 저역 통과 (여러 번 걸어 기울기를 키운다)
    a = np.exp(-2 * np.pi * cutoff / SR)
    y = np.zeros_like(x)
    acc = 0.0
    for _ in range(1):
        pass
    # 벡터화: scipy 사용
    from scipy.signal import lfilter
    y = lfilter([1 - a], [1, -a], x)
    y = lfilter([1 - a], [1, -a], y)
    return y


def bandpass(x, lo, hi):
    from scipy.signal import butter, sosfilt
    sos = butter(2, [lo, hi], btype='band', fs=SR, output='sos')
    return sosfilt(sos, x)


def highpass(x, f):
    from scipy.signal import butter, sosfilt
    sos = butter(2, f, btype='high', fs=SR, output='sos')
    return sosfilt(sos, x)


def saw(freq, n, detune=0.0):
    t = t_arr(n)
    out = np.zeros(n)
    for d in (-detune, 0, detune):
        f = freq * (1 + d)
        out += 2 * ((t * f + rng.random()) % 1) - 1
    return out / 3


def note(name):
    names = {'C': 0, 'C#': 1, 'D': 2, 'D#': 3, 'Eb': 3, 'E': 4, 'F': 5, 'F#': 6, 'G': 7, 'G#': 8, 'Ab': 8, 'A': 9, 'A#': 10, 'Bb': 10, 'B': 11}
    p, o = name[:-1], int(name[-1])
    return 440 * 2 ** ((names[p] + 12 * (o + 1) - 69) / 12)


# ---------------------------------------------------------------- 소리 재료

def shing(n=int(1.4 * SR)):
    t = t_arr(n)
    s = np.zeros(n)
    for f, a, dcy in [(2873, 0.5, 0.9), (4211, 0.35, 0.7), (5630, 0.25, 0.5), (7350, 0.18, 0.35), (9810, 0.1, 0.25), (1630, 0.2, 1.0)]:
        s += a * np.sin(2 * np.pi * f * t * (1 + 0.002 * np.sin(2 * np.pi * 5 * t))) * np.exp(-t / dcy)
    scrape = highpass(rng.standard_normal(n), 3000) * np.exp(-t / 0.08) * 0.4
    return (s + scrape) * np.minimum(1, t / 0.002)


def boom(n=int(1.8 * SR), f0=90, f1=32, dcy=0.6):
    t = t_arr(n)
    f = f1 + (f0 - f1) * np.exp(-t / 0.12)
    ph = 2 * np.pi * np.cumsum(f) / SR
    return np.sin(ph) * np.exp(-t / dcy) * np.minimum(1, t / 0.003)


def taiko(n=int(0.9 * SR)):
    t = t_arr(n)
    f = 55 + 70 * np.exp(-t / 0.05)
    body = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.28)
    skin = lowpass(rng.standard_normal(n), 900) * np.exp(-t / 0.03) * 2.5
    return (body + skin) * 0.9


def whoosh(n=int(0.16 * SR)):
    t = t_arr(n)
    noise = rng.standard_normal(n)
    # 높이 올라가는 쉿 소리
    out = np.zeros(n)
    seg = n // 8
    for k in range(8):
        lo = 1500 + k * 700
        part = bandpass(noise[k * seg:(k + 1) * seg + 1], lo, min(lo * 2.2, 20000))
        out[k * seg:k * seg + len(part)] += part[:max(0, n - k * seg)]
    e = np.sin(np.pi * np.clip(t / t[-1], 0, 1)) ** 1.5
    tick = np.zeros(n)
    tick[int(0.05 * SR):int(0.05 * SR) + 60] = np.hanning(60) * 2.5
    return out * e * 1.6 + tick


def click(n=int(0.25 * SR)):
    t = t_arr(n)
    c = np.zeros(n)
    for at, amp in [(0.0, 1.0), (0.028, 0.7)]:
        i = int(at * SR)
        m = n - i
        tt = t_arr(m)
        c[i:] += amp * (np.sin(2 * np.pi * 3400 * tt) + 0.6 * np.sin(2 * np.pi * 5200 * tt)) * np.exp(-tt / 0.012)
    return c


def glass(n=int(2.5 * SR)):
    out = np.zeros(n)
    for k in range(70):
        at = abs(rng.normal(0, 0.18))
        f = rng.uniform(2500, 11000)
        m = n - int(at * SR)
        if m <= 0:
            continue
        tt = t_arr(m)
        out[int(at * SR):] += rng.uniform(0.05, 0.25) * np.sin(2 * np.pi * f * tt) * np.exp(-tt / rng.uniform(0.04, 0.35))
    crash = highpass(rng.standard_normal(n), 2500) * np.exp(-t_arr(n) / 0.25) * 0.8
    return out + crash


def string_stab(freq, n, bright=2500):
    t = t_arr(n)
    s = lowpass(saw(freq, n, 0.004), bright)
    return s * np.minimum(1, t / 0.006) * np.exp(-t / 0.09)


def pad(freqs, n, cutoff=1800, att=0.25):
    t = t_arr(n)
    s = np.zeros(n)
    for f in freqs:
        vib = 1 + 0.004 * np.sin(2 * np.pi * 5.2 * t + rng.random() * 6)
        ph = 2 * np.pi * np.cumsum(f * vib) / SR
        s += (2 * ((ph / (2 * np.pi) + rng.random()) % 1) - 1) * 0.5 + 0.5 * np.sin(ph)
    s = lowpass(s / len(freqs), cutoff)
    e = np.minimum(1, t / att) * np.clip((t[-1] - t) / 0.25, 0, 1)
    return s * e


def choir(freqs, n):
    # 'ah' 모음: 톱니파를 모음 대역 두 개로 거른다
    src = pad(freqs, n, 6000, 0.35)
    return bandpass(src, 600, 1300) * 1.4 + bandpass(src, 2200, 3200) * 0.6


# ---------------------------------------------------------------- 배치

add(shing(), 0.0, 0.0, 0.9)
add(boom(), 0.0, 0.0, 1.0)
# 긴장되는 16분음표 오스티나토 (D 단조)
seq = ['D2', 'D2', 'D3', 'D2', 'F2', 'D2', 'E2', 'D2', 'D2', 'D2', 'D3', 'D2', 'G2', 'F2', 'E2', 'C#2', 'D2', 'D2', 'D3', 'D2']
i = 0
tm = 0.21
while tm < 2.28:
    f = note(seq[i % len(seq)])
    acc = 1.0 + 0.6 * (i % 4 == 0)
    gain = 0.18 + 0.22 * (tm / 2.3)
    add(string_stab(f, int(0.2 * SR), 1600 + 2400 * tm / 2.3), tm, -0.2, gain * acc)
    add(string_stab(f * 2, int(0.2 * SR), 2600), tm, 0.3, gain * 0.35)
    i += 1
    tm += SIX
# 올라가는 트레몰로 (긴장)
n = int(2.1 * SR)
t = t_arr(n)
trem = pad([note('A3'), note('D4'), note('A4')], n, 3000, 1.8) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 14 * t)))
add(trem * np.linspace(0.05, 0.35, n), 0.2, 0.0, 1.0)
# 베기
for k, s in enumerate(SLASH_TIMES):
    add(whoosh(), s - 0.05, (-0.7 if k % 2 == 0 else 0.7) * (0.4 + 0.6 * rng.random()), 0.55 + 0.03 * k)
# 정적 속 이명
n = int(0.6 * SR)
add(np.sin(2 * np.pi * 7900 * t_arr(n)) * np.hanning(n) * 0.025, 2.3, 0.0)
# 칼집에 넣는 소리
add(click(), 2.90, 0.0, 0.9)
# 대폭발
add(boom(int(3.5 * SR), 120, 28, 1.4), 3.0, 0.0, 1.3)
add(glass(), 3.0, 0.0, 0.8)
add(taiko(), 3.0, -0.2, 1.0)
add(taiko(), 3.0, 0.2, 1.0)
# 화음 진행 Dm - Bb - C - Dm (각 2박)
beat = 60 / BPM
prog = [['D3', 'F3', 'A3', 'D4'], ['Bb2', 'D3', 'F3', 'Bb3'], ['C3', 'E3', 'G3', 'C4'], ['D3', 'F3', 'A3', 'D4']]
for k, ch in enumerate(prog):
    at = 3.0 + k * 2 * beat
    length = 2 * beat + (2.6 if k == 3 else 0.12)
    fr = [note(x) for x in ch]
    add(pad(fr, int(length * SR), 2200, 0.05), at, 0.0, 0.42)
    add(choir([f * 2 for f in fr], int(length * SR)), at, 0.0, 0.5)
    add(pad([fr[0] / 2], int(length * SR), 400, 0.02), at, 0.0, 0.5)
# 북 리듬
pattern = [0, 0.75, 1.0, 1.5, 1.75, 2.0, 2.75, 3.0, 3.25, 3.5, 3.75, 4.0, 4.75, 5.0, 5.5, 5.75, 6.0, 6.5, 6.75, 7.0, 7.25, 7.5, 7.75, 8.0]
for b in pattern:
    at = 3.0 + b * beat
    if at < 8.0:
        add(taiko(), at, rng.uniform(-0.4, 0.4), 0.55 + 0.25 * (b % 1 == 0))
# 마지막 일격
add(boom(int(2.6 * SR), 100, 30, 1.0), 3.0 + 8 * beat, 0.0, 1.0)
add(shing(), 3.0 + 8 * beat, 0.0, 0.4)

# ---------------------------------------------------------------- 마무리
mix = np.stack([L, R], 1)
# 간단한 잔향 (지연 합)
rev = np.zeros_like(mix)
for d, g in [(0.031, 0.35), (0.047, 0.3), (0.071, 0.25), (0.113, 0.2), (0.167, 0.15)]:
    k = int(d * SR)
    rev[k:, 0] += mix[:-k, 1] * g
    rev[k:, 1] += mix[:-k, 0] * g
mix = mix + rev * 0.6
fade = np.ones(N)
fs = int(7.6 * SR)
fade[fs:] = np.linspace(1, 0, N - fs)
mix *= fade[:, None]
mix = np.tanh(mix / np.max(np.abs(mix)) * 1.6) * 0.92
pcm = (mix * 32767).astype(np.int16)
with wave.open(sys.argv[1], 'wb') as w:
    w.setnchannels(2)
    w.setsampwidth(2)
    w.setframerate(SR)
    w.writeframes(pcm.tobytes())
print('ok', DUR, 's')
