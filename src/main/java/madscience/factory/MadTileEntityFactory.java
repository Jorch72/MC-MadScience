package madscience.factory;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import madscience.MadForgeMod;
import madscience.factory.block.MadGhostBlockData;
import madscience.factory.mod.MadMod;
import madscience.factory.model.MadModel;
import madscience.factory.tileentity.MadTileEntityFactoryProduct;
import madscience.factory.tileentity.MadTileEntityFactoryProductData;
import madscience.items.ItemBlockTooltip;
import madscience.util.MadUtils;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.registry.GameRegistry;

public class MadTileEntityFactory
{
    /** Prevents multiple instances of this class from being created. */
    private static MadTileEntityFactory instance;
    
    /** Mapping of machine names to created products. */
    private static final Map<String, MadTileEntityFactoryProduct> registeredMachines = new LinkedHashMap<String, MadTileEntityFactoryProduct>();
    
    public MadTileEntityFactory()
    {
        super();
    }
    
    /** Only a single instance of the tile entity factory may ever exist. */
    public static synchronized MadTileEntityFactory instance()
    {
        if (instance == null)
        {
            instance = new MadTileEntityFactory();
        }
        
        return instance;
    }

    /** Returns information for a particular machine based on name. */
    public MadTileEntityFactoryProduct getMachineInfo(String id)
    {
        return registeredMachines.get(id);
    }

    /** Returns a unmodifiable list of all registered machine by this factory. */
    public Collection<MadTileEntityFactoryProduct> getMachineInfoList()
    {
        return Collections.unmodifiableCollection(registeredMachines.values());
    }

    /** Returns all registered machines, but as data objects which may be serialized by JSON loader to disk. */
    public MadTileEntityFactoryProductData[] getMachineDataList()
    {
        // Loop through every registered machine in the system.
        Set<MadTileEntityFactoryProductData> allMachines = new HashSet<MadTileEntityFactoryProductData>();
        for (Iterator iterator = getMachineInfoList().iterator(); iterator.hasNext();)
        {
            MadTileEntityFactoryProduct registeredMachine = (MadTileEntityFactoryProduct) iterator.next();
            if (registeredMachine != null)
            {
                // Add the machines configuration data to our list for saving.
                allMachines.add(registeredMachine.getData());
            }
        }

        return allMachines.toArray(new MadTileEntityFactoryProductData[]{});
    }

    /** Determines is this machine name has already been registered by this factory. Hopefully this never returns true. */
    private boolean isValidMachineID(String id)
    {
        return !registeredMachines.containsKey(id);
    }

    /** Return itemstack from GameRegistry or from vanilla Item/Block list. */
    public ItemStack[] getItemStackFromString(String modID, String itemName, int stackSize, String metaDataText)
    {
        // Reference list we will return at the end of work.
        ArrayList<ItemStack> itemsToAssociate = new ArrayList<ItemStack>();
        Collection<String> unlocalizedNames = new TreeSet<String>(Collator.getInstance());

        // Reference to if this recipe deals with wildcard (*) values in meta/damage or name.
        boolean wildcardName = itemName.contains("*");
        boolean wildcardMeta = metaDataText.contains("*");

        // Reference to actual metadata since we have to parse it.
        int metaData = 0;
        if (!wildcardMeta)
        {
            // If not using wildcard for damage then parse it as integer.
            metaData = Integer.parseInt(metaDataText);
        }

        // Only lookup individual itemstacks if we are not hunting wildcards.
        if (!wildcardName)
        {
            // Mod items and blocks query.
            ItemStack potentialModItem = GameRegistry.findItemStack(modID, itemName, stackSize);
            if (potentialModItem != null)
            {
                if (!unlocalizedNames.contains(MadUtils.cleanTag(potentialModItem.getUnlocalizedName())))
                {
                    itemsToAssociate.add(potentialModItem);
                    unlocalizedNames.add(MadUtils.cleanTag(potentialModItem.getUnlocalizedName()));
                }
            }
        }

        // Vanilla item query.
        for (Item potentialMCItem : Item.itemsList)
        {
            if (potentialMCItem == null)
            {
                continue;
            }

            // Check if we need to accommodate metadata.
            int tmpMeta = 0;
            if (wildcardMeta)
            {
                tmpMeta = 0;
            }
            else
            {
                // Use given value if not wild.
                tmpMeta = metaData;
            }

            ItemStack vanillaItemStack = new ItemStack(potentialMCItem, stackSize, tmpMeta);

            if (vanillaItemStack != null)
            {
                try
                {
                    // Check if name contains wildcard value.
                    if (wildcardName && potentialMCItem.getUnlocalizedName().contains(itemName.replace("*", "")))
                    {
                        if (!unlocalizedNames.contains(vanillaItemStack.getUnlocalizedName()))
                        {
                            itemsToAssociate.add(vanillaItemStack);
                            unlocalizedNames.add(vanillaItemStack.getUnlocalizedName());
                        }
                    }
                    else if (!wildcardName && MadUtils.cleanTag(vanillaItemStack.getUnlocalizedName()).equals(itemName))
                    {
                        if (!unlocalizedNames.contains(MadUtils.cleanTag(vanillaItemStack.getUnlocalizedName())))
                        {
                            itemsToAssociate.add(vanillaItemStack);
                            unlocalizedNames.add(MadUtils.cleanTag(vanillaItemStack.getUnlocalizedName()));
                        }
                    }
                }
                catch (Exception err)
                {
                    continue;
                }
            }
        }

        // Vanilla block query.
        for (Block potentialMCBlock : Block.blocksList)
        {
            if (potentialMCBlock == null)
            {
                continue;
            }

            // Check if we need to accommodate metadata.
            int tmpMeta = 0;
            if (wildcardMeta)
            {
                tmpMeta = 0;
            }
            else
            {
                // Use given value if not wild.
                tmpMeta = metaData;
            }

            ItemStack vanillaItemStack = new ItemStack(potentialMCBlock, tmpMeta, stackSize);

            if (vanillaItemStack != null)
            {
                try
                {
                    // Check if name contains wildcard value.
                    if (wildcardName && vanillaItemStack.getUnlocalizedName().contains(itemName.replace("*", "")))
                    {
                        if (!unlocalizedNames.contains(vanillaItemStack.getUnlocalizedName()))
                        {
                            itemsToAssociate.add(vanillaItemStack);
                            unlocalizedNames.add(vanillaItemStack.getUnlocalizedName());
                        }
                    }
                    else if (!wildcardName && MadUtils.cleanTag(vanillaItemStack.getUnlocalizedName()).equals(itemName))
                    {
                        if (!unlocalizedNames.contains(MadUtils.cleanTag(vanillaItemStack.getUnlocalizedName())))
                        {
                            itemsToAssociate.add(vanillaItemStack);
                            unlocalizedNames.add(MadUtils.cleanTag(vanillaItemStack.getUnlocalizedName()));
                        }
                    }
                }
                catch (Exception err)
                {
                    continue;
                }
            }
        }

        // Last ditch effort to save compatibility starts here!
        if (itemName.equals("dyePowder") || itemName.equals("dye"))
        {
            // Return whatever type of dye was requested.
            itemsToAssociate.add(new ItemStack(Item.dyePowder, stackSize, metaData));
        }

        if (itemName.equals("wool") || itemName.equals("cloth"))
        {
            // Return whatever color wool was requested.
            itemsToAssociate.add(new ItemStack(Block.cloth, stackSize, metaData));
        }

        // Check if we have items to return back after all that work.
        if (itemsToAssociate.size() > 0)
        {
            return itemsToAssociate.toArray(new ItemStack[]{});
        }

        // Default response is to return nothing.
        return null;
    }

