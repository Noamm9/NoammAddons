package com.github.noamm9.utils.location

import net.minecraft.core.BlockPos

enum class WorldType(val spawnPos: BlockPos? = null) {
    BackwaterBayou(BlockPos(- 13, 74, - 11)),
    Catacombs(BlockPos(0, 100, 0)),
    CrimsonIsle(BlockPos(- 361, 80, - 431)),
    CrystalHollows(BlockPos(213, 113, 417)),
    DeepCaverns(BlockPos(4, 157, 80)),
    DungeonHub(BlockPos(- 25, 121, 0)),
    DwarvenMines(BlockPos(- 49, 200, - 122)),
    End(BlockPos(- 503, 101, - 275)),
    Galatea(BlockPos(- 548, 109, - 23)),
    Garden(BlockPos(6, 71, 17)),
    GoldMine(BlockPos(- 5, 74, - 279)),
    Home(BlockPos(7, 100, 7)),
    Hub(BlockPos(0, 77, - 1)),
    JerryWorkshop(BlockPos(- 5, 76, 100)),
    Kuudra(BlockPos(- 101, 100, - 186)),
    Mineshaft(BlockPos(- 182, 100, - 192)),
    Park(BlockPos(266, 79, - 18)),
    Rift(BlockPos(- 45, 122, 69)),
    SpiderDen(BlockPos(- 203, 83, - 233)),
    LotusAtoll(BlockPos(- 16, 71, 1)),
    TorrhusCanyon(BlockPos(- 720, 94, 141)),
    TheBarn(BlockPos(113, 71, - 208));
}