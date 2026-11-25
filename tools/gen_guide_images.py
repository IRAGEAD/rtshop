# 가이드북에 넣는 그림: python3 tools/gen_guide_images.py
#   CI 가 모드팩 전체에서 찍은 화면(브랜치 ci-fullpack-screenshots)을 줄여서 assets/rtshop/textures/guide/ 에 넣는다.
#   (먼저: git fetch origin '+refs/heads/ci-fullpack-screenshots:refs/remotes/origin/ci-fullpack-screenshots')
import io, os, subprocess
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'rtshop', 'textures', 'guide')
REF = 'origin/ci-fullpack-screenshots'
W, H = 480, 270
# 가이드 그림 이름 → 화면 이름
SHOTS = {
    'hud': '02_hud_edit', 'talk': '114_dialogue_choices', 'journal': '116_journal_available', 'bank': '12_bank', 'shop': '03_farmer_buy_grid',
    'pshop': '91_pshop_owner_stand', 'restaurant': '99d_restaurant_menu_one', 'jobs': '31_jobs', 'enhance': '81_enhance_ready', 'gacha': '45_gacha',
    'altar': '123b_orb_scene', 'attendance': '40_attendance', 'bounty': '117_bounty_board', 'land': '13_land_settings', 'plots': '84_plots_castle_map',
    'raid': '100b_raid_entry', 'abyss': '102c_abyss_weekly_detail', 'pvp': '98_pvp_hud_on', 'manual': '123_weapon_manual',
}
os.makedirs(OUT, exist_ok=True)
total = 0
for name, shot in SHOTS.items():
    try:
        raw = subprocess.run(['git', '-C', ROOT, 'show', REF + ':' + shot + '.png'], capture_output=True, check=True).stdout
    except subprocess.CalledProcessError:
        print('없음:', shot)
        continue
    im = Image.open(io.BytesIO(raw)).convert('RGB').resize((W, H), Image.LANCZOS)
    im = im.quantize(colors=96, method=Image.MEDIANCUT, dither=Image.NONE)
    path = os.path.join(OUT, name + '.png')
    im.save(path, optimize=True)
    total += os.path.getsize(path)
print(len(SHOTS), 'images,', total // 1024, 'KB')
