package carpet.commands;

import net.minecraft.class_6182;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class CommandLoadChunk extends CommandCarpetBase {
    @Override
    public String method_29275(CommandSource sender) {
        return "Usage: loadchunk <X> <Z>";
    }

    @Override
    public String method_29277() {
        return "loadchunk";
    }

    @Override
    public void method_29272(MinecraftServer server, CommandSource sender, String[] args) throws CommandException {
        if (!command_enabled("commandLoadChunk", sender)) return;

        if (args.length != 2)
        {
            throw new class_6182(method_29275(sender));
        }
        int chunkX = method_28715(args[0]);
        int chunkZ = method_28715(args[1]);
        World world = sender.getEntityWorld();
        world.method_25975(chunkX, chunkZ);
        sender.sendSystemMessage(new LiteralText("Chunk" + chunkX + ", " + chunkZ + " loaded"));
    }

    @Override
    public List<String> method_29273(MinecraftServer server, CommandSource sender, String[] args, @Nullable BlockPos targetPos) {
        int chunkX = sender.getBlockPos().getX() >> 4;
        int chunkZ = sender.getBlockPos().getZ() >> 4;

        if (args.length == 1) {
            return method_28732(args, Integer.toString(chunkX));
        } else if (args.length == 2) {
            return method_28732(args, Integer.toString(chunkZ));
        } else {
            return Collections.emptyList();
        }
    }
}
