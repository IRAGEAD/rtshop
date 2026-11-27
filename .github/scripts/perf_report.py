"""성능 측정 보고서 (3.70.0): JFR 의 CPU 표본을 모드별로 모은다.

python3 perf_report.py <기록.jfr> <모드 폴더> <단계 파일(rtshop-perf-*.txt)> [단계 파일 ...]

- 단계 파일의 "PHASE 이름 시작 끝 ..." 줄로 표본을 단계별로 나눈다 (시각은 JFR 과 같은 시간대의 시:분:초.밀리초).
- 스레드 무리: server(Server thread) · render(Render thread) · worldgen(Worker-Main-*) · dh(Distant Horizons) · other.
- 무리마다: 표본 수, 모드별 '포함'(쌓인 호출 어디든 그 모드가 있음) · '자기'(가장 위의 JDK 아닌 칸) 몫, 가장 바쁜 메서드.
"""
import collections
import os
import re
import subprocess
import sys
import zipfile

jfr, modsdir = sys.argv[1], sys.argv[2]
marks = sys.argv[3:]

# ---- 패키지 → 모드 (jar 이름에서 버전을 떼어 낸 것)
def label_of_jar(name):
    base = name[:-4]
    base = re.sub(r'[-_+ ]?(?:mc|forge|neoforge|fabric)?[-_+ ]?1\.20(?:\.1)?.*$', '', base, flags=re.I)
    base = re.sub(r'[-_ ]v?\d+(?:\.\d+)+.*$', '', base)
    return base or name[:-4]

PKG = {}
for f in sorted(os.listdir(modsdir)):
    if not f.endswith('.jar'):
        continue
    lab = label_of_jar(f)
    try:
        z = zipfile.ZipFile(os.path.join(modsdir, f))
    except Exception:
        continue
    for n in z.namelist():
        if n.endswith('.class') and '/' in n and not n.startswith('META-INF'):
            PKG.setdefault(n.rsplit('/', 1)[0].replace('/', '.'), lab)

FIXED = [('net.minecraft.', 'minecraft'), ('com.mojang.', 'minecraft'), ('net.minecraftforge.', 'forge'), ('cpw.mods.', 'forge'),
         ('java.', 'jdk'), ('javax.', 'jdk'), ('jdk.', 'jdk'), ('sun.', 'jdk'), ('com.sun.', 'jdk'), ('org.lwjgl.', 'lwjgl'),
         ('it.unimi.', 'fastutil'), ('com.google.', 'google-libs'), ('io.netty.', 'netty'), ('org.spongepowered.', 'mixin'),
         ('org.slf4j.', 'logging'), ('org.apache.logging.', 'logging'), ('org.joml.', 'joml'), ('oshi.', 'oshi'), ('kotlin.', 'kotlin')]
CACHE = {}

def owner(cls):
    if cls in CACHE:
        return CACHE[cls]
    r = None
    for pre, l in FIXED:
        if cls.startswith(pre):
            r = l
            break
    if r is None:
        p = cls.rsplit('.', 1)[0] if '.' in cls else ''
        while p:
            if p in PKG:
                r = PKG[p]
                break
            if '.' not in p:
                break
            p = p.rsplit('.', 1)[0]
    if r is None:
        r = '?' + (cls.rsplit('.', 1)[0] if '.' in cls else cls)[:48]
    CACHE[cls] = r
    return r

BORING = {'jdk', 'fastutil', 'google-libs', 'logging', 'joml', 'kotlin'}
BASE = {'minecraft', 'forge', 'jdk', 'lwjgl', 'fastutil', 'google-libs', 'netty', 'mixin', 'logging', 'joml', 'oshi', 'kotlin'}

def group(t):
    if t == 'Server thread':
        return 'server'
    if t in ('Render thread', 'main'):   # JFR 은 이름이 바뀌기 전의 'main' 으로 적기도 한다 (클라이언트의 화면 스레드)
        return 'render'
    if t.startswith('Worker-Main') or 'worldgen' in t.lower():
        return 'worldgen'
    if t.startswith('DH') or 'Distant' in t or 'LOD' in t:
        return 'dh'
    return 'other'

# ---- 단계
PHASES = []
def secs(hms):
    h, m, s = hms.split(':')
    return int(h) * 3600 + int(m) * 60 + float(s)
for mf in marks:
    try:
        for line in open(mf, encoding='utf-8'):
            m = re.match(r'PHASE (\S+) (\d\d:\d\d:\d\d\.\d+) (\d\d:\d\d:\d\d\.\d+)', line)
            if m:
                PHASES.append((m.group(1), secs(m.group(2)), secs(m.group(3)), line.strip()))
    except Exception as e:
        print('(no phase file', mf, e, ')')

def phase_of(t):
    for name, a, b, _ in PHASES:
        if a <= t <= b:
            return name
    return '(between phases)'

