package carpet;

import carpet.carpetclient.CarpetClientServer;
import carpet.helpers.TickSpeed;
import carpet.logging.LoggerRegistry;
import carpet.network.PluginChannelManager;
import carpet.network.ToggleableChannelHandler;
import carpet.patches.FakeServerPlayerEntity;
import carpet.pubsub.PubSubInfoProvider;
import carpet.pubsub.PubSubMessenger;
import carpet.utils.*;
import carpet.utils.extensions.WaypointContainer;
import carpet.worldedit.WorldEditBridge;
import narcolepticfrog.rsmm.events.TickStartEventDispatcher;
import narcolepticfrog.rsmm.server.RSMMServer;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;

import javax.annotation.Nullable;
import java.io.*;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class CarpetServer {
    private static CarpetServer instance;

    public final MinecraftServer server;

    public PluginChannelManager pluginChannels;
    public RSMMServer rsmmServer;
    public ToggleableChannelHandler rsmmChannel;
    public ToggleableChannelHandler wecuiChannel;

    private CarpetServer(MinecraftServer server) {
        this.server = server;
        pluginChannels = new PluginChannelManager(server);
        pluginChannels.register(new PubSubMessenger(CarpetMod.PUBSUB));
        pluginChannels.register(new CarpetClientServer(server));

        rsmmServer = new RSMMServer(server);
        rsmmChannel = new ToggleableChannelHandler(pluginChannels, rsmmServer.createChannelHandler(), false);
        wecuiChannel = new ToggleableChannelHandler(pluginChannels, WorldEditBridge.createChannelHandler(), false);
    }

    public static CarpetServer init(MinecraftServer server) {
        return instance = new CarpetServer(server);
    }

    public static CarpetServer getInstance() {
        if (instance == null) throw new IllegalStateException("No CarpetServer instance");
        return instance;
    }

    @Nullable
    public static CarpetServer getNullableInstance() {
        return instance;
    }

    public static MinecraftServer getMinecraftServer() {
        return getInstance().server;
    }

    @Nullable
    public static MinecraftServer getNullableMinecraftServer() {
        return instance == null ? null : instance.server;
    }

    public void onServerLoaded() {
        CarpetSettings.reset();
        CarpetSettings.applySettingsFromConf(server);
        LoggerRegistry.initLoggers(server);
        LoggerRegistry.readSaveFile(server);
        WorldEditBridge.onServerLoaded(server);
    }

    public void onLoadAllWorlds() {
        TickingArea.loadConfig(server);
        for (ServerWorld world : server.worlds) {
            int dim = world.dimension.getType().getRawId();
            try {
                ((WaypointContainer) world).setWaypoints(Waypoint.loadWaypoints(world));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            String prefix = "minecraft." + world.dimension.getType().getSaveDir();
            new PubSubInfoProvider<>(CarpetMod.PUBSUB,prefix + ".chunk_loading.dropped_chunks.hash_size",20,
                    () -> ChunkLoading.getCurrentHashSize(world));
            for (SpawnGroup creatureType : SpawnGroup.values()) {
                String mobCapPrefix = prefix + ".mob_cap." + creatureType.name().toLowerCase(Locale.ROOT);
                new PubSubInfoProvider<>(CarpetMod.PUBSUB, mobCapPrefix + ".filled", 20, () -> {
                    Pair<Integer, Integer> mobCap = SpawnReporter.mobcaps.get(dim).get(creatureType);
                    if (mobCap == null) return 0;
                    return mobCap.getLeft();
                });
                new PubSubInfoProvider<>(CarpetMod.PUBSUB, mobCapPrefix + ".total", 20, () -> {
                    Pair<Integer, Integer> mobCap = SpawnReporter.mobcaps.get(dim).get(creatureType);
                    if (mobCap == null) return 0;
                    return mobCap.getRight();
                });
            }
        }
    }

    public void onWorldsSaved() {
        TickingArea.saveConfig(server);
        for (ServerWorld world : server.worlds) {
            try {
                Waypoint.saveWaypoints(world, ((WaypointContainer) world).getWaypoints());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public void tick() {
        TickSpeed.tick(server);
        if (CarpetSettings.redstoneMultimeter)
        {
            TickStartEventDispatcher.dispatchEvent(server.getTicks());
        }
        HUDController.update_hud(server);
        WorldEditBridge.onStartTick();
        CarpetMod.PUBSUB.update(server.getTicks());
    }

    public void playerConnected(ServerPlayerEntity player) {
        pluginChannels.onPlayerConnected(player);
        LoggerRegistry.playerConnected(player);
    }

    public void playerDisconnected(ServerPlayerEntity player) {
        pluginChannels.onPlayerDisconnected(player);
        LoggerRegistry.playerDisconnected(player);
    }

    public Random setRandomSeed(int p_72843_1_, int p_72843_2_, int p_72843_3_) {
        long i = (long) p_72843_1_ * 341873128712L + (long) p_72843_2_ * 132897987541L + server.worlds[0].getLevelProperties().method_28225() + (long) p_72843_3_;
        CarpetMod.rand.setSeed(i);
        return CarpetMod.rand;
    }

    public void loadBots() {
        try
        {
            File settings_file = server.getLevelStorage().method_28330(server.getLevelName(), "bot.conf");
            BufferedReader b = new BufferedReader(new FileReader(settings_file));
            String line = "";
            boolean temp = CarpetSettings.removeFakePlayerSkins;
            CarpetSettings.removeFakePlayerSkins = true;
            while ((line = b.readLine()) != null)
            {
                FakeServerPlayerEntity.create(line, server);
            }
            b.close();
            CarpetSettings.removeFakePlayerSkins = temp;
        }
        catch(FileNotFoundException e)
        {
            System.out.println(e);
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
    }

    public void writeConf(ArrayList<String> names) {
        try {
            File settings_file = server.getLevelStorage().method_28330(server.getLevelName(), "bot.conf");
            if (names != null) {
                FileWriter fw = new FileWriter(settings_file);
                for (String name : names) {
                    fw.write(name + "\n");
                }
                fw.close();
            } else {
                settings_file.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
