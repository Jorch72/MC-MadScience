package madscience;

import madscience.container.SlotContainerTypeEnum;
import madscience.product.TileEntityFactoryProduct;
import madscience.tile.TileEntityPrefab;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class DataDuplicator extends TileEntityPrefab
{
    public DataDuplicator()
    {
        super();
    }
    
    public DataDuplicator(TileEntityFactoryProduct registeredMachine)
    {
        super(registeredMachine);
    }

    public DataDuplicator(String machineName)
    {
        super(machineName);
    }

    @Override
    public boolean canSmelt()
    {
        super.canSmelt();
        
        // Check if we have redstone power applied to us.
        if (!this.isRedstonePowered())
        {
            return false;
        }
        
        // Check if both input slots for reel to copy and empty reel to copy onto.
        if (this.getStackInSlotByType(SlotContainerTypeEnum.INPUT_INGREDIENT1) == null || this.getStackInSlotByType(SlotContainerTypeEnum.INPUT_INGREDIENT2) == null)
        {
            return false;
        }

        // Check if output slots are empty and ready to be filled with.
        if (this.getStackInSlotByType(SlotContainerTypeEnum.OUTPUT_RESULT1) == null)
        {
            return true;
        }

        // Check if input slot 1 matches output slot 1.
        if (this.getStackInSlotByType(SlotContainerTypeEnum.INPUT_INGREDIENT1).isItemEqual(this.getStackInSlotByType(SlotContainerTypeEnum.OUTPUT_RESULT1)))
        {
            return false;
        }

        return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);
    }

    @Override
    public void smeltItem()
    {
        super.smeltItem();
        
        if (this.getStackInSlotByType(SlotContainerTypeEnum.INPUT_INGREDIENT2) != null &&
                this.getStackInSlotByType(SlotContainerTypeEnum.INPUT_INGREDIENT2).isItemDamaged() &&
                this.getStackInSlotByType(SlotContainerTypeEnum.OUTPUT_RESULT1) == null)
        {
            if (this.getStackInSlotByType(SlotContainerTypeEnum.INPUT_INGREDIENT2).isItemDamaged())
            {
                int currentGenomeStatus = this.getStackInSlotByType(SlotContainerTypeEnum.INPUT_INGREDIENT2).getItemDamage();
                this.getStackInSlotByType(SlotContainerTypeEnum.INPUT_INGREDIENT2).setItemDamage(--currentGenomeStatus);
            }

            // Copy B -> A
            if (this.getStackInSlotByType(SlotContainerTypeEnum.OUTPUT_RESULT1) == null &&
                    !this.getStackInSlotByType(SlotContainerTypeEnum.INPUT_INGREDIENT2).isItemDamaged())
            {
                this.setInventorySlotContentsByType(SlotContainerTypeEnum.OUTPUT_RESULT1, this.getStackInSlotByType(SlotContainerTypeEnum.INPUT_INGREDIENT2).copy());

                --this.getStackInSlotByType(SlotContainerTypeEnum.INPUT_INGREDIENT2).stackSize;
                if (this.getStackInSlotByType(SlotContainerTypeEnum.INPUT_INGREDIENT2).stackSize <= 0)
                {
                    this.setInventorySlotContentsByType(SlotContainerTypeEnum.INPUT_INGREDIENT2, null);
                }
            }

            --this.getStackInSlotByType(SlotContainerTypeEnum.INPUT_INGREDIENT1).stackSize;
            if (this.getStackInSlotByType(SlotContainerTypeEnum.INPUT_INGREDIENT1).stackSize <= 0)
            {
                this.setInventorySlotContentsByType(SlotContainerTypeEnum.INPUT_INGREDIENT1, null);
            }

            return;
        }

        // Copy A -> B
        if (this.getStackInSlotByType(SlotContainerTypeEnum.INPUT_INGREDIENT2) != null &&
                this.getStackInSlotByType(SlotContainerTypeEnum.OUTPUT_RESULT1) == null)
        {
            if (this.getStackInSlotByType(SlotContainerTypeEnum.OUTPUT_RESULT1) == null)
            {
                this.setInventorySlotContentsByType(SlotContainerTypeEnum.OUTPUT_RESULT1, this.getStackInSlotByType(SlotContainerTypeEnum.INPUT_INGREDIENT1).copy());
            }
            else if (this.getStackInSlotByType(SlotContainerTypeEnum.OUTPUT_RESULT1).isItemEqual(this.getStackInSlotByType(SlotContainerTypeEnum.INPUT_INGREDIENT1)))
            {
                this.getStackInSlotByType(SlotContainerTypeEnum.OUTPUT_RESULT1).stackSize += this.getStackInSlotByType(SlotContainerTypeEnum.INPUT_INGREDIENT1).stackSize;
            }

            --this.getStackInSlotByType(SlotContainerTypeEnum.INPUT_INGREDIENT2).stackSize;
            if (this.getStackInSlotByType(SlotContainerTypeEnum.INPUT_INGREDIENT2).stackSize <= 0)
            {
                this.setInventorySlotContentsByType(SlotContainerTypeEnum.INPUT_INGREDIENT2, null);
            }
        }
    }

    @Override
    public void updateAnimation()
    {
        super.updateAnimation();
        
        // Active state has many textures based on item cook progress.
        if (isPowered() && canSmelt())
        {
            if (this.getAnimationCurrentFrame() <= 9 && worldObj.getWorldTime() % 5L == 0L)
            {
                // Load this texture onto the entity.
                this.setTextureRenderedOnModel("models/" + this.getMachineInternalName() + "/work_" + this.getAnimationCurrentFrame() + ".png");

                // Update animation frame.
                this.incrementAnimationCurrentFrame();
            }
            else if (this.getAnimationCurrentFrame() >= 10)
            {
                // Check if we have exceeded the ceiling and need to reset.
                this.setAnimationCurrentFrame(0);
            }
        }
        else if (isPowered() && !canSmelt())
        {
            // Idle state single texture.
            this.setTextureRenderedOnModel("models/" + this.getMachineInternalName() + "/idle.png");
        }
        else
        {
            // We are not powered or working.
            this.setTextureRenderedOnModel("models/" + this.getMachineInternalName() + "/off.png");
        }
    }

    @Override
    public void updateEntity()
    {
        // Important to call the class below us!
        super.updateEntity();

        if (this.isPowered() && this.canSmelt())
        {
            // Decrease to amount of energy this item has on client and server.
            this.consumeInternalEnergy(this.getEnergyConsumeRate());
        }

        // Server side processing for furnace.
        if (!this.worldObj.isRemote)
        {
            // First tick for new item being cooked in furnace.
            if (this.getProgressValue() == 0 && this.canSmelt() && this.isPowered())
            {
                // New item pulled from cooking stack to be processed, check how long this item will take to cook.
                this.setProgressMaximum(2600);

                // Increments the timer to kickstart the cooking loop.
                this.incrementProgressValue();
            }
            else if (this.getProgressValue() > 0 && this.canSmelt() && this.isPowered())
            {
                // Increments the timer to kickstart the cooking loop.
                this.incrementProgressValue();

                // Check if furnace has exceeded total amount of time to cook.
                if (this.getProgressValue() >= this.getProgressMaximum())
                {
                    // Convert one item into another via 'cooking' process.
                    this.setProgressValue(0);
                    this.smeltItem();
                    this.setInventoriesChanged();
                }
            }
            else
            {
                // Reset loop, prepare for next item or closure.
                this.setProgressValue(0);
            }
        }
    }

    @Override
    public void updateSound()
    {
        super.updateSound();
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);
    }

    @Override
    public void initiate()
    {
        super.initiate();
    }

    @Override
    public void onBlockRightClick(World world, int x, int y, int z, EntityPlayer par5EntityPlayer)
    {
        super.onBlockRightClick(world, x, y, z, par5EntityPlayer);
    }

    @Override
    public void onBlockLeftClick(World world, int x, int y, int z, EntityPlayer player)
    {
        super.onBlockLeftClick(world, x, y, z, player);
    }
}
