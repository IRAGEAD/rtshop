START = 961
EN = [
    # 961 도구 (도감의 장비 탭)
    "A surveyor has a few tools. All are used by holding them and right-clicking.\n\n"
    "· Surveyor's Codex: records the mobs you met, the mobs you slew and the gear you obtained. Its gear tab lists only gear you can get from shops, gacha or loot.\n"
    "· Surveyor's Guidebook: the book you are reading now.\n"
    "· Field Notebook: holds the traces you looked at and what you read there.\n"
    "· Survey Compass: shows the direction and distance of the place your current quest is looking for.\n"
    "· Return Ticket: takes you back to town. (See the 'Return tickets' page.)\n"
    "· Weapon Manual: you receive one the first time you obtain a weapon with a special skill.\n\n"
    "You receive one Codex, one Guidebook and one Field Notebook at the start. If you lose them, the general store sells them again.",
    # 962 대화
    "Right-click a townsperson to open the dialogue box.\n\n"
    "· The top choice is that person's own work (shop, bank, jobs, attendance, gacha…).\n"
    "· A story marked '✦' is one you have not heard yet.\n"
    "· [Quests] shows the quests you can take and the ones in progress.\n"
    "· [Talk history] lets you reread what has been said so far.\n"
    "· Questions about another person appear after you have talked with that person once, and questions about something appear after you have heard of it somewhere.\n\n"
    "Chat with the same person long enough and now and then they let slip something you cannot hear anywhere else.\n\n"
    "People you have talked to and places you have visited stay on the map (Diorama). People show as a picture of what they do (anvil, fish, book…), and their name appears when you hover over it. "
    "The pin colour tells shops (orange), town services (green), story people (blue) and combat masters (red) apart. "
    "A mark you removed from the map comes back when you talk to that person again. To bring back all marks at once, use /rtshop mapmark resend.",
    # 963 퀘스트
    "Quests are taken in conversation with NPCs. Fill the goals, go back, and you receive the reward.\n\n"
    "· Quest journal: /rtshop quest (you can bind a key in the controls)\n"
    "· You can have 10 in progress at once, and an abandoned quest can be taken again.\n"
    "· The quest display on screen shows each goal's progress and whom to visit.\n"
    "· Only one combat job quest line and one life job quest line can be in progress at a time.\n"
    "· The 'Next quest' on screen points you to job quests only after Expedition Captain Roran's 'A Surveyor's Trade', and only while you have no job and no job quest in progress. You can still take them from the Job Guide at any time.",
    # 964 화면 표시
    "Type /rtshop hud to drag the Gold, quest and PvP displays with the mouse.\n\n"
    "· Click a display to select it, then change its size, font and text size.\n"
    "· Tab selects the next display and the arrow keys nudge it.\n"
    "· Each display can be switched on or off.\n"
    "· 'Language' switches the text of the displays, and 'Story language' the text of dialogue, quests and this guidebook, between Korean and English (by default they follow the game language).",
    # 965
    "HUD position font display language English Korean",
    # 966 명령어
    "· /rtshop quest : quest journal\n"
    "· /rtshop hud : move the on-screen displays\n"
    "· /rtshop balance : see wallet and savings\n"
    "· /rtshop check <amount> : write a Gold check\n"
    "· /rtshop land : my land settings\n"
    "· /rtshop job : see my jobs\n"
    "· /party : party (create, invite, leave)\n"
    "· /pc <text> : party chat\n"
    "· /pvp : turn PvP on or off\n"
    "· /bounty : see the bounties I took\n"
    "· /rtshop notebook : the field notebook (if you lost yours)\n"
    "· /rtshop stocks : guild share prices\n"
    "· /rtshop mapmark resend : receive all map marks again\n"
    "· /fpscheck : type it where the game gets slow; it measures for 30 seconds and leaves a record file in the game folder (send that file when looking for the cause)\n"
    "· Reports (bugs, suggestions, player reports, land or trade trouble): talk to Haru the Town Clerk and choose 'File a report'. Your reports and their answers are in that window, or /rtshop report list\n"
    "· A boss entrance can be skipped with Enter (change the key under 'Skip scene' in Controls)",
    # 967
    "commands command report complaint suggestion skip scene FPS lag slow",
    # 968 요리
    "The food merchant sells ingredients and buys finished food. Crops, raw meat, eggs and milk are sold by the Crop Merchant.\n\n"
    "· Food made by hands-on cooking (pan, steamer, kettle) is bought for more.\n"
    "· Even if you buy every ingredient at the shops and sell what you make, you do not lose money.\n"
    "· Hover over a food in the shop to see the ingredients its price is based on.\n"
    "· The cook job sells hand-made dishes for more.\n"
    "· A dish cooked in a pot with just one of each ingredient instead of the full recipe (a dish that shows a quality) is bought for what went into it. A 'Superb' dish gets the full price.",
    # 969 강화 (W-5)
    "There is only one enhance anvil in town. Right-click it and place your gear in the left slot; the panel beside it shows the odds, materials and Gold. Take the gear out of the right slot to enhance it.\n\n"
    "· The result is one of success (+1), no change, damage (-1) or destruction. It gets harder as the level rises.\n"
    "· Every failure (no change or damage) raises that item's next success chance a little (by 10% of that level's chance, up to 5 times). A success resets it.\n"
    "· Legendary and higher gear and hidden gear are never destroyed. The destruction share goes half to no change and half to damage.\n"
    "· How to use a guard: put it in the slot right next to the gear (the middle slot) and enhance. One is used on that attempt and is gone whatever the result. Only one kind can go in at a time, and right-clicking it in your hand does nothing.\n"
    "· Damage Guard: a damage result leaves the level as it is.\n"
    "· Destruction Guard: a destruction result keeps the item and turns into damage (-1).\n"
    "· Enhancement Protection: stops both damage and destruction, so the level stays as it is.\n"
    "· Guards are sold in the Blacksmith's Enhance tab. The bottom right of the panel shows the guard that is on.\n"
    "· Enhanced gear shows +N before its name and gains attack or defense. Bows, crossbows and guns raise the damage of their arrows and bullets, and staffs and wands raise spell damage, by the same share.\n"
    "· From +8, armour also cuts the damage you take (adding up over the pieces you wear, up to 40%), because armour points stop rising at 30.",
    # 970
    "anvil guard ticket damage destruction enhance failure legendary",
    # 971 요리 모드
    "Make food with the cooking mods' tools, such as the cutting board, pan, pot and steamer.\n\n"
    "· See recipes by clicking a food in the recipe viewer (JEI).\n"
    "· Food made by hands-on cooking is bought for more by the food merchant.\n"
    "· Cooking ingredients no shop used to sell are sold automatically by the food merchant (except what the Crop Merchant sells).",
]
