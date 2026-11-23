# 뽑기 연출 소리 (오리지널, 직접 합성). python3 tools/gacha_audio.py <출력 폴더> → yamato.wav, blackout.wav, legendary_pop.wav
# 시간표는 GachaFx 와 맞춘다 (YAMATO 음악은 YM_START 틱에 시작, 초 × 20 = 틱).
#   0.00  암전: 모든 소리가 끊기는 둔탁한 충격
#   0.40  칼집에서 칼을 천천히 뽑는 금속음 → 0.62 '챙' 하는 울림
#   0.75~2.35  차원참 난무 (SLASH_TIMES), 북·현악 오스티나토가 점점 빨라진다
#   2.38, 2.44  마지막 X 자 두 번
#   2.50  화면이 깨진다 (유리 + 충격)
#   2.55~3.70  흑백 정적: 이명, 느린 심장, 칼을 천천히 칼집에 넣는 소리
#   3.60  '딸깍'
#   3.70  대폭발 → 합창·북·현악 테마, 10.7초에 사라진다
import os
import sys
import wave

import numpy as np
from scipy.signal import butter, sosfilt, lfilter

SR = 44100
rng = np.random.default_rng(11)


def t_arr(n):
    return np.arange(n) / SR


class Track:
    def __init__(self, dur):
        self.n = int(SR * dur)
        self.L = np.zeros(self.n)
        self.R = np.zeros(self.n)

    def add(self, sig, at, pan=0.0, gain=1.0):
        i = int(at * SR)
        if i >= self.n or i < 0:
            return
        k = min(len(sig), self.n - i)
        l = np.cos((pan + 1) * np.pi / 4) * 1.414
        r = np.sin((pan + 1) * np.pi / 4) * 1.414
        self.L[i:i + k] += sig[:k] * gain * l
        self.R[i:i + k] += sig[:k] * gain * r

    def add_wide(self, sig, at, gain=1.0, delay=0.011):
        """좌우로 퍼지는 소리 (한쪽을 조금 늦춘다)"""
        d = int(delay * SR)
        self.add(sig, at, -0.6, gain * 0.8)
        self.add(np.concatenate([np.zeros(d), sig]), at, 0.6, gain * 0.8)

    def duck(self, a, b, level, ramp=0.06):
        """a~b 초 사이를 level 배로 줄인다 (이미 넣은 소리만)"""
        g = np.ones(self.n)
        ia, ib, r = int(a * SR), int(b * SR), int(ramp * SR)
        g[ia:ib] = level
        g[ia:ia + r] = np.linspace(1, level, r)
        g[ib - r:ib] = np.linspace(level, 1, r)
        self.L *= g
        self.R *= g

    def write(self, path, fade_from=None, drive=1.5, reverb=0.55):
        mix = np.stack([self.L, self.R], 1)
        rev = np.zeros_like(mix)
        for d, g in [(0.029, 0.36), (0.043, 0.31), (0.067, 0.27), (0.101, 0.22), (0.149, 0.18), (0.211, 0.13), (0.307, 0.09)]:
            k = int(d * SR)
            rev[k:, 0] += mix[:-k, 1] * g
            rev[k:, 1] += mix[:-k, 0] * g
        rev = lowpass(rev[:, 0], 5000)[:, None] * np.array([1, 0]) + lowpass(rev[:, 1], 5000)[:, None] * np.array([0, 1])
        mix = mix + rev * reverb
        if fade_from is not None:
            fade = np.ones(self.n)
            fs = int(fade_from * SR)
            fade[fs:] = np.linspace(1, 0, self.n - fs) ** 1.5
            mix *= fade[:, None]
        mix = np.tanh(mix / np.max(np.abs(mix)) * drive) / np.tanh(drive) * 0.95
        pcm = (mix * 32767).astype(np.int16)
        with wave.open(path, 'wb') as w:
            w.setnchannels(2)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes(pcm.tobytes())


def env(n, a, d, s=0.0):
    t = t_arr(n)
    return np.minimum(1, t / max(a, 1e-4)) * (s + (1 - s) * np.exp(-np.maximum(0, t - a) / max(d, 1e-4)))


