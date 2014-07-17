package madscience;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Level;

import madscience.factory.MadTileEntityFactory;
import madscience.factory.MadTileEntityFactoryProduct;
import madscience.factory.MadTileEntityFactoryProductData;
import madscience.factory.mod.MadMod;
import madscience.gui.MadGUI;
import madscience.items.spawnegg.MadSpawnEggTags;
import madscience.mobs.abomination.AbominationMobEntity;
import madscience.mobs.abomination.AbominationMobLivingHandler;
import madscience.mobs.creepercow.CreeperCowMobEntity;
import madscience.mobs.enderslime.EnderslimeMobEntity;
import madscience.mobs.endersquid.EnderSquidMobEntity;
import madscience.mobs.shoggoth.ShoggothMobEntity;
import madscience.mobs.werewolf.WerewolfMobEntity;
import madscience.mobs.woolycow.WoolyCowMobEntity;
import madscience.network.MadPacketHandler;
import madscience.server.CommonProxy;
import madscience.util.MadColors;
import net.minecraft.entity.EntityList;
import net.minecraft.launchwrapper.LogWrapper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLFingerprintViolationEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;

@Mod(modid = MadMod.ID, name = MadMod.NAME, version = MadMod.VERSION_FULL, useMetadata = false, acceptedMinecraftVersions = MadMod.MINECRAFT_VERSION, dependencies = MadMod.DEPENDENCIES)
@NetworkMod(channels = { MadMod.CHANNEL_NAME }, packetHandler = MadPacketHandler.class, clientSideRequired = true, serverSideRequired = false)
public class MadForgeMod
{
    // Proxy that runs commands based on where they are from so we can separate server and client logic easily.
    @SidedProxy(clientSide = MadMod.CLIENT_PROXY, serverSide = MadMod.SERVER_PROXY)
    public static CommonProxy proxy;

    // Public instance of our mod that Forge needs to hook us, based on our internal modid.
    @Instance(value = MadMod.CHANNEL_NAME)
    public static MadForgeMod instance;

    // Public extra data about our mod that Forge uses in the mods listing page for more information.
    @Mod.Metadata(MadMod.ID)
    private static ModMetadata metadata;

    // Hooks Forge's replacement openGUI function so we can route our block ID's to proper interfaces.
    private static MadGUI guiHandler = new MadGUI();

    // Link to our configuration file which Forge also handles in a standardized way.
    private static Configuration config;
    
    /** @param event */
    @EventHandler
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void postInit(FMLPostInitializationEvent event) // NO_UCD (unused code)
    {
        // ---------------
        // FACTORY RECIPES
        // ---------------
        
        // Loop through all the tile entity factory objects and populate their recipe ItemStacks.
        Iterable<MadTileEntityFactoryProduct> registeredMachines = MadTileEntityFactory.getMachineInfoList();
        for (Iterator iterator = registeredMachines.iterator(); iterator.hasNext();)
        {
            MadTileEntityFactoryProduct registeredMachine = (MadTileEntityFactoryProduct) iterator.next();
            if (registeredMachine != null)
            {
                // Recipes that pertain to machine itself, association slots with items they should have in them.
                registeredMachine.loadMachineInternalRecipes();
                
                // Recipes for crafting the machine itself, registered with Minecraft/Forge GameRegistry.
                registeredMachine.loadCraftingRecipes();
            }
        }
        
        // ----------------
        // NOT ENOUGH ITEMS
        // ----------------
        
        // Interface with NEI and attempt to call functions from it if it exists.
        // Note: This method was given by Alex_hawks, buy him a beer if you see him!
        if (Loader.isModLoaded("NotEnoughItems"))
        {
            try
            {
                Class clazz = Class.forName("codechicken.nei.api.API");
                Method m = clazz.getMethod("hideItem", Integer.TYPE);

                // Cryotube Ghost Block.
                m.invoke(null, MadFurnaces.CRYOTUBEGHOST.blockID);

                // Soniclocator Ghost Block.
                m.invoke(null, MadFurnaces.SONICLOCATORGHOST.blockID);

                // Magazine Loader Ghost Block.
                m.invoke(null, MadFurnaces.MAGLOADERGHOST.blockID);
                
                // CnC Machine Ghost Block.
                m.invoke(null, MadFurnaces.CNCMACHINEGHOST_TILEENTITY.blockID);
            }
            catch (Throwable e)
            {
                MadMod.LOGGER.log(Level.WARNING, "NEI Integration has failed...");
                MadMod.LOGGER.log(Level.WARNING, "Please email devs@madsciencemod.com the following stacktrace.");
                e.printStackTrace();
                MadMod.LOGGER.log(Level.WARNING, "Spamming console to make more obvious...");
                for (int i = 0; i < 15; i++)
                {
                    MadMod.LOGGER.log(Level.WARNING, "Something Broke. See above.");
                }
            }

        }
        
        // ---------
        // DEBUGGING
        // ---------
        
        // Prints out all internal names.
        proxy.dumpUnlocalizedNames();
        
        // Dumps all registered machines JSON to disk.
        proxy.dumpAllMachineJSON();
    }