# ---- 표본 읽기
incl = collections.defaultdict(collections.Counter)   # (phase, group) -> mod -> samples
selfc = collections.defaultdict(collections.Counter)
meth = collections.defaultdict(collections.Counter)
total = collections.Counter()
threads = collections.defaultdict(collections.Counter)   # (phase, group) -> 스레드 이름(번호 뗀 것) -> 표본
# 모드마다: 그 모드의 칸 중 쌓인 호출의 가장 바깥(무엇이 그 모드의 일을 부르나) · 가장 안쪽(그 모드가 어디서 시간을 쓰나)
outer = collections.defaultdict(lambda: collections.defaultdict(collections.Counter))
inner = collections.defaultdict(lambda: collections.defaultdict(collections.Counter))
# 바닐라 클래스에 섞여 들어간 mixin 처리기(handler$zza000$modid$name 등)는 그 모드의 칸으로 센다
MIXIN_RE = re.compile(r'^(?:handler|redirect|modify\w*|wrap\w*|localvar|constant)\$[0-9a-z]+\$([a-z0-9_]+)\$')
proc = subprocess.Popen(['jfr', 'print', '--events', 'jdk.ExecutionSample', '--stack-depth', '48', jfr], stdout=subprocess.PIPE,
                        stderr=subprocess.DEVNULL, text=True, errors='replace')
thread, t, frames, inst = '', 0.0, [], False
frame_re = re.compile(r"^\s+([\w$./]+)\.([\w$<>]+)\(")
def flush():
    if not frames:
        return
    key = (phase_of(t), group(thread))
    total[key] += 1
    threads[key][re.sub(r'[-#]?\d+$', '', thread)] += 1
    owners = []
    for c, mname in frames:
        mm = MIXIN_RE.match(mname)
        owners.append('mixin:' + mm.group(1) if mm else owner(c))
    for o in set(owners):
        incl[key][o] += 1
    seen = set()
    for (c, mname), o in zip(frames, owners):
        if o in BASE or o in seen:
            continue
        seen.add(o)
        inner[key][o][c + '.' + mname] += 1
    seen = set()
    for (c, mname), o in reversed(list(zip(frames, owners))):
        if o in BASE or o in seen:
            continue
        seen.add(o)
        outer[key][o][c + '.' + mname] += 1
    for (c, mname), o in zip(frames, owners):
        if o not in BORING:
            selfc[key][o] += 1
            meth[key][c + '.' + mname] += 1
            break
for line in proc.stdout:
    if line.startswith('jdk.ExecutionSample'):
        flush()
        thread, t, frames, inst = '', 0.0, [], False
    elif 'startTime = ' in line and not inst:
        m = re.search(r'(\d\d:\d\d:\d\d\.\d+)', line)
        if m:
            t = secs(m.group(1))
    elif 'sampledThread = ' in line:
        m = re.search(r'"([^"]*)"', line)
        thread = m.group(1) if m else ''
    elif line.strip().startswith('stackTrace = ['):
        inst = True
    elif inst:
        if line.strip() == ']':
            inst = False
        else:
            m = frame_re.match(line)
            if m:
                frames.append((m.group(1), m.group(2)))
flush()
proc.wait()

# ---- 출력
print('JFR', jfr, '· mods dir', modsdir, '· packages mapped', len(PKG))
for _, _, _, line in PHASES:
    print(line)
print()
order = [p[0] for p in PHASES] + ['(between phases)']
for ph in order:
    for g in ('server', 'render', 'worldgen', 'dh', 'other'):
        key = (ph, g)
        n = total[key]
        if n < (15 if g == 'render' else 50):
            continue
        print('=== phase %s · %s thread(s) · %d samples' % (ph, g, n))
        print('  threads:', ', '.join('%s %.0f%%' % (tn[:40], 100.0 * c / n) for tn, c in threads[key].most_common(8)))
        mods = [(o, c) for o, c in incl[key].most_common() if o not in BASE]
        print('  mods anywhere in the stack (inclusive %):', ', '.join('%s %.1f' % (o, 100.0 * c / n) for o, c in mods[:22]))
        base = [(o, c) for o, c in incl[key].most_common() if o in BASE]
        print('  base:', ', '.join('%s %.1f' % (o, 100.0 * c / n) for o, c in base[:8]))
        print('  top of the stack (self %):', ', '.join('%s %.1f' % (o, 100.0 * c / n) for o, c in selfc[key].most_common(14)))
        print('  busiest methods:')
        for mname, c in meth[key].most_common(12):
            print('    %5.1f%%  %s' % (100.0 * c / n, mname[-150:]))
        heavy = [(o, c) for o, c in mods if 100.0 * c / n >= 3.0][:6]
        for o, c in heavy:
            print('  [%s %.1f%%] called from:' % (o, 100.0 * c / n), ' | '.join('%.1f %s' % (100.0 * k / n, m[-110:]) for m, k in outer[key][o].most_common(4)))
            print('  [%s] working in:' % o, ' | '.join('%.1f %s' % (100.0 * k / n, m[-110:]) for m, k in inner[key][o].most_common(4)))
        print()
