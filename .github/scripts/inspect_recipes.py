"""3.65.0 가격 점검: 지정한 아이템을 결과로 내는 조합법 JSON 을 모드 jar 에서 찾아 적는다 (돈 불리기 길이 진짜인지 보려고)."""
import json, sys, zipfile, glob, os

targets = sys.argv[2].split(",")
out = []
for jar in sorted(glob.glob(os.path.join(sys.argv[1], "*.jar"))):
    try:
        z = zipfile.ZipFile(jar)
    except Exception:
        continue
    for n in z.namelist():
        if "/recipes/" not in n or not n.endswith(".json") or not n.startswith("data/"):
            continue
        try:
            txt = z.read(n).decode("utf-8", "replace")
            o = json.loads(txt)
        except Exception:
            continue
        if not isinstance(o, dict):
            continue
        res = json.dumps({k: o.get(k) for k in ("result", "results", "output", "outputs") if k in o})
        hit = [t for t in targets if ('"' + t + '"') in res]
        if hit:
            out.append("######## %s | %s | %s\n%s" % (os.path.basename(jar), n, ",".join(hit), txt[:1800]))
print("\n".join(out) if out else "no recipes found")
