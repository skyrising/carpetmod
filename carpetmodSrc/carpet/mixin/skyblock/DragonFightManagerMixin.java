package carpet.mixin.skyblock;

import carpet.CarpetSettings;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.end.DragonFightManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DragonFightManager.class)
public class DragonFightManagerMixin {
    @Shadow private BlockPos exitPortalLocation;

    @Inject(method = "generatePortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/feature/WorldGenEndPodium;generate(Lnet/minecraft/world/World;Ljava/util/Random;Lnet/minecraft/util/math/BlockPos;)Z"))
    private void fixExitPortalLocation(boolean active, CallbackInfo ci) {
        // Fix for the end portal somehow spawning at y = -2 when spawning the first time in skyblock CARPET-XCOM
        if(CarpetSettings.skyblock && exitPortalLocation.getY() <= 0) {
            exitPortalLocation = new BlockPos(exitPortalLocation.getX(), 63, exitPortalLocation.getZ());
        }
    }
}
