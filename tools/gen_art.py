import sys
from PIL import Image

PAL = {
 '.': (0, 0, 0, 0),
 'k': (0x2B, 0x19, 0x0B, 255),   # outline
 'G': (0xFF, 0xE8, 0x9A, 255),   # gold highlight
 'g': (0xE8, 0xB8, 0x4A, 255),   # gold
 'd': (0xA8, 0x7A, 0x22, 255),   # gold shade
 'D': (0x7A, 0x54, 0x12, 255),   # dark gold (engraving)
 'p': (0xF3, 0xE6, 0xC2, 255),   # paper
 'q': (0xD9, 0xC5, 0x92, 255),   # paper shade
 'i': (0x8A, 0x70, 0x48, 255),   # ink
 'r': (0xB0, 0x2E, 0x26, 255),   # red seal
 'R': (0xE0, 0x5A, 0x48, 255),
 'e': (0x2F, 0x7A, 0x2A, 255),   # green
 'E': (0x6F, 0xC0, 0x5A, 255),
 'b': (0x2E, 0x4F, 0x9E, 255),   # blue
 'B': (0x6E, 0x96, 0xE0, 255),
 'w': (0x5C, 0x3A, 0x1E, 255),   # wood/brown
}

def make(rows, path):
    assert len(rows) == 16, len(rows)
    im = Image.new('RGBA', (16, 16))
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row, len(row))
        for x, ch in enumerate(row):
            im.putpixel((x, y), PAL[ch])
    im.save(path)
    return im

out = sys.argv[1]
prev = sys.argv[2]

# Gold 수표: 금화(G) + 종이 쪽지
check = [
 "....kkkkkk......",
 "..kkGGGGggkk....",
 ".kGGGggggggdk...",
 ".kGggDDDDggdk...",
 "kGggDDggggggdk..",
 "kGggDgggggggdk..",
 "kGggDggDDDggdkkk",
 "kgggDgggDDggdkpk",
 "kgggDDggDDgddkpk",
 "kggggDDDDDgddkpk",
 ".kgggggggdddkppk",
 ".kdgggddddddkiqk",
 "..kkddddddkkppqk",
 "...kkkkkkkkiipqk",
 "...kpppppppppqqk",
 "...kkkkkkkkkkkkk",
]
# 토지 문서 공통 틀: 종이 + 글줄 + 봉인
def deed(seal_hi, seal, ribbon):
    rows = [
     "..kkkkkkkkkk....",
     ".kppppppppppk...",
     ".kpiiiiiippqqk..",
     ".kppppppppppqk..",
     ".kpiiiippwwpqk..",
     ".kpppppwwwwpqk..",
     ".kpiiipwppwpqk..",
     ".kpppppwwwwpqk..",
     ".kpiiiiiiippqk..",
     ".kpppppppppSSSk.",
     ".kpiiiippppSsssk",
     ".kppppppppSssssk",
     ".kqqqqqqqqkssssk",
     "..kkkkkkkkksksk.",
     "..........kTkTk.",
     "..........kk.kk.",
    ]
    res = []
    for r in rows:
        res.append(r.replace('S', seal_hi).replace('s', seal).replace('T', ribbon))
    return res

files = {
 'gold_check': check,
 'land_deed': deed('R', 'r', 'r'),
 'first_land_deed': deed('E', 'e', 'e'),
 'large_land_deed': deed('B', 'b', 'b'),
}
sheet = Image.new('RGBA', (16 * 16 * len(files) + 16 * (len(files) + 1), 16 * 16 + 32), (0x8B, 0x8B, 0x8B, 255))
x = 16
for name, rows in files.items():
    im = make(rows, f"{out}/{name}.png")
    big = im.resize((256, 256), Image.NEAREST)
    sheet.paste(big, (x, 16), big)
    x += 256 + 16
sheet.save(prev)
print("ok")