def lowpass(x, cutoff):
    a = np.exp(-2 * np.pi * cutoff / SR)
    y = lfilter([1 - a], [1, -a], x)
    return lfilter([1 - a], [1, -a], y)


def bandpass(x, lo, hi, order=2):
    sos = butter(order, [lo, min(hi, SR / 2 - 100)], btype='band', fs=SR, output='sos')
    return sosfilt(sos, x)


def highpass(x, f):
    return sosfilt(butter(2, f, btype='high', fs=SR, output='sos'), x)


def saw(freq, n, detune=0.0, voices=3):
    t = t_arr(n)
    out = np.zeros(n)
    for k in range(voices):
        d = detune * (k - (voices - 1) / 2)
        out += 2 * ((t * freq * (1 + d) + rng.random()) % 1) - 1
    return out / voices


def note(name):
    names = {'C': 0, 'C#': 1, 'Db': 1, 'D': 2, 'D#': 3, 'Eb': 3, 'E': 4, 'F': 5, 'F#': 6, 'G': 7, 'G#': 8, 'Ab': 8, 'A': 9, 'A#': 10, 'Bb': 10, 'B': 11}
    p, o = name[:-1], int(name[-1])
    return 440 * 2 ** ((names[p] + 12 * (o + 1) - 69) / 12)


# ---------------------------------------------------------------- 소리 재료

def shing(n=int(1.6 * SR), base=1.0):
    t = t_arr(n)
    s = np.zeros(n)
    for f, a, dcy in [(2873, 0.5, 1.1), (4211, 0.35, 0.8), (5630, 0.25, 0.6), (7350, 0.18, 0.4), (9810, 0.1, 0.25), (1630, 0.22, 1.3), (12400, 0.06, 0.15)]:
        s += a * np.sin(2 * np.pi * f * base * t * (1 + 0.002 * np.sin(2 * np.pi * 5 * t))) * np.exp(-t / dcy)
    scrape = highpass(rng.standard_normal(n), 3000) * np.exp(-t / 0.07) * 0.5
    return (s + scrape) * np.minimum(1, t / 0.002)


def draw_scrape(dur, rising=True):
    """칼날이 칼집을 스치는 긴 금속 마찰음"""
    n = int(dur * SR)
    t = t_arr(n)
    noise = rng.standard_normal(n)
    out = np.zeros(n)
    seg = 12
    step = n // seg
    for k in range(seg):
        frac = k / (seg - 1)
        f = (2200 + 5200 * frac) if rising else (7000 - 4200 * frac)
        part = bandpass(noise[k * step:(k + 1) * step + 2000], f * 0.8, f * 1.35)
        end = min(n, k * step + len(part))
        w = np.hanning(end - k * step)
        out[k * step:end] += part[:end - k * step] * w
    ring = np.sin(2 * np.pi * (3100 + (1800 if rising else -900) * t / dur) * t) * 0.12
    e = np.minimum(1, t / 0.05) * (0.6 + 0.4 * t / dur if rising else 1 - 0.3 * t / dur)
    return (out * 1.2 + ring) * e


def boom(n=int(1.8 * SR), f0=90, f1=32, dcy=0.6):
    t = t_arr(n)
    f = f1 + (f0 - f1) * np.exp(-t / 0.12)
    ph = 2 * np.pi * np.cumsum(f) / SR
    return (np.sin(ph) + 0.25 * np.sin(2 * ph)) * np.exp(-t / dcy) * np.minimum(1, t / 0.003)


def thud(n=int(1.0 * SR)):
    """암전: 둔탁하고 무거운 충격 + 아래로 꺼지는 음"""
    t = t_arr(n)
    f = 30 + 140 * np.exp(-t / 0.035)
    body = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.35)
    knock = lowpass(rng.standard_normal(n), 500) * np.exp(-t / 0.02) * 3
    down = np.sin(2 * np.pi * np.cumsum(220 * np.exp(-t / 0.18) + 25) / SR) * np.exp(-t / 0.5) * 0.35
    return body + knock + down


