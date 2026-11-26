import json,sys
d=json.load(open('src/main/resources/rtshop_defaults/quests/story.json',encoding='utf-8'))
for q in d['quests']:
    if q['id'] in sys.argv[1:]:
        print('#####',q['id'],q['title'],q['giver'])
        print('I:',q['intro']);print('P:',q['progress']);print('D:',q['done'])
        for o in q['objectives']:
            print(' -',o['type'],o.get('target'),o.get('text',''))
            for l in o.get('lines',[]): print('     ',l)
        for k in ('choices','companion','sceneDone','sceneDream'):
            if k in q: print(k,json.dumps(q[k],ensure_ascii=False)[:1500])
