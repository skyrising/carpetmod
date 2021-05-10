package carpet.mixin.nitwitCrafter;

import carpet.CarpetSettings;
import carpet.helpers.EntityAICrafter;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.inventory.BasicInventory;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin extends PassiveEntity {
    private EntityAICrafter craftingAI;

    @Shadow public abstract int getType();

    @Shadow @Final private BasicInventory inventory;

    public VillagerEntityMixin(World worldIn) {
        super(worldIn);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/World;I)V", at = @At("RETURN"))
    private void onInit(World worldIn, int professionId, CallbackInfo ci) {
        craftingAI = new EntityAICrafter((VillagerEntity) (Object) this);
    }

    @Inject(method = {
        "method_24925",
        "onGrowUp"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/VillagerEntity;getType()I"))
    private void addCraftTask(CallbackInfo ci) {
        if (CarpetSettings.nitwitCrafter && getType() == 5) {
            craftingAI.updateNitwit();
            goalSelector.add(6, craftingAI);
        }
    }

    @Inject(method = "onDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/PassiveEntity;onDeath(Lnet/minecraft/entity/damage/DamageSource;)V"))
    private void onDeath(DamageSource cause, CallbackInfo ci) {
        if (CarpetSettings.nitwitCrafter && getType() == 5 || CarpetSettings.villagerInventoryDropFix) {
            craftingAI.dropInventory();
        }
    }

    @Inject(method = "method_24926", at = @At("HEAD"), cancellable = true)
    private void emptyNitwitBuyingList(CallbackInfo ci) {
        if (CarpetSettings.nitwitCrafter && getType() == 5) {
            ci.cancel();
        }
    }

    @Inject(method = "loot", at = @At("HEAD"), cancellable = true)
    private void updateCraftingEquipment(ItemEntity itemEntity, CallbackInfo ci) {
        if (CarpetSettings.nitwitCrafter && craftingAI != null) {
            if (craftingAI.updateEquipment(itemEntity, inventory)) ci.cancel();
        }
    }
}