    /** @param event */
    @EventHandler
    public void init(FMLInitializationEvent event) // NO_UCD (unused code)
    {
        // Register any custom sounds we want to play (Client only).
        proxy.registerSoundHandler();
        
        // Register block handler for custom GUI.
        NetworkRegistry.instance().registerGuiHandler(MadForgeMod.instance, MadForgeMod.guiHandler);

        // Check Mad Science Jenkins build server for latest build numbers to compare with running one.
        MadUpdates.checkJenkinsBuildNumbers();
    }

    @EventHandler
    public void invalidFingerprint(FMLFingerprintViolationEvent event) // NO_UCD (unused code)
    {
        // Report (log) to the user that the version of Mad Science
        // they are using has been changed/tampered with
        if (MadMod.FINGERPRINT.equals(MadMod.FINGERPRINT))
        {
            LogWrapper.warning("The copy of Mad Science that you are running is a development version of the mod, and as such may be unstable and/or incomplete.");
        }
        else
        {
            LogWrapper.severe("The copy of Mad Science that you are running has been modified from the original, and unpredictable things may happen. Please consider re-downloading the original version of the mod.");
        }
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) // NO_UCD (unused code)
    {
        // --------------
        // PRE-INT CONFIG
        // --------------
        
        // Register instance.
        instance = this;

        // Logging.
        MadMod.LOGGER = event.getModLog();
        MadMod.LOGGER.setParent(FMLLog.getLogger());

        // Read our mod only config, Forge provides a method for getting standardized filename.
        config = new Configuration(event.getSuggestedConfigurationFile());
        MadConfig.load(config);

        // Setup Mod Metadata for players to see in mod list with other mods.
        metadata.modId = MadMod.ID;
        metadata.name = MadMod.NAME;
        metadata.description = MadMod.DESCRIPTION;
        metadata.url = MadMod.HOME_URL;
        metadata.logoFile = MadMod.LOGO_PATH;
        metadata.version = MadMod.VMAJOR + "." + MadMod.VMINOR + MadMod.VREVISION;
        metadata.authorList = Arrays.asList(MadMod.AUTHORS);
        metadata.credits = MadMod.CREDITS;
        metadata.autogenerated = false;

        // Custom creative mode tab to organize our mod items and blocks.
        MadEntities.createCustomCreativeTab("tabMadScience", MadMod.NAME);

        // ------
        // FLUIDS
        // ------
        MadMod.LOGGER.info("Creating Fluids");

        // Creates both flowing and still variants of new fluid.
        // Note: Still's ID must be 1 above Flowing.
        MadFluids.createLiquidDNA(MadConfig.LIQUIDDNA, MadConfig.LIQUIDDNA_BUCKET);

        // Liquid Mutant DNA.
        MadFluids.createLiquidDNAMutant(MadConfig.LIQUIDDNA_MUTANT, MadConfig.LIQUIDDNA_MUTANT_BUCKET);

        // ------
        // BLOCKS
        // ------
        MadMod.LOGGER.info("Creating Blocks");

        // Abomination Egg
        MadBlocks.createAbominationEgg(MadConfig.ABOMINATIONEGG);

        // Enderslime Block
        MadBlocks.createEnderslimeBlock(MadConfig.ENDERSLIMEBLOCK);

        // ----------
        // COMPONENTS
        // ----------
        MadMod.LOGGER.info("Creating Components");

        MadComponents.createComponentCaseItem(MadConfig.COMPONENT_CASE);
        MadComponents.createComponentCPUItem(MadConfig.COMPONENT_CPU);
        MadComponents.createComponentFanItem(MadConfig.COMPONENT_FAN);
        MadComponents.createComponentFusedQuartzItem(MadConfig.COMPONENT_FUSEDQUARTZ);
        MadComponents.createComponentMagneticTapeItem(MadConfig.COMPONENT_MAGNETICTAPE);
        MadComponents.createComponentPowerSupplyItem(MadConfig.COMPONENT_POWERSUPPLY);
        MadComponents.createComponentRAMItem(MadConfig.COMPONENT_RAM);
        MadComponents.createComponentScreenItem(MadConfig.COMPONENT_SCREEN);
        MadComponents.createComponentSiliconWaferItem(MadConfig.COMPONENT_SILICONWAFER);
        MadComponents.createComponentTransistorItem(MadConfig.COMPONENT_TRANSISTOR);
        MadComponents.createComponentComputerItem(MadConfig.COMPONENT_COMPUTER);
        MadComponents.createComponentThumperItem(MadConfig.COMPONENT_THUMPER);
        MadComponents.createComponentEnderslimeItem(MadConfig.COMPONENT_ENDERSLIME);
        MadComponents.createComponentPulseRifleBarrelItem(MadConfig.COMPONENT_PULSERIFLEBARREL);
        MadComponents.createComponentPulseRifleBoltItem(MadConfig.COMPONENT_PULSERIFLEBOLT);
        MadComponents.createComponentPulseRifleReceiverItem(MadConfig.COMPONENT_PULSERIFLERECEIVER);
        MadComponents.createComponentPulseRifleTriggerItem(MadConfig.COMPONENT_PULSERIFLETRIGGER);
        MadComponents.createComponentPulseRifleBulletCasingItem(MadConfig.COMPONENT_PULSERIFLEBULLETCASING);
        MadComponents.createComponentPulseRifleGrenadeCasingItem(MadConfig.COMPONENT_PULSERIFLEGRENADECASING);

        // --------
        // CIRCUITS
        // --------
        MadMod.LOGGER.info("Creating Circuits");

        MadCircuits.createCircuitComparatorItem(MadConfig.CIRCUIT_COMPARATOR);
        MadCircuits.createCircuitDiamondItem(MadConfig.CIRCUIT_DIAMOND);
        MadCircuits.createCircuitEmeraldItem(MadConfig.CIRCUIT_EMERALD);
        MadCircuits.createCircuitEnderEyeItem(MadConfig.CIRCUIT_ENDEREYE);
        MadCircuits.createCircuitEnderPerlItem(MadConfig.CIRCUIT_ENDERPEARL);
        MadCircuits.createCircuitGlowstoneItem(MadConfig.CIRCUIT_GLOWSTONE);
        MadCircuits.createCircuitRedstoneItem(MadConfig.CIRCUIT_REDSTONE);
        MadCircuits.createCircuitSpiderEyeItem(MadConfig.CIRCUIT_SPIDEREYE);

        // -------
        // NEEDLES
        // -------
        MadMod.LOGGER.info("Creating Needles");

        MadNeedles.createEmptyNeedle(MadConfig.NEEDLE_EMPTY);
        MadNeedles.createDirtyNeedle(MadConfig.NEEDLE_DIRTY);
        MadNeedles.createCaveSpiderNeedle(MadConfig.NEEDLE_CAVESPIDER);
        MadNeedles.createChickenNeedle(MadConfig.NEEDLE_CHICKEN);
        MadNeedles.createCowNeedle(MadConfig.NEEDLE_COW);
        MadNeedles.createCreeperNeedle(MadConfig.NEEDLE_CREEPER);
        MadNeedles.createEndermanNeedle(MadConfig.NEEDLE_ENDERMAN);
        MadNeedles.createHorseNeedle(MadConfig.NEEDLE_HORSE);
        MadNeedles.createMushroomCowNeedle(MadConfig.NEEDLE_MUSHROOMCOW);
        MadNeedles.createOcelotNeedle(MadConfig.NEEDLE_OCELOT);
        MadNeedles.createPigNeedle(MadConfig.NEEDLE_PIG);
        MadNeedles.createSheepNeedle(MadConfig.NEEDLE_SHEEP);
        MadNeedles.createSpiderNeedle(MadConfig.NEEDLE_SPIDER);
        MadNeedles.createSquidNeedle(MadConfig.NEEDLE_SQUID);
        MadNeedles.createVillagerNeedle(MadConfig.NEEDLE_VILLAGER);
        MadNeedles.createWitchNeedle(MadConfig.NEEDLE_WITCH);
        MadNeedles.createWolfNeedle(MadConfig.NEEDLE_WOLF);
        MadNeedles.createZombieNeedle(MadConfig.NEEDLE_ZOMBIE);
        MadNeedles.createMutantNeedle(MadConfig.NEEDLE_MUTANT);
        MadNeedles.createBatNeedle(MadConfig.NEEDLE_BAT);

        // -----------
        // DNA SAMPLES
        // -----------
        MadMod.LOGGER.info("Creating DNA Samples");

        MadDNA.createCaveSpiderDNA(MadConfig.DNA_CAVESPIDER);
        MadDNA.createChickenDNA(MadConfig.DNA_CHICKEN);
        MadDNA.createCowDNA(MadConfig.DNA_COW);
        MadDNA.createCreeperDNA(MadConfig.DNA_CREEPER);
        MadDNA.createEndermanDNA(MadConfig.DNA_ENDERMAN);
        MadDNA.createGhastDNA(MadConfig.DNA_GHAST);
        MadDNA.createHorseDNA(MadConfig.DNA_HORSE);
        MadDNA.createMushroomCowDNA(MadConfig.DNA_MUSHROOMCOW);
        MadDNA.createOcelotDNA(MadConfig.DNA_OCELOT);
        MadDNA.createPigDNA(MadConfig.DNA_PIG);
        MadDNA.createSheepDNA(MadConfig.DNA_SHEEP);
        MadDNA.createSkeletonDNA(MadConfig.DNA_SKELETON);
        MadDNA.createSpiderDNA(MadConfig.DNA_SPIDER);
        MadDNA.createSquidDNA(MadConfig.DNA_SQUID);
        MadDNA.createVillagerDNA(MadConfig.DNA_VILLAGER);
        MadDNA.createWitchDNA(MadConfig.DNA_WITCH);
        MadDNA.createWolfDNA(MadConfig.DNA_WOLF);
        MadDNA.createZombieDNA(MadConfig.DNA_ZOMBIE);
        MadDNA.createBatDNA(MadConfig.DNA_BAT);
        MadDNA.createSlimeDNA(MadConfig.DNA_SLIME);

        // -----------------
        // GENOME DATA REELS
        // -----------------
        MadMod.LOGGER.info("Creating Genome Data Reels");

        MadEntities.createEmptyDataReel(MadConfig.DATAREEL_EMPTY);
        MadGenomes.createCaveSpiderGenome(MadConfig.GENOME_CAVESPIDER);
        MadGenomes.createChickenGenome(MadConfig.GENOME_CHICKEN);
        MadGenomes.createCowGenome(MadConfig.GENOME_COW);
        MadGenomes.createCreeperGenome(MadConfig.GENOME_CREEPER);
        MadGenomes.createEndermanGenome(MadConfig.GENOME_ENDERMAN);
        MadGenomes.createGhastGenome(MadConfig.GENOME_GHAST);
        MadGenomes.createHorseGenome(MadConfig.GENOME_HORSE);
        MadGenomes.createMushroomCowGenome(MadConfig.GENOME_MUSHROOMCOW);
        MadGenomes.createOcelotGenome(MadConfig.GENOME_OCELOT);
        MadGenomes.createPigGenome(MadConfig.GENOME_PIG);
        MadGenomes.createSheepGenome(MadConfig.GENOME_SHEEP);
        MadGenomes.createSkeletonGenome(MadConfig.GENOME_SKELETON);
        MadGenomes.createSpiderGenome(MadConfig.GENOME_SPIDER);
        MadGenomes.createSquidGenome(MadConfig.GENOME_SQUID);
        MadGenomes.createVillagerGenome(MadConfig.GENOME_VILLAGER);
        MadGenomes.createWitchGenome(MadConfig.GENOME_WITCH);
        MadGenomes.createWolfGenome(MadConfig.GENOME_WOLF);
        MadGenomes.createZombieGenome(MadConfig.GENOME_ZOMBIE);
        MadGenomes.createBatGenome(MadConfig.GENOME_BAT);
        MadGenomes.createSlimeGenome(MadConfig.GENOME_SLIME);
        MadGenomes.createPigZombieGenome(MadConfig.GENOME_PIGZOMBIE);
        
        // -------
        // WEAPONS
        // -------
        MadMod.LOGGER.info("Creating Weapons");

        MadWeapons.createPulseRifle(MadConfig.WEAPON_PULSERIFLE);
        MadWeapons.createPulseRifleBullet(MadConfig.WEAPON_PULSERIFLE_BULLETITEM);
        MadWeapons.createPulseRifleGrenade(MadConfig.WEAPON_PULSERIFLE_GRENADEITEM);
        MadWeapons.createPulseRifleMagazine(MadConfig.WEAPON_PULSERIFLE_MAGAZINEITEM);

        // -------------
        // TILE ENTITIES
        // -------------
        MadMod.LOGGER.info("Creating Tile Entities");
        
        // Take the machines from loaded mod instance and register them with tile entity factory.
        MadTileEntityFactoryProductData[] machineData = MadMod.getUnregisteredMachines();
        for (int i = 0; i < machineData.length; i++) 
        {
            MadTileEntityFactoryProductData unregisteredMachine = machineData[i];
            MadTileEntityFactory.registerMachine(unregisteredMachine);
         }

        MadFurnaces.createSanitizerTileEntity(MadConfig.SANTITIZER);
        MadFurnaces.createMainframeTileEntity(MadConfig.MAINFRAME);
        MadFurnaces.createGeneSequencerTileEntity(MadConfig.GENE_SEQUENCER);
        MadFurnaces.createCryoFreezerTileEntity(MadConfig.CRYOFREEZER);
        MadFurnaces.createGeneIncubatorTileEntity(MadConfig.INCUBATOR);
        MadFurnaces.createCryotubeTileEntity(MadConfig.CRYOTUBE);
        MadFurnaces.createCryotubeGhostTileEntity(MadConfig.CRYOTUBEGHOST);
        MadFurnaces.createThermosonicBonderTileEntity(MadConfig.THERMOSONIC);
        MadFurnaces.createDataReelDuplicatorTileEntity(MadConfig.DATADUPLICATOR);
        MadFurnaces.createSoniclocatorTileEntity(MadConfig.SONICLOCATOR);
        MadFurnaces.createSoniclocatorGhostTileEntity(MadConfig.SONICLOCATOREGHOST);
        MadFurnaces.createClayFurnaceTileEntity(MadConfig.CLAYFURNACE);
        MadFurnaces.createVOXBoxTileEntity(MadConfig.VOXBOX);
        MadFurnaces.createMagLoaderTileEntity(MadConfig.MAGLOADER);
        MadFurnaces.createMagLoaderGhostTileEntity(MadConfig.MAGLOADERGHOST);
        MadFurnaces.createCnCMachineTileEntity(MadConfig.CNCMACHINE);
        MadFurnaces.createCnCMachineGhostTileEntity(MadConfig.CNCMACHINEGHOST);
        
        // -----
        // ARMOR
        // -----

        // Note: Armor follows an array 0,1,2,3 for helmet, body, leggings, boots.
        MadEntities.createLabCoatGoggles(MadConfig.LABCOAT_GOGGLES, 0);
        MadEntities.createLabCoatBody(MadConfig.LABCOAT_BODY, 1);
        MadEntities.createLabCoatLeggings(MadConfig.LABCOAT_LEGGINGS, 2);
        
        // -----
        // ITEMS
        // -----
        
        MadEntities.createWarningSign(MadConfig.WARNING_SIGN);

        // --------------------
        // MONSTER PLACER ITEMS
        // --------------------
        MadMod.LOGGER.info("Creating Monster Placer Items");

        MadEntities.createGeneticallyModifiedMonsterPlacer(MadConfig.GENETICALLYMODIFIED_MONSTERPLACER);
        MadEntities.createCombinedGenomeMonsterPlacer(MadConfig.COMBINEDGENOME_MONSTERPLACER);
        MadEntities.createCombinedMemoryMonsterPlacer(MadConfig.COMBINEDMEMORY_MONSTERPLACER);

        // -------
        // RECIPES
        // -------
        MadMod.LOGGER.info("Creating Recipes");

        MadRecipes.createCircuitRecipes();
        MadRecipes.createComponentsRecipes();
        MadRecipes.createWeaponRecipes();
        MadRecipes.createOtherRecipes();

        // -------------------------
        // GENETICALLY MODIFIED MOBS
        // -------------------------
        MadMod.LOGGER.info("Creating Genetically Modified Creatures");

        // Werewolf [Villager + Wolf]
        MadMobs.createGMOMob(MadConfig.GMO_WEREWOLF_METAID, WerewolfMobEntity.class, new NBTTagCompound(), MadMobs.GMO_WEREWOLF_INTERNALNAME, MadMobs.GENOME_WEREWOLF_INTERNALNAME, MadColors.villagerEgg(), MadColors.wolfEgg(), MadGenomes.GENOME_VILLAGER,
                MadGenomes.GENOME_WOLF, MadConfig.GMO_WEREWOLF_COOKTIME);

        // Disgusting Meatcube [Slime + Cow,Pig,Chicken]
        MadFurnaces.createMeatcubeTileEntity(MadConfig.MEATCUBE, MadConfig.GMO_MEATCUBE_METAID, MadColors.slimeEgg(), MadColors.pigEgg(), MadConfig.GMO_MEATCUBE_COOKTIME);

        // Creeper Cow [Creeper + Cow]
        MadMobs.createGMOMob(MadConfig.GMO_CREEPERCOW_METAID, CreeperCowMobEntity.class, new NBTTagCompound(), MadMobs.GMO_CREEPERCOW_INTERNALNAME, MadMobs.GENOME_CREEPERCOW_INTERNALNAME, MadColors.creeperEgg(), MadColors.cowEgg(),
                MadGenomes.GENOME_CREEPER, MadGenomes.GENOME_COW, MadConfig.GMO_CREEPERCOW_COOKTIME);

        // Enderslime [Enderman + Slime]
        MadMobs.createGMOMob(MadConfig.GMO_ENDERSLIME_METAID, EnderslimeMobEntity.class, new NBTTagCompound(), MadMobs.GMO_ENDERSLIME_INTERNALNAME, MadMobs.GENOME_ENDERSLIME_INTERNALNAME, MadColors.endermanEgg(), MadColors.slimeEgg(),
                MadGenomes.GENOME_ENDERMAN, MadGenomes.GENOME_SLIME, MadConfig.GMO_ENDERSLIME_COOKTIME);

        // --------------------------
        // Bart74(bart.74@hotmail.fr)
        // --------------------------

        // Wooly cow [Cow + Sheep]
        MadMobs.createGMOMob(MadConfig.GMO_WOOLYCOW_METAID, WoolyCowMobEntity.class, new NBTTagCompound(), MadMobs.GMO_WOOLYCOW_INTERNALNAME, MadMobs.GENOME_WOOLYCOW_INTERNALNAME, MadColors.cowEgg(), MadColors.sheepEgg(), MadGenomes.GENOME_COW,
                MadGenomes.GENOME_SHEEP, MadConfig.GMO_WOOLYCOW_COOKTIME);

        // ----------------------------------------
        // Deuce_Loosely(captainlunautic@yahoo.com)
        // ----------------------------------------

        // Shoggoth [Slime + Squid]
        MadMobs.createGMOMob(MadConfig.GMO_SHOGGOTH_METAID, ShoggothMobEntity.class, new NBTTagCompound(), MadMobs.GMO_SHOGGOTH_INTERNALNAME, MadMobs.GENOME_SHOGGOTH_INTERNALNAME, MadColors.slimeEgg(), MadColors.squidEgg(), MadGenomes.GENOME_SLIME,
                MadGenomes.GENOME_SQUID, MadConfig.GMO_SHOGGOTH_COOKTIME);

        // ------------------------------------
        // monodemono(coolplanet3000@gmail.com)
        // ------------------------------------

        // The Abomination [Enderman + Spider]
        MadMobs.createGMOMob(MadConfig.GMO_ABOMINATION_METAID, AbominationMobEntity.class, new NBTTagCompound(), MadMobs.GMO_ABOMINATION_INTERNALNAME, MadMobs.GENOME_ABOMINATION_INTERNALNAME, MadColors.endermanEgg(), MadColors.spiderEgg(),
                MadGenomes.GENOME_ENDERMAN, MadGenomes.GENOME_SPIDER, MadConfig.GMO_ABOMINATION_COOKTIME);

        // Add Forge hook for Abomination so we can know when it kills another mob so we can lay an egg.
        MinecraftForge.EVENT_BUS.register(new AbominationMobLivingHandler());

        // ---------------------------------
        // Pyrobrine(haskar.spore@gmail.com)
        // ---------------------------------

        // Wither Skeleton [Enderman + Skeleton]
        MadMobs.createVanillaGMOMob(MadConfig.GMO_WITHERSKELETON_METAID, MadSpawnEggTags.witherSkeleton(), EntityList.getStringFromID(51), MadMobs.GENOME_WITHERSKELETON_INTERNALNAME, MadColors.endermanEgg(), MadColors.skeletonEgg(), MadGenomes.GENOME_ENDERMAN,
                MadGenomes.GENOME_SKELETON, MadConfig.GMO_WITHERSKELETON_COOKTIME);

        // Villager Zombie [Villager + Zombie]
        MadMobs.createVanillaGMOMob(MadConfig.GMO_VILLAGERZOMBIE_METAID, MadSpawnEggTags.villagerZombie(), EntityList.getStringFromID(54), MadMobs.GENOME_VILLAGERZOMBIE_INTERNALNAME, MadColors.villagerEgg(), MadColors.zombieEgg(), MadGenomes.GENOME_VILLAGER,
                MadGenomes.GENOME_ZOMBIE, MadConfig.GMO_VILLAGERZOMBIE_COOKTIME);

        // Skeleton Horse [Horse + Skeleton]
        MadMobs.createVanillaGMOMob(MadConfig.GMO_SKELETONHORSE_METAID, MadSpawnEggTags.horseType(4), EntityList.getStringFromID(100), MadMobs.GENOME_SKELETONHORSE_INTERNALNAME, MadColors.horseEgg(), MadColors.skeletonEgg(), MadGenomes.GENOME_HORSE,
                MadGenomes.GENOME_SKELETON, MadConfig.GMO_SKELETONHORSE_COOKTIME);

        // Zombie Horse [Zombie + Horse]
        MadMobs.createVanillaGMOMob(MadConfig.GMO_ZOMBIEHORSE_METAID, MadSpawnEggTags.horseType(3), EntityList.getStringFromID(100), MadMobs.GENOME_ZOMBIEHORSE_INTERNALNAME, MadColors.horseEgg(), MadColors.zombieEgg(), MadGenomes.GENOME_ZOMBIE,
                MadGenomes.GENOME_HORSE, MadConfig.GMO_ZOMBIEHORSE_COOKTIME);

        // ---------------------------------
        // TheTechnician(tallahlf@gmail.com)
        // ---------------------------------

        // Ender Squid [Enderman + Squid]
        MadMobs.createGMOMob(MadConfig.GMO_ENDERSQUID_METAID, EnderSquidMobEntity.class, new NBTTagCompound(), MadMobs.GMO_ENDERSQUID_INTERNALNAME, MadMobs.GENOME_ENDERSQUID_INTERNALNAME, MadColors.endermanEgg(), MadColors.squidEgg(),
                MadGenomes.GENOME_ENDERMAN, MadGenomes.GENOME_SQUID, MadConfig.GMO_ENDERSQUID_COOKTIME);

        // ---------
        // DONE INIT
        // ---------
        MadMod.LOGGER.info("Finished loading all madness!");
    }
}