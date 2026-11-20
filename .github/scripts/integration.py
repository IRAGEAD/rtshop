"""CI helper: build a Forge 1.20.1 test server with the cooking / farming / fishing mods of the modpack
(downloaded from Modrinth together with their required dependencies) so the RT Shop self-test can run
next to the real mods. Usage: integration.py <mods dir>"""
import json, os, sys, urllib.parse, urllib.request

UA = {"User-Agent": "IRAGEAD/rtshop integration test (GitHub Actions)"}
API = "https://api.modrinth.com/v2"
OUT = sys.argv[1]
START = ["farmers-delight", "kaleidoscope-cookery", "kaleidoscopetavern", "handson-delight", "lets-do-farm-charm",
         "starcatcher", "better-farming-plus"] + sys.argv[2:]


def get(url):
    return urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=180).read()


def get_json(url):
    return json.loads(get(url))


os.makedirs(OUT, exist_ok=True)
loaders = urllib.parse.quote('["forge"]')
gvs = urllib.parse.quote('["1.20.1"]')
queue = list(START)
seen = set()
while queue:
    ref = queue.pop(0)
    try:
        project = get_json(f"{API}/project/{urllib.parse.quote(ref)}")
    except Exception as ex:
        print("MISSING", ref, ex)
        continue
    if project["id"] in seen:
        continue
    seen.add(project["id"])
    try:
        versions = get_json(f"{API}/project/{project['id']}/version?loaders={loaders}&game_versions={gvs}")
        if not versions:
            print("NOVERSION", project["slug"])
            continue
        v = versions[0]
        f = next((x for x in v["files"] if x.get("primary")), v["files"][0])
        with open(os.path.join(OUT, f["filename"]), "wb") as fh:
            fh.write(get(f["url"]))
        deps = [d["project_id"] for d in v.get("dependencies", []) if d.get("dependency_type") == "required" and d.get("project_id")]
        print("OK", project["slug"], v["version_number"], f["filename"], "deps:", deps)
        queue.extend(deps)
    except Exception as ex:
        print("ERROR", project["slug"], ex)
print("mods:", sorted(os.listdir(OUT)))