    /** Registers product data that is loaded from JSON or created manually in class files. This method will register the machine with our own internal systems
     *  but also all required Minecraft/Forge systems (whatever they may be since ForgeTeam like to change it all the time anyway). */
    public MadTileEntityFactoryProduct registerMachine(MadTileEntityFactoryProductData machineData) throws IllegalArgumentException
    {
        // Pass the data object into the product to activate it, creates needed data structures inside it based on data supplied.
        MadTileEntityFactoryProduct tileEntityProduct = new MadTileEntityFactoryProduct(machineData);

        // Check to make sure we have not added this machine before.
        if (!isValidMachineID(tileEntityProduct.getMachineName()))
        {
            throw new IllegalArgumentException("Duplicate MadTileEntityFactoryProduct '" + tileEntityProduct.getMachineName() + "' was added. Execution halted!");
        }

        // Debugging!
        MadMod.log().info("[MadTileEntityFactory]Registering machine: " + tileEntityProduct.getMachineName());
        
        // Check if rendering information is null and needs to be set to defaults.
        this.checkRenderingInformation(tileEntityProduct);
        
        // Ensure that ghost block config at minimum has the single block that is the machine.
        this.checkGhostBlockConfig(tileEntityProduct);

        // Actually register the machine with the product listing.
        registeredMachines.put(tileEntityProduct.getMachineName(), tileEntityProduct);
        
        // Register the machine with Minecraft/Forge.
        GameRegistry.registerTileEntity(tileEntityProduct.getTileEntityLogicClass(), tileEntityProduct.getMachineName());
        GameRegistry.registerBlock(tileEntityProduct.getBlockContainer(), ItemBlockTooltip.class, MadMod.ID + tileEntityProduct.getMachineName());
        
        // Register client only information such as rendering and model information to the given machine.
        MadForgeMod.proxy.registerRenderingHandler(tileEntityProduct.getBlockID());

        return tileEntityProduct;
    }

    /** Ensures that machine being registered at very minimum specifies the single block that is itself with empty int based vector. */
    private void checkGhostBlockConfig(MadTileEntityFactoryProduct tileEntityProduct)
    {
        MadGhostBlockData machineGhostBlocks = tileEntityProduct.getMultiBlockConfiguration();
        if (machineGhostBlocks == null)
        {
            // Default is no ghost blocks on any sides with only the machine itself in the center.
            tileEntityProduct.setMultiBlockDefaults();
        }
    }

    /** Ensures that a given tile entity factory product will always have proper rendering information even if none is provided (for whatever reason). */
    private void checkRenderingInformation(MadTileEntityFactoryProduct tileEntityProduct)
    {
        // Check if model rendering information exists.
        MadModel renderingInformation = tileEntityProduct.getModelArchive();
        if (renderingInformation != null)
        {
            // Rendering information for tile as it would exist as an item block in players inventory.
            if (renderingInformation.getItemRenderInfoClone() == null)
            {
                MadMod.log().info("[" + tileEntityProduct.getMachineName() + "]Creating default ITEM rendering information where there is none.");
                renderingInformation.setItemRenderInfoDefaults();
            }
            
            // Rendering information for tile as it would exist in the game world as seen by the player and other players.
            if (renderingInformation.getWorldRenderInfoClone() == null)
            {
                MadMod.log().info("[" + tileEntityProduct.getMachineName() + "]Creating default WORLD rendering information where there is none.");
                renderingInformation.setWorldRenderInfoDefaults();
            }
        }
        else
        {
            throw new IllegalArgumentException("Cannot register MadTileEntityFactoryProduct '" + tileEntityProduct.getMachineName() + "'. This tile entity contains no models!");
        }
    }
}