def taiko(n=int(0.9 * SR), pitch=1.0):
    t = t_arr(n)
    f = (55 + 70 * np.exp(-t / 0.05)) * pitch
    body = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.28)
    skin = lowpass(rng.standard_normal(n), 900) * np.exp(-t / 0.03) * 2.5
    return (body + skin) * 0.9


def snare(n=int(0.35 * SR)):
    t = t_arr(n)
    return (bandpass(rng.standard_normal(n), 1500, 7000) * np.exp(-t / 0.07) * 0.9
            + np.sin(2 * np.pi * 190 * t) * np.exp(-t / 0.05) * 0.6)


def cut(n=int(0.22 * SR), bright=1.0):
    """차원참 한 번: 짧은 '슉' + 날카로운 금속 '칭' + 공간이 찢기는 저음"""
    t = t_arr(n)
    noise = rng.standard_normal(n)
    sweep = np.zeros(n)
    seg = n // 6
    for k in range(6):
        lo = 1800 + k * 1300
        part = bandpass(noise[k * seg:(k + 1) * seg + 1], lo, lo * 2.0)
        sweep[k * seg:k * seg + len(part)] += part[:max(0, n - k * seg)]
    e = np.sin(np.pi * np.clip(t / (n / SR * 0.55), 0, 1)) ** 1.2 * (t < n / SR * 0.55)
    tear = np.sin(2 * np.pi * np.cumsum(90 * np.exp(-t / 0.05) + 40) / SR) * np.exp(-t / 0.08) * 0.9
    ting_at = int(0.06 * SR)
    ting = np.zeros(n)
    tt = t_arr(n - ting_at)
    f = rng.uniform(3500, 6500) * bright
    ting[ting_at:] = (np.sin(2 * np.pi * f * tt) + 0.5 * np.sin(2 * np.pi * f * 1.51 * tt)) * np.exp(-tt / 0.06) * 0.45
    return sweep * e * 1.5 + tear + ting


def click(n=int(0.3 * SR)):
    t = t_arr(n)
    c = np.zeros(n)
    for at, amp, f in [(0.0, 1.0, 3400), (0.026, 0.8, 2700), (0.034, 0.4, 5200)]:
        i = int(at * SR)
        tt = t_arr(n - i)
        c[i:] += amp * (np.sin(2 * np.pi * f * tt) + 0.6 * np.sin(2 * np.pi * f * 1.53 * tt)) * np.exp(-tt / 0.014)
    return c


def glass(n=int(2.8 * SR), count=110):
    out = np.zeros(n)
    for k in range(count):
        at = abs(rng.normal(0, 0.22))
        f = rng.uniform(2500, 12000)
        m = n - int(at * SR)
        if m <= 0:
            continue
        tt = t_arr(m)
        out[int(at * SR):] += rng.uniform(0.05, 0.25) * np.sin(2 * np.pi * f * tt) * np.exp(-tt / rng.uniform(0.04, 0.4))
    crash = highpass(rng.standard_normal(n), 2500) * np.exp(-t_arr(n) / 0.3) * 0.9
    return out + crash


def cymbal(n=int(3.0 * SR), swell=False):
    t = t_arr(n)
    x = highpass(rng.standard_normal(n), 4500)
    if swell:
        return x * (t / t[-1]) ** 2.5 * 0.7
    return x * np.exp(-t / 0.9) * np.minimum(1, t / 0.002) * 0.6


def riser(dur):
    n = int(dur * SR)
    t = t_arr(n)
    f = 200 * (1 + 12 * (t / dur) ** 2)
    tone = saw(1, n) * 0
    tone = np.sin(2 * np.pi * np.cumsum(f) / SR) * 0.3 + bandpass(rng.standard_normal(n), 2000, 9000) * 0.5
    return tone * (t / dur) ** 2


def string_stab(freq, n, bright=2500, dcy=0.09):
    t = t_arr(n)
    s = lowpass(saw(freq, n, 0.004), bright)
    return s * np.minimum(1, t / 0.006) * np.exp(-t / dcy)


