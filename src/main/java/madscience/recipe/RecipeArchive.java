package madscience.recipe;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import cpw.mods.fml.common.registry.GameRegistry;
import madscience.ModMetadata;
import madscience.crafting.CraftingRecipe;
import madscience.crafting.CraftingRecipeComponent;
import madscience.crafting.CraftingRecipeTypeEnum;
import madscience.furnace.FurnaceRecipe;
import madscience.mod.ModLoader;
import madscience.util.MiscUtils;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public final class RecipeArchive
{
    /**
     * Master list of recipes for this machine.
     */
    private final HashMap<List<RecipeComponent[]>, RecipeComponent[]> smeltingList =
            new HashMap<List<RecipeComponent[]>, RecipeComponent[]>();

    /**
     * Final input result of recipe.
     */
    @Expose
    @SerializedName("InputIngredientsArray")
    private final RecipeComponent[] inputIngredientsArray = null;

    /**
     * Final output result of recipe.
     */
    @Expose
    @SerializedName("OutputResultsArray")
    private final RecipeComponent[] outputResultsArray = null;

    /**
     * Amount of time in seconds it should take to create this recipe.
     */
    @Expose
    @SerializedName("CreationTimeInSeconds")
    private final int creationTimeInSeconds = 0;

    /**
     * Amount of experience the player will get after crafting this recipe.
     */
    @Expose
    @SerializedName("ExperienceFromCreation")
    private final float experienceFromCreation = 0.0F;

    /**
     * Loads recipes associated with the product itself so that it may be crafted in the game.
     * This should only be run once, and in postInit after all other mods have been loaded so products from them may be queried.
     */
    public static void loadCraftingRecipes(CraftingRecipe[] recipesToLoad, String productName, ItemStack craftingResult)
    {
        int totalLoadedRecipeItems = 0;
        int totalFailedRecipeItems = 0;

        // Skip items that have no crafting recipes to register.
        if (recipesToLoad == null)
        {
            return;
        }

        // Grab the recipe data for each individual sub-item.
        for (CraftingRecipe itemCraftingRecipe : recipesToLoad)
        {
            // Keeps track if anything bad happened during crafting recipe creation.
            boolean errorsWithAssociation = true;

            // Reference to recipe we want to create, Minecraft/Forge accepts them as object array.
            List<Object> craftingInputArray = new ArrayList<Object>();

            // Crafting strings that makeup reference to what numbers goto what slot on the grid.
            Object[] craftingGridLayout = new Object[9];

            // Initialize each slot with empty space since in Minecraft/Forge this means null/nothing.
            for (int i = 0; i < craftingGridLayout.length; i++)
            {
                craftingGridLayout[i] = " ";
            }

            for (CraftingRecipeComponent recipeComponent : itemCraftingRecipe.getCraftingRecipeComponents())
            {
                // Start building output string now, append to it below.
                String resultInputPrint = "[" + productName +
                                          "]Crafting Component " +
                                          recipeComponent.getModID() + ":" +
                                          recipeComponent.getInternalName();

                // Query game registry and vanilla blocks and items for the incoming name in an attempt to turn it into an itemstack.
                ItemStack inputItem = RecipeArchive.getItemStackFromString( recipeComponent.getModID(),
                                                                            recipeComponent.getInternalName(),
                                                                            recipeComponent.getAmount(),
                                                                            recipeComponent.getMetaDamage() );

                boolean searchResult = false;
                if (inputItem != null)
                {
                    searchResult = true;
                    errorsWithAssociation = false;
                    totalLoadedRecipeItems++;
                    resultInputPrint += "=SUCCESS";
                    recipeComponent.associateItemStackToRecipeComponent( inputItem );
                }
                else
                {
                    searchResult = false;
                    errorsWithAssociation = true;
                    totalFailedRecipeItems++;
                    resultInputPrint += "=FAILED";
                }

                // Only add the craft slot number if we are making a shaped recipe.
                if (itemCraftingRecipe.getCraftingRecipeType() == CraftingRecipeTypeEnum.SHAPED)
                {
                    // First, place the prepared crafting component into grid as a string.
                    craftingGridLayout[recipeComponent.getCraftingGridPosition()] =
                            String.valueOf( recipeComponent.getCraftingGridPosition() );

                    // Second, add to input array the crafting grid number.
                    craftingInputArray.add( recipeComponent.getCraftingGridPositionAsCharacter() );
                }

                // Third, add to input array the crafting ingredient (we always will want this).
                if (recipeComponent.isLoaded())
                {
                    craftingInputArray.add( recipeComponent.getAssociatedItemStack() );
                }

                // Debugging!
                if (! searchResult)
                {
                    ModLoader.log().info( resultInputPrint );
                }
            }

            // If the above crafting component registration went well then we will add them.
            if (! errorsWithAssociation)
            {
                // Depending on type of recipe, very different things need to happen with the data we have collected so far.
                switch (itemCraftingRecipe.getCraftingRecipeType())
                {
                    case SHAPED:
                    {
                        // Move the string array into proper alignment for recipe input, any slots without components become blank spaces this is intentional.
                        String[] craftingGridLayoutFinal = new String[3];
                        craftingGridLayoutFinal[0] = String.valueOf( craftingGridLayout[0] ) +
                                                     String.valueOf( craftingGridLayout[1] ) +
                                                     String.valueOf( craftingGridLayout[2] );
                        craftingGridLayoutFinal[1] = String.valueOf( craftingGridLayout[3] ) +
                                                     String.valueOf( craftingGridLayout[4] ) +
                                                     String.valueOf( craftingGridLayout[5] );
                        craftingGridLayoutFinal[2] = String.valueOf( craftingGridLayout[6] ) +
                                                     String.valueOf( craftingGridLayout[7] ) +
                                                     String.valueOf( craftingGridLayout[8] );

                        // Construct the final object array recipe input.
                        List<Object> recipeFinalInput = new ArrayList<Object>();
                        recipeFinalInput.add( craftingGridLayoutFinal[0] );
                        recipeFinalInput.add( craftingGridLayoutFinal[1] );
                        recipeFinalInput.add( craftingGridLayoutFinal[2] );
                        recipeFinalInput.addAll( craftingInputArray );

                        // Actually add the recipe to Minecraft/Forge.
                        try
                        {
                            GameRegistry.addShapedRecipe( new ItemStack( craftingResult.getItem(),
                                                                         itemCraftingRecipe.getCraftingAmount(),
                                                                         craftingResult.getItemDamage() ),
                                                          recipeFinalInput.toArray( new Object[] {} ) );
                        }
                        catch (Exception err)
                        {
                            ModLoader.log().info( "[" + productName +
                                                  "]Unable to load shaped crafting recipe!" );
                        }

                        break;
                    }
                    case SHAPELESS:
                    {
                        try
                        {
                            // Shapeless recipes are a little easier since we only need to pass in the item array.
                            GameRegistry.addShapelessRecipe( new ItemStack( craftingResult.getItem(),
                                                                            itemCraftingRecipe.getCraftingAmount(),
                                                                            craftingResult.getItemDamage() ),
                                                             craftingInputArray.toArray( new Object[] {} ) );
                        }
                        catch (Exception err)
                        {
                            ModLoader.log().info( "[" + productName +
                                                  "]Unable to load shapeless crafting recipe!" );
                        }

                        break;
                    }
                    default:
                    {
                        // Nothing to see here.
                        break;
                    }
                }
            }
            else
            {
                ModLoader.log().info( "[" + productName +
                                      "]Bad Crafting Recipe: " +
                                      itemCraftingRecipe.getCraftingRecipeType().name() );
            }
        }

        // Information about total loaded and failed.
        ModLoader.log().info( "[" + productName +
                              "]Total Loaded Crafting Recipe Items: " +
                              totalLoadedRecipeItems );
        ModLoader.log().info( "[" + productName +
                              "]Failed To Load Crafting Recipe Items: " +
                              totalFailedRecipeItems );
    }

    /**
     * Parses input and output items and associates them with Minecraft/Forge ItemStacks.
     * After this it will register the completed items with vanilla cobblestone Furnace.
     *
     * @param madFurnaceRecipes
     */
    public static void loadVanillaFurnaceRecipes(FurnaceRecipe[] madFurnaceRecipes)
    {
        // Skip items that have no vanilla furnace recipes.
        if (madFurnaceRecipes == null)
        {
            return;
        }

        // Loop through all the vanilla furnace recipes and associate them with Minecraft/Forge ItemStacks.
        for (FurnaceRecipe furnaceRecipe : madFurnaceRecipes)
        {
            // Input Component
            if (furnaceRecipe.getInputComponent() != null)
            {
                // Query game registry and vanilla blocks and items for the incoming name in an attempt to turn it into an itemstack.
                ItemStack inputItem =
                        RecipeArchive.getItemStackFromString( furnaceRecipe.getInputComponent().getModID(),
                                                              furnaceRecipe.getInputComponent().getInternalName(),
                                                              furnaceRecipe.getInputComponent().getAmount(),
                                                              furnaceRecipe.getInputComponent().getMetaDamage() );

                if (inputItem != null && ! furnaceRecipe.getInputComponent().isLoaded())
                {
                    furnaceRecipe.getInputComponent().associateItemStackToRecipeComponent( inputItem );
                }
            }

            // Output Component
            if (furnaceRecipe.getOutputComponent() != null)
            {
                ItemStack outputItem =
                        RecipeArchive.getItemStackFromString( furnaceRecipe.getOutputComponent().getModID(),
                                                              furnaceRecipe.getOutputComponent().getInternalName(),
                                                              furnaceRecipe.getOutputComponent().getAmount(),
                                                              furnaceRecipe.getOutputComponent().getMetaDamage() );

                if (outputItem != null && ! furnaceRecipe.getOutputComponent().isLoaded())
                {
                    furnaceRecipe.getOutputComponent().associateItemStackToRecipeComponent( outputItem );
                }
            }

            // Now that input and output items are loaded lets apply this to vanilla furnace.
            if (furnaceRecipe.getInputComponent().isLoaded() && furnaceRecipe.getOutputComponent().isLoaded())
            {
                ItemStack inputItem = furnaceRecipe.getInputComponent().getAssociatedItemStack();
                ItemStack outputItem = furnaceRecipe.getOutputComponent().getAssociatedItemStack();
                if (inputItem != null && outputItem != null)
                {
                    FurnaceRecipes.smelting().addSmelting( inputItem.itemID,
                                                           outputItem,
                                                           0.0F );
                }
            }
        }
    }

    /**
     * Return itemstack from GameRegistry or from vanilla Item/Block list.
     */
    public static ItemStack getItemStackFromString(String modID, String itemName, int stackSize, int metaData)
    {
        // Abort if name is empty or null.
        if (itemName == null)
        {
            return null;
        }

        if (itemName.isEmpty())
        {
            return null;
        }

        // Check if this object is from our own mod project.
        if (modID.equals( ModMetadata.ID ))
        {
            // Remove prefix from the item name if from our own project.
            itemName = MiscUtils.cleanTag( itemName );
        }

        // Check for some more trick items and ensure we always return the item version of them (ex. redstone comparator).
        ItemStack trickVanillaItem = findTrickyItemStack( itemName,
                                                          stackSize,
                                                          metaData );
        if (trickVanillaItem != null)
        {
            return trickVanillaItem.copy();
        }

        // Minecraft Forge related method for locating item and block data (fails most of the time).
        ItemStack potentialModItem = GameRegistry.findItemStack( modID,
                                                                 itemName,
                                                                 stackSize );
        if (potentialModItem != null)
        {
            return new ItemStack( potentialModItem.getItem(),
                                  stackSize,
                                  metaData );
        }

        // Find block.
        Block potentialBlock = GameRegistry.findBlock( modID,
                                                       itemName );
        if (potentialBlock != null)
        {
            return new ItemStack( potentialBlock,
                                  stackSize,
                                  metaData );
        }

        // Find item.
        Item potentialItem = GameRegistry.findItem( modID,
                                                    itemName );
        if (potentialItem != null)
        {
            return new ItemStack( potentialItem,
                                  stackSize,
                                  metaData );
        }

        // Vanilla item query.
        for (Item potentialMCItem : Item.itemsList)
        {
            if (potentialMCItem == null)
            {
                continue;
            }

            ItemStack vanillaItemStack = new ItemStack( potentialMCItem,
                                                        stackSize,
                                                        metaData );
            if (vanillaItemStack != null)
            {
                try
                {
                    if (vanillaItemStack.getUnlocalizedName().equals( itemName ))
                    {
                        return new ItemStack( vanillaItemStack.getItem(),
                                              stackSize,
                                              metaData );
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

            ItemStack vanillaItemStack = new ItemStack( potentialMCBlock,
                                                        stackSize,
                                                        metaData );
            if (vanillaItemStack != null)
            {
                try
                {
                    if (vanillaItemStack.getUnlocalizedName().equals( itemName ))
                    {
                        return new ItemStack( vanillaItemStack.getItem(),
                                              stackSize,
                                              metaData );
                    }
                }
                catch (Exception err)
                {
                    continue;
                }
            }
        }

        if (itemName.equals( "dyePowder" ) || itemName.equals( "dye" ))
        {
            // Return whatever type of dye was requested.
            return new ItemStack( Item.dyePowder,
                                  stackSize,
                                  metaData );
        }

        if (itemName.equals( "wool" ) || itemName.equals( "cloth" ))
        {
            // Return whatever color wool was requested.
            return new ItemStack( Block.cloth,
                                  stackSize,
                                  metaData );
        }

        return null;
    }

    /**
     * Parses some harder to locate and differentiate items and blocks to ensure the version players want is returned.
     * Entries are added manually to this list, don't expect any miracles.
     */
    private static ItemStack findTrickyItemStack(String itemName, int stackSize, int metaData)
    {
        if (itemName.equals( "tile.comparator" ))
        {
            // Return the item version of the redstone comparator since the player will not want the tile version.
            return new ItemStack( Item.comparator,
                                  stackSize,
                                  metaData );
        }

        if (itemName.equals( "tile.diode" ))
        {
            // Return the item version of the redstone repeater since you never want the tile (placed in world) version.
            return new ItemStack( Item.redstoneRepeater,
                                  stackSize,
                                  metaData );
        }

        if (itemName.equals( "item.milk" ))
        {
            // Return a bucket of cow's milk.
            return new ItemStack( Item.bucketMilk,
                                  stackSize,
                                  metaData );
        }

        if (itemName.equals( "item.dyePowder" ) || itemName.equals( "item.dye" ))
        {
            // Return whatever type of dye was requested.
            return new ItemStack( Item.dyePowder,
                                  stackSize,
                                  metaData );
        }

        if (itemName.equals( "tile.wool" ) || itemName.equals( "tile.cloth" ))
        {
            // Return whatever color wool was requested.
            return new ItemStack( Block.cloth,
                                  stackSize,
                                  metaData );
        }

        return null;
    }

    public int getCreationTimeInSeconds()
    {
        return this.creationTimeInSeconds;
    }

    public int getCreationTimeInTicks()
    {
        return this.creationTimeInSeconds * MiscUtils.SECOND_IN_TICKS;
    }

    public RecipeComponent[] getInputIngredientsArray()
    {
        return inputIngredientsArray;
    }

    public RecipeComponent[] getOutputResultsArray()
    {
        return outputResultsArray;
    }

    public float getExperienceFromCreation()
    {
        return experienceFromCreation;
    }
}