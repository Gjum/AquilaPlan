package gjum.minecraft.liteloader.aquilaplan;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mumfrey.liteloader.PostRenderListener;
import com.mumfrey.liteloader.Tickable;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.modconfig.ConfigStrategy;
import com.mumfrey.liteloader.modconfig.ExposableOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;

import java.io.File;

import static org.lwjgl.opengl.GL11.*;

@ExposableOptions(strategy = ConfigStrategy.Versioned, filename = "aquilaPlan.json")
public class LiteModAquilaPlan implements PostRenderListener, Tickable {
    private static KeyBinding toggleShownKey = new KeyBinding("key.aquilaPlan.toggleShown", Keyboard.KEY_O, "key.categories.litemods");
    private static KeyBinding toggleFixedHeightKey = new KeyBinding("key.aquilaPlan.toggleFixedHeight", Keyboard.KEY_H, "key.categories.litemods");

    @Expose
    @SerializedName("centerX")
    int centerX = 3900;

    @Expose
    @SerializedName("centerZ")
    int centerZ = -850;

    @Expose
    @SerializedName("visible")
    private boolean visible = true;

    @Expose
    @SerializedName("fixedHeight")
    private int fixedHeight = -1;

    /**
     * Default constructor. All LiteMods must have a default constructor. In general you should do very little
     * in the mod constructor EXCEPT for initialising any non-game-interfacing components or performing
     * sanity checking prior to initialisation
     */
    public LiteModAquilaPlan() {
    }

    @Override
    public String getName() {
        return "Aquila Plan Helper";
    }

    /**
     * getVersion() should return the same version string present in the mod metadata, although this is
     * not a strict requirement.
     *
     * @see com.mumfrey.liteloader.LiteMod#getVersion()
     */
    @Override
    public String getVersion() {
        return "0.1.1";
    }

    /**
     * init() is called very early in the initialisation cycle, before the game is fully initialised, this
     * means that it is important that your mod does not interact with the game in any way at this point.
     *
     * @see com.mumfrey.liteloader.LiteMod#init(java.io.File)
     */
    @Override
    public void init(File configPath) {
        LiteLoader.getInput().registerKeyBinding(LiteModAquilaPlan.toggleShownKey);
        LiteLoader.getInput().registerKeyBinding(LiteModAquilaPlan.toggleFixedHeightKey);
    }

    /**
     * upgradeSettings is used to notify a mod that its version-specific settings are being migrated
     *
     * @see com.mumfrey.liteloader.LiteMod#upgradeSettings(java.lang.String, java.io.File, java.io.File)
     */
    @Override
    public void upgradeSettings(String version, File configPath, File oldConfigPath) {
    }

    @Override
    public void onTick(Minecraft minecraft, float partialTicks, boolean inGame, boolean clock) {
        if (inGame && minecraft.currentScreen == null && Minecraft.isGuiEnabled()) {
            if (LiteModAquilaPlan.toggleShownKey.isPressed()) {
                visible = !visible;
                LiteLoader.getInstance().writeConfig(this);
                minecraft.thePlayer.addChatComponentMessage(
                        new TextComponentString("Aquila plan " + (visible ? "shown" : "hidden"))
                                .setStyle(new Style().setItalic(true).setColor(TextFormatting.DARK_GRAY)));
            }
            if (LiteModAquilaPlan.toggleFixedHeightKey.isPressed()) {
                if (fixedHeight == -1)
                    fixedHeight = (int) minecraft.thePlayer.posY;
                else fixedHeight = -1;
                LiteLoader.getInstance().writeConfig(this);
                minecraft.thePlayer.addChatComponentMessage(
                        new TextComponentString("Aquila plan height: " + (fixedHeight == -1 ? "follow player" : "at " + fixedHeight))
                                .setStyle(new Style().setItalic(true).setColor(TextFormatting.DARK_GRAY)));
            }
        }
    }

    @Override
    public void onPostRenderEntities(float partialTicks) {
    }