def brass(freqs, n, att=0.03, bright=3200):
    t = t_arr(n)
    s = np.zeros(n)
    for f in freqs:
        s += saw(f, n, 0.003)
    s /= len(freqs)
    cutoff_env = 600 + bright * (np.exp(-t / 0.4) * 0.7 + 0.3)
    # 시간에 따라 닫히는 필터 (구간별)
    out = np.zeros(n)
    blk = 2048
    for i in range(0, n, blk):
        out[i:i + blk] = lowpass(s[max(0, i - 512):i + blk], cutoff_env[i])[-min(blk, n - i):]
    return out * np.minimum(1, t / att) * np.clip((t[-1] - t) / 0.3, 0, 1)


def pad(freqs, n, cutoff=1800, att=0.25):
    t = t_arr(n)
    s = np.zeros(n)
    for f in freqs:
        vib = 1 + 0.004 * np.sin(2 * np.pi * 5.2 * t + rng.random() * 6)
        ph = 2 * np.pi * np.cumsum(f * vib) / SR
        s += (2 * ((ph / (2 * np.pi) + rng.random()) % 1) - 1) * 0.5 + 0.5 * np.sin(ph)
    s = lowpass(s / len(freqs), cutoff)
    return s * np.minimum(1, t / att) * np.clip((t[-1] - t) / 0.25, 0, 1)


def choir(freqs, n, att=0.35):
    src = pad(freqs, n, 6000, att)
    return bandpass(src, 600, 1300) * 1.4 + bandpass(src, 2200, 3200) * 0.6


def bell(freq, n=int(2.5 * SR)):
    t = t_arr(n)
    s = np.zeros(n)
    for m, a, d in [(1, 1, 1.4), (2.0, 0.5, 0.9), (2.76, 0.4, 0.6), (5.4, 0.25, 0.3), (0.5, 0.3, 1.8)]:
        s += a * np.sin(2 * np.pi * freq * m * t) * np.exp(-t / d)
    return s * np.minimum(1, t / 0.002)


def heartbeat(n=int(0.6 * SR)):
    t = t_arr(n)
    one = np.sin(2 * np.pi * np.cumsum(45 + 30 * np.exp(-t / 0.03)) / SR) * np.exp(-t / 0.09)
    two = np.zeros(n)
    i = int(0.22 * SR)
    tt = t_arr(n - i)
    two[i:] = np.sin(2 * np.pi * np.cumsum(40 + 25 * np.exp(-tt / 0.03)) / SR) * np.exp(-tt / 0.08) * 0.7
    return lowpass(one + two, 300) * 2


# ---------------------------------------------------------------- YAMATO 음악

def slash_times():
    """0.75 초부터 점점 빨라지며 2.35 초까지 40번 벤다."""
    n = 40
    w = np.array([1.0 / (1 + 1.6 * k / n) for k in range(n)])
    w = w / w.sum() * (2.35 - 0.75)
    ts = 0.75 + np.concatenate([[0], np.cumsum(w)[:-1]])
    return [round(float(x), 3) for x in ts]


SLASH = slash_times()
X_TIMES = [2.38, 2.44]
BREAK = 2.50
CLICK = 3.60
BOOM = 3.70


