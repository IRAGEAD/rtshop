#!/usr/bin/env python3
"""보스·레이드 음악 넣기: python3 tools/add_music.py <곡 이름> <음악 파일(mp3·wav·ogg·flac…)> [음량 0.1~1.0]

- 파일을 ogg(Vorbis)로 바꿔 src/main/resources/assets/rtshop/sounds/music/<곡 이름>.ogg 에 두고,
  sounds.json 에 소리 rtshop:music.<곡 이름> 을 더한다 (길게 이어 트는 곡이라 stream).
- 곡 이름은 config/rtshop/boss_music.json 의 tracks 나 raid 에 적는 이름이다 (기본: 모든 보스 "boss", 레이드 "raid").
- 지우기: python3 tools/add_music.py --remove <곡 이름>
"""
import json, os, re, subprocess, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src/main/resources/assets/rtshop')
SOUNDS = os.path.join(ROOT, 'sounds.json')

def load():
    with open(SOUNDS, encoding='utf-8') as f:
        return json.load(f)

def save(d):
    with open(SOUNDS, 'w', encoding='utf-8') as f:
        json.dump(d, f, ensure_ascii=False, indent=2)
        f.write('\n')

def main(argv):
    if len(argv) >= 2 and argv[0] == '--remove':
        name = argv[1]
        d = load()
        d.pop('music.' + name, None)
        save(d)
        p = os.path.join(ROOT, 'sounds', 'music', name + '.ogg')
        if os.path.exists(p):
            os.remove(p)
        print('removed', name)
        return 0
    if len(argv) < 2:
        print(__doc__)
        return 1
    name, src = argv[0], argv[1]
    volume = float(argv[2]) if len(argv) > 2 else 1.0
    if not re.fullmatch(r'[a-z0-9_]+', name):
        print('곡 이름은 영어 소문자·숫자·밑줄만 쓸 수 있습니다:', name)
        return 1
    out = os.path.join(ROOT, 'sounds', 'music', name + '.ogg')
    os.makedirs(os.path.dirname(out), exist_ok=True)
    # 스테레오 ogg, 44.1kHz, 품질 5 (약 160kbps). 처음·끝의 군더더기 소리 정보는 지운다.
    r = subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', src, '-vn', '-map_metadata', '-1', '-ac', '2', '-ar', '44100',
                        '-c:a', 'libvorbis', '-q:a', '5', out])
    if r.returncode != 0 or not os.path.exists(out):
        print('변환 실패:', src)
        return 1
    d = load()
    d['music.' + name] = {'sounds': [{'name': 'rtshop:music/' + name, 'stream': True, 'volume': max(0.1, min(1.0, volume))}]}
    save(d)
    print('added rtshop:music.%s  (%s, %.1f MB)' % (name, os.path.relpath(out), os.path.getsize(out) / 1e6))
    return 0

if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
