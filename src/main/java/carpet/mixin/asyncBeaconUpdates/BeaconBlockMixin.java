package carpet.mixin.asyncBeaconUpdates;

import carpet.CarpetSettings;
import net.minecraft.block.*;
import net.minecraft.client.util.NetworkUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeaconBlock.class)
public abstract class BeaconBlockMixin extends BlockWithEntity {
    protected BeaconBlockMixin(Material material) {
        super(material);
    }

    @Inject(method = "neighborUpdate", at = @At("RETURN"))
    private void asyncBeaconUpdates(BlockState state, World world, BlockPos pos, Block block, BlockPos neighborPos, CallbackInfo ci) {
        if (CarpetSettings.asyncBeaconUpdates && world.isReceivingRedstonePower(pos)) {
            NetworkUtils.field_32712.submit(() -> world.updateNeighborsAlways(pos, this, true));
        }
    }
}
