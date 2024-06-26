/// <reference types="../../CTAutocomplete" />
/// <reference lib="es2015" />

import Settings from "../Settings"
import RenderLib from "../../RenderLib"
import { IsInBossRoom, IsInDungeon } from "../utils"

const InvisDungeonMobs = [
    "Revoker",
    "Psycho",
    "Reaper",
    "Cannibal",
    "Mute",
    "Ooze",
    "Putrid",
    "Freak",
    "Leech",
    "Tear",
    "Parasite",
    "Flamer",
    "Skull",
    "Mr. Dead",
    "Vader",
    "Frost",
    "Walker",
    "Wandering Soul",
    "Bonzo",
    "Scarf",
    "Livid",
    "Spirit Bear",
    "Shadow Assassin",
    "Fel",
    "Dinnerbone"
]


register("renderEntity", (entity) => {
    if (!Settings.DungeonMobESP) return
    if (!IsInDungeon()) return
    if (IsInBossRoom()) return

    let name = ChatLib.removeFormatting(entity.getName().removeFormatting())
    const espBox = (x, y, z, height) => {RenderLib.drawEspBox(x, y-height, z, 0.9, height, Settings.MobESPColor.getRed()/255 ,Settings.MobESPColor.getGreen()/255, Settings.MobESPColor.getBlue()/255, 1, true);}
    const espfilledBox = (x, y, z, height) => {RenderLib.drawInnerEspBox(x, y-height, z, 0.9, height, Settings.MobESPColor.getRed()/255 ,Settings.MobESPColor.getGreen()/255, Settings.MobESPColor.getBlue()/255, Settings.MobESPColor.getAlpha() /255, 1, true);}
    
    Tessellator.disableLighting()

    if (entity.getClassName() != `EntityArmorStand`) {
        InvisDungeonMobs.forEach(mobName => {
            if (name.includes(mobName)) entity.getEntity().func_82142_c(false);
        })
    
        if (name.includes("Shadow Assassin")) {
            if (Settings.MobESPMode == 1) espfilledBox(entity.getRenderX(), entity.getRenderY()+2, entity.getRenderZ(), 1.9)
            else if (Settings.MobESPMode == 0) espBox(entity.getRenderX(), entity.getRenderY()+2, entity.getRenderZ(), 1.9)
            else {
                espBox(entity.getRenderX(), entity.getRenderY()+2, entity.getRenderZ(), 1.9)
                espfilledBox(entity.getRenderX(), entity.getRenderY()+2, entity.getRenderZ(), 1.9)
            }
        }
    }



    if (name.includes("✯")) {
        //if (CTEntity.getClassName() == `EntityArmorStand`) return
        
        if (name.includes("Fel") || name.includes("Withermancer")) {
            if (Settings.MobESPMode == 0) espBox(entity.getRenderX(), entity.getRenderY(), entity.getRenderZ(), 2.8);
            else if (Settings.MobESPMode == 1) espfilledBox(entity.getRenderX(), entity.getRenderY(), entity.getRenderZ(), 2.8)
            else {
            espBox(entity.getRenderX(), entity.getRenderY(), entity.getRenderZ(), 2.8)
            espfilledBox(entity.getRenderX(), entity.getRenderY(), entity.getRenderZ(), 2.8)
            }
        }
        else {
            // entity.getEntity().func_82142_c(false);
            if (Settings.MobESPMode == 1) espfilledBox(entity.getRenderX(), entity.getRenderY(), entity.getRenderZ(), 1.9)
            else if (Settings.MobESPMode == 0) espBox(entity.getRenderX(), entity.getRenderY(), entity.getRenderZ(), 1.9)
            else {
                espBox(entity.getRenderX(), entity.getRenderY(), entity.getRenderZ(), 1.9)
                espfilledBox(entity.getRenderX(), entity.getRenderY(), entity.getRenderZ(), 1.9)
            }
        }
    }

    Tessellator.enableLighting()
})


