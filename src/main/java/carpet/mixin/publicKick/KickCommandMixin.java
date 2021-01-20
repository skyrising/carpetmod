package carpet.mixin.publicKick;

import carpet.CarpetSettings;
import net.minecraft.class_1999;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.KickCommand;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(KickCommand.class)
public abstract class KickCommandMixin extends class_1999 {
    @Override
    public boolean method_29271(MinecraftServer server, CommandSource sender) {
        return CarpetSettings.publicKick || super.method_29271(server, sender);
    }
}
