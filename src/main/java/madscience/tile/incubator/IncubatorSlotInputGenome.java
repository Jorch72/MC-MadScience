package madscience.tile.incubator;

import madscience.items.combinedgenomes.CombinedGenomeMonsterPlacer;
import madscience.items.genomes.ItemGenomeBase;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

class IncubatorSlotInputGenome extends Slot
{
    IncubatorSlotInputGenome(IInventory inv, int index, int x, int y)
    {
        super(inv, index, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack stack)
    {
        // Check if we are a genome data reel.
        if (stack != null && stack.getItem() instanceof ItemGenomeBase)
        {
            return true;
        }

        // Check if we are a combined genome (monster from mainframe).
        if (stack != null && stack.getItem() instanceof CombinedGenomeMonsterPlacer)
        {
            return true;
        }

        return false;
    }
}