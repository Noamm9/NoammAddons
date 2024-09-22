package NoammAddons.features.gui

import NoammAddons.NoammAddons.Companion.config
import NoammAddons.NoammAddons.Companion.mc
import NoammAddons.events.GuiContainerEvent
import NoammAddons.mixins.AccessorGuiContainer
import NoammAddons.utils.ChatUtils.addColor
import NoammAddons.utils.ChatUtils.removeFormatting
import NoammAddons.utils.GuiUtils
import NoammAddons.utils.ItemUtils.getItemId
import NoammAddons.utils.ItemUtils.lore
import NoammAddons.utils.RenderUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

// inspired by Doc's Party Finder Overlay
// https://github.com/DocilElm/Doc/blob/main/Doc/features/misc/PartyFinderOverlay.js
object PartyFinderOverlay {
    private val partyMembersRegex = Regex(" \\w{1,16}: (\\w+) \\(\\d+\\)")
    private val levelRequiredRegex = Regex("Dungeon Level Required: (\\d+)")
    private val classNames = listOf("&4&lArcher", "&a&lTank", "&6&lBerserk", "&5&lHealer", "&b&lMage")
    private val blackListItemsIds = listOf(160, 7, 262)

    @SubscribeEvent
    fun guiRender(event: GuiContainerEvent.DrawSlotEvent) {
        if (!config.PartyFinderOverlay) return
        if (GuiUtils.currentChestName != "Party Finder") return
	    
	    val item = event.slot.stack
	    val (x, y) = event.slot.xDisplayPosition.toDouble() to event.slot.yDisplayPosition.toDouble()
	    
	    if (item == null) return
	    if (blackListItemsIds.contains(item.getItemId()) || event.slot.slotNumber >= 36) return

	    val classes = mutableListOf<String>()
	    var levelRequired = 0
	    
	    item.lore.forEach { line ->
			val stripped = line.removeFormatting()
		    when {
				levelRequiredRegex.matches(stripped) -> levelRequired = levelRequiredRegex.find(stripped)?.groupValues?.get(1)?.toInt() ?: 0
			    partyMembersRegex.matches(stripped) -> {
					classes.add(partyMembersRegex.matchEntire(stripped)?.groupValues?.get(1) ?: "")
				}
			}
		}


	    val missingClasses = classNames
		    .filter { name -> classes.indexOf(name.addColor().removeFormatting()) == -1 }
		    .map { it.take(5) }
	    
	    val missingOffsetY = if (missingClasses.size >= 3) mc.fontRendererObj.FONT_HEIGHT * 2 else mc.fontRendererObj.FONT_HEIGHT
	    val missingStr = missingClasses.take(2).joinToString("")
	    val p2 = missingClasses.drop(2).take(2).joinToString("")
	    val missing = "$missingStr\n$p2"

	    GlStateManager.pushMatrix()
		GlStateManager.translate(x, y, 300.0)
	    
	    RenderUtils.drawText(
		    missing,
		    .0,
		    16 - (missingOffsetY * 0.7),
		    0.7
		)

	    val levelX = 16 - mc.fontRendererObj.getStringWidth("$levelRequired") * 0.7

	    RenderUtils.drawText(
		    "&c$levelRequired",
		    levelX,
		    .0,
		    0.7
		)
	    GlStateManager.popMatrix()
    }
}