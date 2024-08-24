package NoammAddons.forge

import net.minecraftforge.common.ForgeVersion
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.MCVersion
import org.spongepowered.asm.launch.MixinBootstrap
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.Mixins

@MCVersion(ForgeVersion.mcVersion)
class FMLLoadingPlugin : IFMLLoadingPlugin {

    override fun getASMTransformerClass(): Array<String> = emptyArray()

    override fun getModContainerClass(): String? = null

    override fun getSetupClass(): String? = null

    override fun injectData(data: Map<String, Any>) {}

    override fun getAccessTransformerClass(): String? = null

    init {
        MixinBootstrap.init()
        Mixins.addConfiguration("mixins.NoammAddons.json")
        MixinEnvironment.getCurrentEnvironment().obfuscationContext = "searge"
    }
}
