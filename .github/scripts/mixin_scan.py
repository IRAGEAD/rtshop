#!/usr/bin/env python3
"""어떤 모드가 주민 거래(VillagerTrades)를 Mixin 으로 고치는지 찾는다. jar 안에 든 jar 도 본다.

쓰기: python3 .github/scripts/mixin_scan.py <모드 폴더>
"""
import io
import os
import sys
import zipfile

NEEDLES = (b'VillagerTrades$EmeraldsForVillagerTypeItem', b'TradeOffers$TypeAwareBuyForOneEmeraldFactory')


def scan(z, label, depth=0):
    for e in z.namelist():
        low = e.lower()
        if low.endswith('.jar') and depth < 3:
            try:
                scan(zipfile.ZipFile(io.BytesIO(z.read(e))), label + ' > ' + e.rsplit('/', 1)[-1], depth + 1)
            except Exception as ex:  # noqa: BLE001
                print('skip', label, e, ex)
        elif low.endswith('.class') and 'mixin' in low:
            b = z.read(e)
            if any(n in b for n in NEEDLES):
                print(label, '|', e)
        elif low.endswith('.json') and 'mixin' in low and depth >= 0:
            b = z.read(e)
            if b'VillagerTrades' in b or b'TradeOffers' in b:
                print(label, '| (config)', e)


d = sys.argv[1]
for n in sorted(os.listdir(d)):
    if n.endswith('.jar'):
        try:
            scan(zipfile.ZipFile(os.path.join(d, n)), n)
        except Exception as ex:  # noqa: BLE001
            print('skip', n, ex)
