package carpet.helpers;

import carpet.CarpetMod;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class AutoCraftingDropperHelper
{
    public static void spawnItemStack(World worldIn, double x, double y, double z, ItemStack stack)
    {
        while (!stack.isEmpty())
        {
            ItemEntity itemEntity = new ItemEntity(worldIn, x, y, z, stack.split(CarpetMod.rand.nextInt(21) + 10));
            itemEntity.velocityX = (CarpetMod.rand.nextDouble() - CarpetMod.rand.nextDouble()) * 0.05;
            itemEntity.velocityY = CarpetMod.rand.nextDouble() * 0.05;
            itemEntity.velocityZ = (CarpetMod.rand.nextDouble() - CarpetMod.rand.nextDouble()) * 0.05;
            worldIn.spawnEntity(itemEntity);
        }
    }
}