def yamato(path):
    tr = Track(10.7)
    # 0.00 암전
    tr.add(thud(), 0.0, 0.0, 1.2)
    tr.add(cymbal(int(0.4 * SR), True)[::-1] * 0.4, 0.0, 0.0, 0.0)
    # 0.40 칼을 뽑는다
    tr.add_wide(draw_scrape(0.24, True), 0.40, 0.7)
    tr.add(shing(int(2.0 * SR)), 0.62, 0.0, 0.95)
    tr.add(boom(int(1.4 * SR), 70, 30, 0.5), 0.62, 0.0, 0.7)
    # 0.70~2.35 북과 현악 오스티나토 (점점 빨라지는 차원참 밑에서)
    BPM = 150
    six = 60 / BPM / 4
    seq = ['D2', 'D2', 'D3', 'D2', 'F2', 'D2', 'E2', 'D2', 'D2', 'D2', 'D3', 'D2', 'G2', 'F2', 'E2', 'C#2']
    tm, i = 0.70, 0
    while tm < 2.36:
        f = note(seq[i % len(seq)])
        prog = (tm - 0.7) / 1.66
        acc = 1.0 + 0.6 * (i % 4 == 0)
        tr.add(string_stab(f, int(0.2 * SR), 1600 + 3000 * prog), tm, -0.25, (0.2 + 0.25 * prog) * acc)
        tr.add(string_stab(f * 2, int(0.2 * SR), 2600), tm, 0.3, (0.2 + 0.25 * prog) * 0.4)
        if i % 2 == 0:
            tr.add(taiko(pitch=1.0 + 0.1 * (i % 4 == 0)), tm, rng.uniform(-0.3, 0.3), 0.35 + 0.3 * prog)
        if i % 4 == 2:
            tr.add(snare(), tm, 0.2, 0.25 + 0.25 * prog)
        i += 1
        tm += six * (1 - 0.35 * prog)
    n = int(1.65 * SR)
    t = t_arr(n)
    trem = pad([note('A3'), note('D4'), note('A4'), note('D5')], n, 3500, 1.4) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 16 * t)))
    tr.add(trem * np.linspace(0.08, 0.45, n), 0.70, 0.0, 1.0)
    tr.add(riser(1.75), 0.70, 0.0, 0.5)
    # 차원참
    for k, s in enumerate(SLASH):
        tr.add(cut(bright=1 + 0.01 * k), s - 0.03, float(rng.uniform(-0.85, 0.85)), 0.42 + 0.012 * k)
    for k, s in enumerate(X_TIMES):
        tr.add(cut(int(0.35 * SR), 0.8), s - 0.03, -0.6 if k == 0 else 0.6, 1.1)
        tr.add(boom(int(0.8 * SR), 110, 40, 0.25), s, 0.0, 0.8)
    # 2.50 깨진다
    tr.add(glass(), BREAK, 0.0, 0.9)
    tr.add(boom(int(2.0 * SR), 120, 30, 0.7), BREAK, 0.0, 1.1)
    tr.add(taiko(), BREAK, -0.3, 1.0)
    tr.add(taiko(), BREAK, 0.3, 1.0)
    # 2.55~3.70 흑백 정적: 앞의 소리를 모두 눌러 끄고, 이명 + 느린 심장 + 칼집에 넣는 소리
    tr.duck(2.58, 3.70, 0.03, 0.12)
    n = int(1.15 * SR)
    tt = t_arr(n)
    tr.add(np.sin(2 * np.pi * 7900 * tt) * np.hanning(n) * 0.03, 2.55, 0.0)
    tr.add(pad([note('D2'), note('A2')], n, 300, 0.4) * 0.5, 2.55, 0.0, 0.2)
    tr.add(heartbeat(), 2.72, 0.0, 0.8)
    tr.add(heartbeat(), 3.22, 0.0, 0.6)
    tr.add_wide(draw_scrape(0.62, False), 2.96, 0.32)
    tr.add(click(), CLICK, 0.0, 1.0)
    tr.add(cymbal(int(0.1 * SR) + 1, True), CLICK, 0.0, 0.5)
    # 3.70 대폭발
    tr.add(boom(int(3.6 * SR), 130, 26, 1.4), BOOM, 0.0, 1.5)
    tr.add(glass(int(2.6 * SR), 80), BOOM, 0.0, 0.6)
    tr.add_wide(cymbal(), BOOM, 0.9)
    for p in (-0.4, 0.0, 0.4):
        tr.add(taiko(), BOOM, p, 0.9)
    # 테마: Dm - Bb - C - A (각 2박) → Dm
    BPM2 = 140
    beat = 60 / BPM2
    prog = [['D3', 'F3', 'A3', 'D4'], ['Bb2', 'D3', 'F3', 'Bb3'], ['C3', 'E3', 'G3', 'C4'], ['A2', 'C#3', 'E3', 'A3'], ['D3', 'F3', 'A3', 'D4']]
    for k, ch in enumerate(prog):
        at = BOOM + k * 2 * beat
        length = 2 * beat + (2.4 if k == 4 else 0.12)
        fr = [note(x) for x in ch]
        tr.add(pad(fr, int(length * SR), 2200, 0.05), at, 0.0, 0.42)
        tr.add_wide(choir([f * 2 for f in fr], int(length * SR)), at, 0.55)
        tr.add(pad([fr[0] / 2], int(length * SR), 400, 0.02), at, 0.0, 0.55)
        tr.add(brass(fr[1:], int(min(length, 1.0) * SR)), at, 0.0, 0.35)
    # 높은 현악 선율
    mel = [('A4', 0, 1), ('D5', 1, 1), ('F5', 2, 1.5), ('E5', 3.5, 0.5), ('D5', 4, 1), ('C5', 5, 1), ('E5', 6, 1), ('C#5', 7, 1), ('D5', 8, 2.5)]
    for nm, b, ln in mel:
        at = BOOM + b * beat
        m = int(ln * beat * SR)
        tr.add(pad([note(nm)], m, 4200, 0.04) * env(m, 0.02, 2.0, 0.6), at, 0.15, 0.32)
    pattern = [0, 0.75, 1.0, 1.5, 1.75, 2.0, 2.75, 3.0, 3.25, 3.5, 3.75, 4.0, 4.75, 5.0, 5.5, 5.75, 6.0, 6.5, 6.75, 7.0, 7.25, 7.5, 7.75, 8.0]
    for b in pattern:
        at = BOOM + b * beat
        tr.add(taiko(), at, float(rng.uniform(-0.4, 0.4)), 0.5 + 0.25 * (b % 1 == 0))
        if b % 2 == 1:
            tr.add(snare(), at, 0.2, 0.3)
    tr.add(cymbal(int(1.2 * SR), True), BOOM + 8 * beat - 1.2, 0.0, 0.8)
    tr.add(boom(int(2.8 * SR), 100, 30, 1.1), BOOM + 8 * beat, 0.0, 1.1)
    tr.add(shing(), BOOM + 8 * beat, 0.0, 0.45)
    tr.add_wide(cymbal(), BOOM + 8 * beat, 0.7)
    tr.write(path, fade_from=9.4, drive=1.6)


