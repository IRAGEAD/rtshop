"""CI helper: download the public Forge 1.20.1 jars of a few mods from Modrinth and extract only
their data files (recipes, tags, loot tables) and language files, so prices can be designed
against the real recipes. The result is uploaded to a private draft release and deleted afterwards."""
import io, json, os, sys, tarfile, urllib.parse, urllib.request, zipfile

UA = {"User-Agent": "IRAGEAD/rtshop refdata helper (GitHub Actions)"}
API = "https://api.modrinth.com/v2"


def get(url):
    return urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=120).read()


def get_json(url):
    return json.loads(get(url))


MODS = {
    "farmersdelight": {"slugs": ["farmers-delight"], "query": "Farmer's Delight"},
    "kaleidoscope_cookery": {"slugs": ["kaleidoscope-cookery"], "query": "Kaleidoscope Cookery"},
    "kaleidoscope_tavern": {"slugs": ["kaleidoscope-tavern"], "query": "Kaleidoscope Tavern"},
    "handson_delight": {"slugs": ["handson-delight", "hands-on-delight"], "query": "Hands-On Delight"},
    "farm_and_charm": {"slugs": ["lets-do-farm-charm", "farm-and-charm"], "query": "Farm & Charm"},
    "starcatcher": {"slugs": ["starcatcher"], "query": "Starcatcher"},
    "immersive_eating": {"slugs": ["kaleidoscope-immersive-eating"], "query": "Kaleidoscope Immersive Eating"},
    "better_farming": {"slugs": ["better-farming", "better-farming++"], "query": "Better Farming"},
    "fancy_crops": {"slugs": ["fancy-crops", "bees-fancy-crops"], "query": "Fancy Crops"},
    "selling_bin": {"slugs": ["selling-bin", "wds-selling-bin"], "query": "Selling Bin"},
    "mining_master": {"slugs": ["mining-master"], "query": "Mining Master"},
    "spelunkery": {"slugs": ["spelunkery"], "query": "Spelunkery"},
}

out_dir = "refdata"
os.makedirs(out_dir, exist_ok=True)
index = {}

for label, spec in MODS.items():
    info = {"label": label}
    project = None
    for slug in spec["slugs"]:
        try:
            project = get_json(f"{API}/project/{urllib.parse.quote(slug)}")
            break
        except Exception as ex:
            info.setdefault("slug_errors", []).append(f"{slug}: {ex}")
    if project is None:
        try:
            q = urllib.parse.quote(spec["query"])
            facets = urllib.parse.quote('[["project_type:mod"],["versions:1.20.1"],["categories:forge"]]')
            hits = get_json(f"{API}/search?query={q}&limit=6&facets={facets}")["hits"]
            info["search_hits"] = [(h["slug"], h["title"], h["author"]) for h in hits]
            want = spec["query"].lower().replace("'", "").replace("-", " ")
            for h in hits:
                if want in h["title"].lower().replace("'", "").replace("-", " "):
                    project = get_json(f"{API}/project/{h['slug']}")
                    break
        except Exception as ex:
            info["search_error"] = str(ex)
    if project is None:
        index[label] = info
        print("MISSING", label, json.dumps(info, ensure_ascii=False))
        continue
    info["slug"] = project["slug"]
    info["title"] = project["title"]
    try:
        loaders = urllib.parse.quote('["forge"]')
        gvs = urllib.parse.quote('["1.20.1"]')
        versions = get_json(f"{API}/project/{project['id']}/version?loaders={loaders}&game_versions={gvs}")
        if not versions:
            info["error"] = "no forge 1.20.1 version"
            index[label] = info
            print("NOVERSION", label)
            continue
        v = versions[0]
        f = next((x for x in v["files"] if x.get("primary")), v["files"][0])
        info["version"] = v["version_number"]
        info["file"] = f["filename"]
        info["dependencies"] = v.get("dependencies", [])
        raw = get(f["url"])
        z = zipfile.ZipFile(io.BytesIO(raw))
        n = 0
        for name in z.namelist():
            keep = name.startswith("data/") or name == "META-INF/mods.toml" or (
                name.startswith("assets/") and "/lang/" in name and name.rsplit("/", 1)[-1] in ("en_us.json", "ko_kr.json"))
            if not keep or name.endswith("/"):
                continue
            if name.endswith((".nbt", ".png", ".ogg", ".mcfunction")) or "/structures/" in name or "/worldgen/" in name:
                continue
            dest = os.path.join(out_dir, label, name)
            os.makedirs(os.path.dirname(dest), exist_ok=True)
            with open(dest, "wb") as fh:
                fh.write(z.read(name))
            n += 1
        info["extracted"] = n
        info["jarjar"] = [x for x in z.namelist() if x.startswith("META-INF/jarjar/") and x.endswith(".jar")]
        print("OK", label, info["version"], info["file"], n)
    except Exception as ex:
        info["error"] = str(ex)
        print("ERROR", label, ex)
    index[label] = info

# source jars of the two Kaleidoscope mods (to read how rice bowls / barrels consume ingredients)
for label in ("kaleidoscope_cookery", "kaleidoscope_tavern"):
    try:
        slug = index[label]["slug"]
        loaders = urllib.parse.quote('["forge"]')
        gvs = urllib.parse.quote('["1.20.1"]')
        v = get_json(f"{API}/project/{slug}/version?loaders={loaders}&game_versions={gvs}")[0]
        for f in v["files"]:
            if f["filename"].endswith("-sources.jar"):
                z = zipfile.ZipFile(io.BytesIO(get(f["url"])))
                n = 0
                for name in z.namelist():
                    if name.endswith(".java"):
                        dest = os.path.join(out_dir, label + "_src", name)
                        os.makedirs(os.path.dirname(dest), exist_ok=True)
                        with open(dest, "wb") as fh:
                            fh.write(z.read(name))
                        n += 1
                print("SRC", label, n)
    except Exception as ex:
        print("SRCERROR", label, ex)

# vanilla 1.20.1 data (recipes, item tags, crop loot tables, item names)
try:
    man = get_json("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
    vurl = next(v["url"] for v in man["versions"] if v["id"] == "1.20.1")
    client = get_json(vurl)["downloads"]["client"]["url"]
    z = zipfile.ZipFile(io.BytesIO(get(client)))
    n = 0
    for name in z.namelist():
        keep = (name.startswith("data/minecraft/recipes/") or name.startswith("data/minecraft/tags/items/")
                or name.startswith("data/minecraft/tags/blocks/") or name.startswith("data/minecraft/loot_tables/blocks/")
                or name == "assets/minecraft/lang/en_us.json")
        if keep and not name.endswith("/"):
            dest = os.path.join(out_dir, "minecraft", name)
            os.makedirs(os.path.dirname(dest), exist_ok=True)
            with open(dest, "wb") as fh:
                fh.write(z.read(name))
            n += 1
    print("VANILLA", n)
except Exception as ex:
    print("VANILLAERROR", ex)

with open(os.path.join(out_dir, "index.json"), "w", encoding="utf-8") as fh:
    json.dump(index, fh, ensure_ascii=False, indent=1)
with tarfile.open("refdata.tar.gz", "w:gz") as tar:
    tar.add(out_dir)
print("done", os.path.getsize("refdata.tar.gz"))
