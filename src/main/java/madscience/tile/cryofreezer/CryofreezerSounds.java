package madscience.tile.cryofreezer;

import madscience.MadMachines;
import madscience.MadScience;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class CryofreezerSounds
{
    // Cryogenic Freezer
    static final String CRYOFREEZER_IDLE = MadScience.ID + ":" + MadMachines.CRYOFREEZER_INTERNALNAME + ".Idle";
    
    @SideOnly(Side.CLIENT)
    public static void init(SoundLoadEvent event)
    {
        event.manager.addSound(MadScience.ID + ":" + MadMachines.CRYOFREEZER_INTERNALNAME + "/Idle.ogg");
    }
}
