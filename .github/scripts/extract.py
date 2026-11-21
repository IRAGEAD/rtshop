import zipfile, os, json, glob, re

WANT = ["epic-fight-2", "epicfight-extra", "irons_spellbooks", "kaleidoscopecookery", "kaleidoscopetavern", "handson_delight",
        "starcatcher", "wizards", "archers", "paladins", "rogues", "miningmaster", "spelunkery", "letsdo-farm_and_charm",
        "better_farming", "farmersdelight", "weaponsofmiracles", "spmovesets", "skill_tree", "efs_iss", "irons_voicebound",
        "ef_knuckles", "lrtactical"]
out = "modinfo"
os.makedirs(out, exist_ok=True)
for jar in sorted(glob.glob("packcache/*.jar")):
    n = os.path.basename(jar).lower()
    if not any(n.startswith(w) for w in WANT):
        continue
    d = os.path.join(out, os.path.basename(jar)[:-4])
    os.makedirs(d, exist_ok=True)
    z = zipfile.ZipFile(jar)
    items, recipes = [], {}
    for e in z.namelist():
        keep = (re.match(r"assets/[^/]+/lang/(en_us|ko_kr)\.json$", e)
                or ("patchouli_books" in e and e.endswith((".json", ".md")))
                or e == "META-INF/mods.toml"
                or re.match(r"data/[^/]+/(tags/items|advancements)/.*\.json$", e)
                or (re.match(r"assets/[^/]+/(guide|book|books|manual|guidebook)/", e) and e.endswith((".json", ".md", ".txt"))))
        if keep:
            p = os.path.join(d, e)
            os.makedirs(os.path.dirname(p), exist_ok=True)
            open(p, "wb").write(z.read(e))
        m = re.match(r"assets/([^/]+)/models/item/(.+)\.json$", e)
        if m:
            items.append(m.group(1) + ":" + m.group(2))
        m = re.match(r"data/([^/]+)/recipes?/(.+)\.json$", e)
        if m:
            try:
                t = json.loads(z.read(e)).get("type", "?")
            except Exception:
                t = "?"
            recipes.setdefault(t, []).append(m.group(1) + ":" + m.group(2))
    open(os.path.join(d, "items.txt"), "w").write("\n".join(sorted(items)))
    open(os.path.join(d, "recipes.json"), "w").write(json.dumps({k: sorted(v) for k, v in recipes.items()}, indent=1))
    print(n, len(items), "items", sum(len(v) for v in recipes.values()), "recipes")