    @Override
    public void onPostRender(float partialTicks) {
        if (!visible) return;
        EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
        if (p == null) return;

        glPushMatrix();
        double px = p.lastTickPosX + (p.posX - p.lastTickPosX) * partialTicks;
        double py = p.lastTickPosY + (p.posY - p.lastTickPosY) * partialTicks;
        double pz = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * partialTicks;
        glTranslated(-px, -py, -pz);
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        int oy = fixedHeight == -1 ? (int) py : fixedHeight;

        // center on the center of the center block
        glTranslated(centerX + .5, oy, centerZ + .5);

        for (int i = 0; i < districtCorners.length; i++) {
            int[][] district = districtCorners[i];

            int dcx = 0, dcz = 0;
            for (int[] corner : district) {
                dcx += corner[1];
                dcz += corner[0];
            }
            dcx /= district.length;
            dcz /= district.length;

            float[] color = districtColors[i];
            glColor4f(color[0], color[1], color[2], .5f);

            glBegin(GL_LINE_LOOP);
            for (int[] corner : district) {
                // point format: lat,long == z,x
                float z = corner[0];
                float x = corner[1];
                // offset lines into the district a bit,
                // so they don't overlap with other districts
                x = x > dcx ? x - .2f : x + .2f;
                z = z > dcz ? z - .2f : z + .2f;
                glVertex3f(x, 0, z);
            }
            glEnd();
        }

        // mark center with cross
        glBegin(GL_LINES);
        glVertex3d(-.5, .002, -.5);
        glVertex3d( .5, .002,  .5);
        glVertex3d( .5, .002, -.5);
        glVertex3d(-.5, .002,  .5);
        glEnd();

        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);
        glPopMatrix();
    }

    private static final float[][] districtColors = {
            {  1,  0,  1}, // capital
            {  1,  0,  0}, // district 6
            {  1,  1,  0}, // district 5
            {  1,  1,  0}, // district 4
            {  0,  0,  1}, // newfriend-ville
            {.5f,.5f,.5f}, // district 2
            {.5f,.5f,.5f}, // district 1
            {.5f,.5f,.5f}, // district 3
    };

    private static final int[][][] districtCorners = {
            // capital
            {{-51, 21}, {-93, 21}, {-21, 93}, {-21, 51}, {21, 51}, {21, 93}, {63, 51}, {123, 51}, {93, 21}, {51, 21}, {51, -21}, {93, -21}, {123, -51}, {63, -51}, {21, -93}, {21, -51}, {-21, -51}, {-21, -93}, {-93, -21}, {-51, -21}},
            // district 6
            {{-51, 21}, {-93, 21}, {-21, 93}, {-51, 123}, {-93, 123}, {-93, 165}, {-165, 93}, {-123, 93}, {-123, 51}, {-165, 51}, {-195, 21}, {-195, -21}, {-165, -51}, {-123, -51}, {-123, -93}, {-165, -93}, {-93, -165}, {-93, -123}, {-51, -123}, {-21, -93}, {-93, -21}, {-51, -21}},
            // district 5
            {{-21, 93}, {-21, 51}, {21, 51}, {21, 93}, {63, 51}, {123, 51}, {123, 93}, {165, 93}, {93, 165}, {93, 123}, {51, 123}, {51, 165}, {21, 195}, {-21, 195}, {-51, 165}, {-51, 123}},
            // district 4
            {{-21, -93}, {-21, -51}, {21, -51}, {21, -93}, {63, -51}, {123, -51}, {123, -93}, {165, -93}, {93, -165}, {93, -123}, {51, -123}, {51, -165}, {21, -195}, {-21, -195}, {-51, -165}, {-51, -123}},
            // newfriend-ville
            {{51, 21}, {93, 21}, {123, 51}, {123, 93}, {165, 93}, {207, 51}, {267, 51}, {237, 21}, {237, -21}, {267, -51}, {207, -51}, {165, -93}, {123, -93}, {123, -51}, {93, -21}, {51, -21}},
            // district 2
            {{93, 165}, {93, 123}, {51, 123}, {51, 165}, {21, 195}, {21, 237}, {51, 267}, {93, 267}, {267, 93}, {267, 51}, {207, 51}},
            // district 1
            {{93, -165}, {93, -123}, {51, -123}, {51, -165}, {21, -195}, {21, -237}, {51, -267}, {93, -267}, {267, -93}, {267, -51}, {207, -51}},
            // district 3
            {{-267, 93}, {-93, 267}, {-51, 267}, {-21, 237}, {-21, 195}, {-51, 165}, {-51, 123}, {-93, 123}, {-93, 165}, {-165, 93}, {-123, 93}, {-123, 51}, {-165, 51}, {-195, 21}, {-195, -21}, {-165, -51}, {-123, -51}, {-123, -93}, {-165, -93}, {-93, -165}, {-93, -123}, {-51, -123}, {-51, -165}, {-21, -195}, {-21, -237}, {-51, -267}, {-93, -267}, {-267, -93}},
    };
}
