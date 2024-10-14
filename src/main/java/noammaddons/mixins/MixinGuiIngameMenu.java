package noammaddons.mixins;

    /*
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.achievement.GuiAchievements;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraft.client.resources.I18n;
import net.minecraft.realms.RealmsBridge;
import noammaddons.config.Config;
import noammaddons.noammaddons;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import noammaddons.utils.GuiUtils;


@Mixin(GuiIngameMenu.class)
public class MixinGuiIngameMenu extends GuiScreen {

    @Shadow
    private int field_146445_a;


     * @author Noamm9
     * @reason Cool

    @Overwrite
    public void initGui() {
        this.field_146445_a = 0;
        this.buttonList.clear();
        int i = -16;
        this.buttonList.add(new GuiButton(1, this.width / 2 - 100, this.height / 4 + 144 + i, I18n.format("menu.returnToMenu")));

        if (!this.mc.isIntegratedServerRunning()) {
            this.buttonList.get(0).displayString = I18n.format("menu.disconnect");
        }

        this.buttonList.add(new GuiButton(4, this.width / 2 - 100, this.height / 4 + 74 + i, I18n.format("menu.returnToGame")));
        this.buttonList.add(new GuiButton(30, this.width / 2 - 100, this.height / 4 + 96 + i, 98, 20, "Servers"));
        this.buttonList.add(new GuiButton(31, this.width / 2 + 2, this.height / 4 + 96 + i, 98, 20, noammaddons.FULL_PREFIX));
        this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height / 4 + 120 + i, 98, 20, I18n.format("menu.options")));
        this.buttonList.add(new GuiButton(12, this.width / 2 + 2, this.height / 4 + 120 + i, 98, 20, I18n.format("fml.menu.modoptions")));
    }

    @Overwrite
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0:
                this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
                break;
            case 1:
                boolean flag = this.mc.isIntegratedServerRunning();
                boolean flag1 = this.mc.isConnectedToRealms();
                button.enabled = false;
                this.mc.theWorld.sendQuittingDisconnectingPacket();
                this.mc.loadWorld(null);

                if (flag) {
                    this.mc.displayGuiScreen(new GuiMainMenu());
                } else if (flag1) {
                    RealmsBridge realmsbridge = new RealmsBridge();
                    realmsbridge.switchToRealms(new GuiMainMenu());
                } else {
                    this.mc.displayGuiScreen(new GuiMultiplayer(new GuiMainMenu()));
                }

            case 2:
            case 3:
            default:
                break;
            case 4:
                this.mc.displayGuiScreen(null);
                this.mc.setIngameFocus();
                break;
            case 5:
                if (this.mc.thePlayer != null)
                    this.mc.displayGuiScreen(new GuiAchievements(this, this.mc.thePlayer.getStatFileWriter()));
                break;
            case 6:
                if (this.mc.thePlayer != null)
                    this.mc.displayGuiScreen(new GuiStats(this, this.mc.thePlayer.getStatFileWriter()));
                break;
            case 7:
                this.mc.displayGuiScreen(new GuiShareToLan(this));
                break;
            case 12:
                net.minecraftforge.fml.client.FMLClientHandler.instance().showInGameModOptions((GuiIngameMenu) (Object) this);
                break;
            case 30:
                this.mc.displayGuiScreen(new GuiMultiplayer(this));
                break;
            case 31:
                GuiUtils.INSTANCE.openScreen(Config.INSTANCE.gui());
                break;
        }
    }
}
*/