# ---------------------------------------------------------------- 전설 연출용 효과음

def blackout(path):
    tr = Track(1.6)
    tr.add(thud(int(1.5 * SR)), 0.0, 0.0, 1.2)
    n = int(0.9 * SR)
    t = t_arr(n)
    # 꺼지는 전원 (테이프가 멈추듯 내려가는 음)
    f = 330 * np.exp(-t / 0.25) + 20
    tr.add(np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.4) * 0.35, 0.0, 0.0)
    tr.add(lowpass(saw(55, n, 0.01), 400) * np.exp(-t / 0.3) * 0.5, 0.0, 0.0)
    tr.write(path, drive=1.3, reverb=0.4)


def legendary_pop(path):
    tr = Track(3.6)
    fr = [note(x) for x in ['C3', 'G3', 'C4', 'E4', 'G4']]
    n = int(2.6 * SR)
    tr.add(boom(int(3.0 * SR), 120, 30, 1.0), 0.0, 0.0, 1.3)
    tr.add(taiko(), 0.0, -0.3, 1.0)
    tr.add(taiko(), 0.0, 0.3, 1.0)
    tr.add_wide(cymbal(int(3.2 * SR)), 0.0, 0.9)
    tr.add(brass(fr, n, 0.015, 4200), 0.0, 0.0, 0.8)
    tr.add_wide(choir([f * 2 for f in fr[1:]], int(3.2 * SR), 0.05), 0.0, 0.9)
    tr.add(pad([fr[0] / 2, fr[0]], int(3.0 * SR), 600, 0.01), 0.0, 0.0, 0.7)
    for k, f in enumerate([note('C6'), note('E6'), note('G6'), note('C7')]):
        tr.add(bell(f, int(2.4 * SR)) * 0.3, 0.05 + k * 0.07, -0.5 + k * 0.33, 0.6)
    tr.add(shing(int(2.0 * SR), 1.2), 0.0, 0.0, 0.35)
    tr.write(path, fade_from=2.8, drive=1.5)


if __name__ == '__main__':
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    yamato(os.path.join(out, 'yamato.wav'))
    blackout(os.path.join(out, 'blackout.wav'))
    legendary_pop(os.path.join(out, 'legendary_pop.wav'))
    print('SLASH_TIMES =', SLASH)
