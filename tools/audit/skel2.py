import json,sys,re
d=json.load(open('src/main/resources/rtshop_defaults/quests/story.json',encoding='utf-8'))
pre=sys.argv[1:]
prev=None;run=0
for q in d['quests']:
    if pre and not any(q['id'].startswith(p) for p in pre): continue
    objs=[];sig=[]
    for o in q['objectives']:
        if o.get('mod','').startswith('!'): continue
        t=o['type'][:3]; tg=o.get('target','').split(':')[-1].split('@')
        s=t+':'+tg[0][:20]
        if o.get('count',1)>1: s+='x%d'%o['count']
        if 'lines' in o: s+='[%d]'%len(o['lines'])
        if o.get('verb'): s+='<'+o['verb'][:6]+'>'
        objs.append(s); sig.append(t if not o.get('verb') else 'ACT')
    sig=tuple(sorted(set(sig)))
    run=run+1 if sig==prev else 1; prev=sig
    c=q.get('companion',{}).get('npc','').replace('order_','')
    r=q.get('reward',{})
    ex=''
    if q.get('choices'): ex+=' CH'
    if r.get('flags'): ex+=' F:'+','.join(r['flags'])
    if q.get('sceneDream'): ex+=' DREAM'
    if q.get('requiresFlags'): ex+=' RF:'+','.join(q['requiresFlags'])
    print(('!!' if run>=3 else '  ')+q['id'].replace('story_',''),'|',q['title'],'|',q['giver'].replace('quest_','').replace('order_','')[:7],('+'+c if c else ''),'|',' '.join(objs),'|',r.get('gold',0),ex)
