#!/bin/bash
mkdir -p /tmp/inspect2
curl -fsSL -o /tmp/cfr.jar https://repo1.maven.org/maven2/org/benf/cfr/0.152/cfr-0.152.jar || true
# 해독제 아이템 찾기 + Spell Engine(RPG Series) 주문 비용 구조 조사 (Iron's Spells 마나로 통일하려고)
{
  for J in packcache/*.jar; do
    for L in $(unzip -l "$J" 2>/dev/null | awk '{print $4}' | grep -E "assets/[^/]+/lang/(en_us|ko_kr)\.json$"); do
      unzip -p "$J" "$L" 2>/dev/null | grep -iE "antidote|해독|cure poison" | head -n 8 | sed "s|^|$(basename "$J") $L: |"
    done
  done
} > /tmp/inspect2/antidote.txt 2>&1 || true
{
  SEJ="$(ls packcache/*.jar | grep -i "spell_engine" | head -1)"; echo "jar: $SEJ"
  unzip -l "$SEJ" | awk '{print $4}' | grep -E "\.class$" | grep -viE "mixin/client|client/gui" | head -n 400
  echo "==== mods.toml"; unzip -p "$SEJ" META-INF/mods.toml | head -n 60
  WZJ="$(ls packcache/*.jar | grep -i "wizards-forge" | head -1)"
  echo "==== wizards spells"; unzip -l "$WZJ" | awk '{print $4}' | grep -E "spell|weapon_attributes" | head -n 60
  for f in $(unzip -l "$WZJ" | awk '{print $4}' | grep -E "data/wizards/spells?/.*json$" | head -n 3); do echo "---- $f"; unzip -p "$WZJ" "$f" | head -c 2500; echo; done
  for f in $(unzip -l "$WZJ" | awk '{print $4}' | grep -E "spell_assignments?/.*json$|weapon.*json$" | head -n 3); do echo "---- $f"; unzip -p "$WZJ" "$f" | head -c 800; echo; done
} > /tmp/inspect2/spellengine.txt 2>&1 || true
(SEJ="$PWD/$(ls packcache/*.jar | grep -i "spell_engine" | head -1)"; mkdir -p /tmp/se && cd /tmp/se && unzip -o -q "$SEJ" && \
  java -jar /tmp/cfr.jar "$SEJ" --outputdir /tmp/inspect2/spellengine-src --silent true --jarfilter ".*(SpellHelper|SpellCast|SpellContainer|Spell\$|Spell$|AmmoUtil|SpellCooldown|SpellCasterEntity|SpellCastSyncHelper|ServerNetwork|Packets|SpellEvents|api.event.Event|StagedEvent|SpellExecution|cost.Ammo|SpellRegistry|HudMessages|HudRenderHelper).*" > /tmp/inspect2/spellengine-cfr.log 2>&1) || echo "se cfr failed"
# 3.41: Qliphoth Awakening 조사 (2·3장 마무리 보스에 쓰려고) + Spiral Abyss 층 입장 흐름 (층 모드를 이야기 때만 열려고)
QJ="$(ls packcache/*.jar | grep -i "fdbosses" | head -1)"
{
    echo "######## qliphoth jar: $QJ"
  if [ -n "$QJ" ]; then
    unzip -p "$QJ" META-INF/mods.toml | head -n 60
    echo "==== files (data/assets, no textures/sounds/models/animations)"; unzip -l "$QJ" | awk '{print $4}' | grep -E "^(data|assets)/" | grep -vE "\.(png|ogg|mcmeta)$|/models/|/animations/|/geo/|/particles/|/blockstates/" | head -n 900
    echo "==== classes"; unzip -l "$QJ" | awk '{print $4}' | grep -E "\.class$" | grep -v '\$' | head -n 700
    for L in $(unzip -l "$QJ" | awk '{print $4}' | grep -E "assets/[^/]+/lang/en_us\.json$"); do echo "==== $L"; unzip -p "$QJ" "$L" | head -c 60000; echo; done
    for L in $(unzip -l "$QJ" | awk '{print $4}' | grep -E "data/.*/(worldgen/structure|worldgen/structure_set|dimension|dimension_type|advancements|loot_tables/entities|tags/worldgen|tags/entity_types)/.*\.json$" | head -n 120); do echo "---- $L"; unzip -p "$QJ" "$L" | head -c 1600; echo; done
  fi
} > /tmp/inspect2/qliphoth.txt 2>&1 || true
if [ -n "$QJ" ]; then java -jar /tmp/cfr.jar "$PWD/$QJ" --outputdir /tmp/inspect2/src-qliphoth --silent true > /dev/null 2>&1 || echo "qliphoth cfr failed"; fi
SAJ="$(ls packcache/*.jar | grep -iE "spiral.?abyss" | head -1)"
if [ -n "$SAJ" ]; then java -jar /tmp/cfr.jar "$PWD/$SAJ" --outputdir /tmp/inspect2/src-spiral --silent true --jarfilter ".*(FloorController|PortalParties|AbyssNetwork|AbyssParties|ArenaSession|EndlessWaves|AbyssPlayers|GuestFloorRules|FloorProgress|AbyssCycle|QliphothEncounters|AbyssConfig).*" > /dev/null 2>&1 || echo "spiral cfr failed"; fi
