"""셰이더 사진(3.64.2)에 쓸 팩을 고르고 client/shaderpacks 에 넣는다. 쓸 순서대로 파일 이름을 한 줄씩 찍는다.

사용자는 Photon 으로 플레이하므로 Photon 이 먼저다. 그런데 CI 의 소프트웨어 그리기(llvmpipe)는 계산 셰이더의 공유 메모리가
32KB 뿐이라 Photon 의 계산 패스 deferred4_a(약 36KB)가 만들어지지 않고, 그러면 Oculus 가 셰이더 팩을 통째로 끈다.
그래서 Photon 은 그 계산 패스만 끈 사본(shaders.properties 에 program.<세계>/<패스>.enabled=false)을 넣는다.
끄는 패스는 둘째 인자(쉼표로 여러 개)로 바꿀 수 있다. Photon 이 그래도 안 켜지면 같은 Oculus 경로를 타는 다른 팩으로 찍는다.
"""
import os
import re
import shutil
import sys
import zipfile

src, dst = sys.argv[1], sys.argv[2]
off = [p for p in (sys.argv[3] if len(sys.argv) > 3 else "deferred4_a").split(",") if p]
os.makedirs(dst, exist_ok=True)
names = sorted(os.listdir(src))
order = []
for pat in (r"^photon.*\.zip$", r"^complementaryreimagined.*\.zip$", r"^bsl.*\.zip$"):
    for n in names:
        if re.match(pat, n, re.I):
            order.append(n)
            break
for n in order:
    out = os.path.join(dst, n)
    if not n.lower().startswith("photon"):
        shutil.copy(os.path.join(src, n), out)
        print(n)
        continue
    with zipfile.ZipFile(os.path.join(src, n)) as zin:
        files = zin.namelist()
        props = next((f for f in files if re.search(r"(^|/)shaders/shaders\.properties$", f)), None)
        if props is None:
            shutil.copy(os.path.join(src, n), out)
            print(n)
            continue
        root = props[: -len("shaders.properties")]
        lines = []
        for f in files:
            if not f.startswith(root) or not f.endswith(".csh"):
                continue
            prog = f[len(root):-4]           # world0/deferred4_a
            if prog.split("/")[-1] in off:
                lines.append("program.%s.enabled=false" % prog)
        sys.stderr.write("photon compute passes: %s\n" % ", ".join(sorted(f[len(root):] for f in files if f.startswith(root) and f.endswith(".csh"))))
        sys.stderr.write("photon passes turned off for llvmpipe: %s\n" % (", ".join(lines) or "none found"))
        with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as zout:
            for info in zin.infolist():
                data = zin.read(info.filename)
                if info.filename == props and lines:
                    data = data.rstrip(b"\n") + ("\n# rtshop CI (llvmpipe): compute passes over 32KB shared memory\n" + "\n".join(lines) + "\n").encode()
                zout.writestr(info, data)
    print(n)
