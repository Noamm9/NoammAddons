package com.github.noamm9.features.impl.misc

import com.github.noamm9.commands.BaseCommand
import com.github.noamm9.commands.CommandNodeBuilder
import com.github.noamm9.features.Feature
import com.github.noamm9.utils.ChatUtils

object WarpShortcuts : Feature("removes the /warp in warp commands", "Warp Shortcuts") {
    object Arachne : BaseCommand("arachne") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp arachne")
            }
        }
    }

    object Barn : BaseCommand("barn") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp barn")
            }
        }
    }

    object Bayou : BaseCommand("bayou") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp bayou")
            }
        }
    }

    object Back : BaseCommand("back") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp bayou")
            }
        }
    }

    object Backwater : BaseCommand("backwater") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp bayou")
            }
        }
    }

    object Camp : BaseCommand("camp") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp camp")
            }
        }
    }

    object Castle : BaseCommand("castle") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp castle")
            }
        }
    }

    object Crypts : BaseCommand("crypts") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp crypts")
            }
        }
    }

    object Zombie : BaseCommand("zombie") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp crypts")
            }
        }
    }

    object DarkAuction : BaseCommand("da") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp da")
            }
        }
    }

    object Deep : BaseCommand("deep") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp deep")
            }
        }
    }

    object Desert : BaseCommand("desert") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp desert")
            }
        }
    }

    object Drag : BaseCommand("drag") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp drag")
            }
        }
    }

    object End : BaseCommand("end") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp end")
            }
        }
    }

    object Forge : BaseCommand("forge") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp forge")
            }
        }
    }

    object Gold : BaseCommand("gold") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp gold")
            }
        }
    }

    object Howl : BaseCommand("howl") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp howl")
            }
        }
    }

    object Wolf : BaseCommand("wolf") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp howl")
            }
        }
    }

    object Isle : BaseCommand("isle") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp isle")
            }
        }
    }

    object Jungle : BaseCommand("jungle") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp jungle")
            }
        }
    }

    object Kuudra : BaseCommand("kuudra") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp kuudra")
            }
        }
    }

    object Skull : BaseCommand("skull") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp kuudra")
            }
        }
    }

    object Mines : BaseCommand("mines") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp mines")
            }
        }
    }

    object Murk : BaseCommand("murk") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp murk")
            }
        }
    }

    object Murkwater : BaseCommand("murkwater") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp murk")
            }
        }
    }

    object Museum : BaseCommand("museum") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp museum")
            }
        }
    }

    object Nest : BaseCommand("nest") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp nest")
            }
        }
    }

    object Nucleus : BaseCommand("cn") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp cn")
            }

        }
    }

    object Nucleus2 : BaseCommand("nc") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp cn")
            }

        }
    }

    object Park : BaseCommand("park") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp park")
            }
        }
    }

    object Rift : BaseCommand("rift") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp rift")
            }
        }
    }

    object Spider : BaseCommand("spider") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp spider")
            }
        }
    }

    object Stonks : BaseCommand("stonks") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp stonks")
            }
        }
    }

    object Tomb : BaseCommand("tomb") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp tomb")
            }
        }
    }

    object Smold : BaseCommand("smold") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp tomb")
            }
        }
    }

    object Blaze : BaseCommand("blaze") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp tomb")
            }
        }
    }

    object Trapper : BaseCommand("trapper") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp trapper")
            }
        }
    }

    object Void : BaseCommand("void") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp void")
            }
        }
    }

    object Eman : BaseCommand("eman") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp void")
            }
        }
    }

    object Wizard : BaseCommand("wiz") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp wizard")
            }
        }
    }

    object DungeonHub : BaseCommand("dhub") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp dhub")
            }
        }
    }

    object DungeonHub2 : BaseCommand("dn") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp dhub")
            }
        }
    }

    object DungeonHub3 : BaseCommand("dh") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp dhub")
            }
        }
    }

    object DungeonHub4 : BaseCommand("d") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp dhub")
            }
        }
    }

    object DungeonHub5 : BaseCommand("dungeon") {
        override fun CommandNodeBuilder.build() {
            runs {
                ChatUtils.sendCommand("warp dhub")
            }
        }
    }
}