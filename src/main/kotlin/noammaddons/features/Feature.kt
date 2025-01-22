package noammaddons.features

import noammaddons.noammaddons

abstract class Feature {
    protected val mc = noammaddons.mc
    protected val config = noammaddons.config
    protected val scope = noammaddons.scope
    protected val hudData = noammaddons.hudData
    protected val ahData = noammaddons.ahData
    protected val bzData = noammaddons.bzData
    protected val npcData = noammaddons.npcData
    protected val mayorData get() = noammaddons.mayorData
    protected val itemIdToNameLookup = noammaddons.itemIdToNameLookup
}
