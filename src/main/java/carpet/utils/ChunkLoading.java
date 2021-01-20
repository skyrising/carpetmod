package carpet.utils;

import carpet.CarpetSettings;
import carpet.mixin.accessors.ThreadedAnvilChunkStorageAccessor;
import carpet.mixin.accessors.ServerChunkManagerAccessor;
import net.minecraft.class_6380;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColumnPos;
import net.minecraft.world.chunk.WorldChunk;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.zip.DeflaterOutputStream;

import com.google.common.collect.Lists;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.io.output.NullOutputStream;

public class ChunkLoading
{
    public static final ThreadLocal<ServerPlayerEntity> INITIAL_PLAYER_FOR_CHUNK_MAP_ENTRY = new ThreadLocal<>();
    public static final LongSet droppedChunksSet_new = new LongOpenHashSet();
    public static void queueUnload113(ServerWorld world, ServerChunkManager chunkproviderserver, WorldChunk chunkIn)
    {
        if (world.dimension.method_27511(chunkIn.field_25365, chunkIn.field_25366))
        {
            droppedChunksSet_new.add(Long.valueOf(ColumnPos.method_25891(chunkIn.field_25365, chunkIn.field_25366)));
            chunkIn.field_25367 = true;
        }
    }

    public static List<String> test_save_chunks(ServerWorld server, BlockPos pos, boolean verbose)
    {

        ServerChunkManager chunkproviderserver = server.getChunkManager();

        if (chunkproviderserver.method_33457())
        {

            chunkproviderserver.method_33450(true);

            class_6380 pcm = server.getRaidManager();

            for (WorldChunk chunk : Lists.newArrayList(chunkproviderserver.method_33445()))
            {
                if (chunk != null && !pcm.method_33579(chunk.field_25365, chunk.field_25366))
                {
                    chunkproviderserver.method_33448(chunk);
                }
            }
            return ChunkLoading.tick_reportive_no_action(server, pos, verbose);
        }
        List<String> rep = new ArrayList<String>();
        rep.add("Saving is disabled on the server");
        return rep;
    }


    public static List<String> test_save_chunks_113(ServerWorld server, BlockPos pos, boolean verbose)
    {

        ServerChunkManager chunkproviderserver = server.getChunkManager();

        if (chunkproviderserver.method_33457())
        {

            chunkproviderserver.method_33450(true);

            class_6380 pcm = server.getRaidManager();

            for (WorldChunk chunk : Lists.newArrayList(chunkproviderserver.method_33445()))
            {
                if (chunk != null && !pcm.method_33579(chunk.field_25365, chunk.field_25366))
                {
                    queueUnload113(server, chunkproviderserver, chunk);
                }
            }
            List <String> rep = ChunkLoading.tick_reportive_no_action_113(server, pos, verbose);
            return rep;
        }
        List<String> rep = new ArrayList<String>();
        rep.add("Saving is disabled on the server");
        return rep;
    }





    public static int getCurrentHashSize(ServerWorld server)
    {
        ServerChunkManager chunkproviderserver = server.getChunkManager();
        try
        {
            Set<Long> droppedChunks = ((ServerChunkManagerAccessor) chunkproviderserver).getDroppedChunks();
            Field field = droppedChunks.getClass().getDeclaredField("map");
            field.setAccessible(true);
            HashMap<?, ?> map = (HashMap<?,?>) field.get(droppedChunks);
            field = map.getClass().getDeclaredField("table");
            field.setAccessible(true);
            Object [] table = (Object [])field.get(map);
            if (table==null)
                return 2;
            return table.length;
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            e.printStackTrace();
        }
        return -1;
    }
    public static int getCurrentHashSize_113()
    {
        try
        {
            Field field = droppedChunksSet_new.getClass().getDeclaredField("n");
            field.setAccessible(true);
            int n = field.getInt(droppedChunksSet_new);
            return n;
        }
        catch (NoSuchFieldException e)
        {
            e.printStackTrace();
        } catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        return -1;
    }


    public static int getChunkOrder(ColumnPos chpos, int hashsize)
    {
        //return HashMap_hash(Long.hashCode(ChunkPos.asLong(chpos.chunkXPos, chpos.chunkZPos)));
        try
        {
            Method method = HashMap.class.getDeclaredMethod("hash", Object.class);
            method.setAccessible(true);
            return (Integer) method.invoke(null, Long.hashCode(ColumnPos.method_25891(chpos.x, chpos.z))) & (hashsize-1);
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e)
        {
            CarpetSettings.LOG.error("You broke java");
            return -1;
        }
    }
    public static long get_chunk_order_113(ColumnPos chpos, int hashsize)
    {
        return (HashCommon.mix(ColumnPos.method_25891(chpos.x, chpos.z))) & (hashsize-1L);
    }

    public static List<String> check_unload_order(ServerWorld server, BlockPos pos, BlockPos pos1)
    {
        List<String> rep = new ArrayList<>();
        int size = getCurrentHashSize(server);
        if (pos1 == null)
        {
            ColumnPos chpos = new ColumnPos(pos);
            int o = getChunkOrder(chpos, size);
            rep.add("Chunks order of "+chpos+" is "+o+" / "+size);
            return rep;
        }
        ColumnPos chpos1 = new ColumnPos(pos);
        ColumnPos chpos2 = new ColumnPos(pos1);
        int minx = (chpos1.x < chpos2.x) ? chpos1.x : chpos2.x;
        int maxx = (chpos1.x > chpos2.x) ? chpos1.x : chpos2.x;
        int minz = (chpos1.z < chpos2.z) ? chpos1.z : chpos2.z;
        int maxz = (chpos1.z > chpos2.z) ? chpos1.z : chpos2.z;
        HashMap<Integer,Integer> stat = new HashMap<>();
        int total = 0;
        for (int chposx = minx; chposx <= maxx; chposx++)
        {
            for (int chposz = minz; chposz <= maxz; chposz++)
            {
                int o1 = getChunkOrder(new ColumnPos(chposx, chposz),size);
                int count = stat.containsKey(o1) ? stat.get(o1) : 0;
                stat.put(o1, count + 1);
                total ++;
            }
        }
        rep.add("Counts of chunks with specific unload order / "+size+" ("+total+" total)");
        SortedSet<Integer> keys = new TreeSet<>(stat.keySet());
        for (int key : keys)
        {
            rep.add(" - order "+key+": "+stat.get(key));

        }
        return rep;

    }
    public static List<String> check_unload_order_13(ServerWorld server, BlockPos pos, BlockPos pos1)
    {
        List<String> rep = new ArrayList<>();
        int size = getCurrentHashSize(server);
        if (pos1 == null)
        {
            ColumnPos chpos = new ColumnPos(pos);
            int o = (int)get_chunk_order_113(chpos, size);
            int olong = (int)get_chunk_order_113(chpos, 1<<20);
            rep.add("Chunks order of "+chpos+" is "+o+" / "+size+", or part of "+Integer.toBinaryString(olong));
            return rep;
        }
        ColumnPos chpos1 = new ColumnPos(pos);
        ColumnPos chpos2 = new ColumnPos(pos1);
        int minx = (chpos1.x < chpos2.x) ? chpos1.x : chpos2.x;
        int maxx = (chpos1.x > chpos2.x) ? chpos1.x : chpos2.x;
        int minz = (chpos1.z < chpos2.z) ? chpos1.z : chpos2.z;
        int maxz = (chpos1.z > chpos2.z) ? chpos1.z : chpos2.z;
        HashMap<Integer,Integer> stat = new HashMap<>();
        int total = 0;
        for (int chposx = minx; chposx <= maxx; chposx++)
        {
            for (int chposz = minz; chposz <= maxz; chposz++)
            {
                int o1 = (int)get_chunk_order_113(new ColumnPos(chposx, chposz),size);
                int count = stat.containsKey(o1) ? stat.get(o1) : 0;
                stat.put(o1, count + 1);
                total ++;
            }
        }
        rep.add("Counts of chunks with specific unload order / "+size+" ("+total+" total)");
        SortedSet<Integer> keys = new TreeSet<>(stat.keySet());
        for (int key : keys)
        {
            rep.add(" - order "+key+": "+stat.get(key));

        }
        return rep;

    }

    public static List<String> protect_13(ServerWorld server, BlockPos pos, BlockPos pos1, String protect)
    {
        int size = getCurrentHashSize(server);
        String rest = protect.replaceAll("[\\D]", "");
        if (!(rest.equals("")))
        {
            size = Integer.parseInt(rest);
            size = HashCommon.nextPowerOfTwo(size-1);
        }
        if (size < 256)
            size = 256;
        List<String> rep = new ArrayList<>();
        if (pos1 == null)
        {
            pos1 = pos;
        }
        ColumnPos chpos1 = new ColumnPos(pos);
        ColumnPos chpos2 = new ColumnPos(pos1);
        int minx = (chpos1.x < chpos2.x) ? chpos1.x : chpos2.x;
        int maxx = (chpos1.x > chpos2.x) ? chpos1.x : chpos2.x;
        int minz = (chpos1.z < chpos2.z) ? chpos1.z : chpos2.z;
        int maxz = (chpos1.z > chpos2.z) ? chpos1.z : chpos2.z;
        int lenx = maxx-minx+1;
        int lenz = maxz-minz+1;
        HashMap<Integer,Integer> stat = new HashMap<>();
        int total = 0;
        for (int chposx = minx; chposx <= maxx; chposx++)
        {
            for (int chposz = minz; chposz <= maxz; chposz++)
            {
                int o1 = (int)get_chunk_order_113(new ColumnPos(chposx, chposz),size);
                int count = stat.containsKey(o1) ? stat.get(o1) : 0;
                stat.put(o1, count + 1);
                total ++;
            }
        }
        rep.add("Counts of chunks with specific unload order out of "+size+" ("+total+" total chunks to protect)");
        SortedSet<Integer> keys = new TreeSet<>(stat.keySet());
        String chunklist = "";
        int order_to_protect = 1;
        for (int key : keys)
        {
            chunklist += String.format("%d:%d ",key,stat.get(key));
            order_to_protect = key;
        }
        rep.add(chunklist);

        int best_config_chunks = Integer.MAX_VALUE;
        int best_config_minx = 0;
        int best_config_maxx = 0;
        int best_config_minz = 0;
        int best_config_maxz = 0;
        int protect_limit = (int)(0.75*size);
        for (int xdir = -1; xdir <= 1; xdir+= 2)
        {
            for (int zdir = -1; zdir <= 1; zdir += 2)
            {

                for (int ex = 1; ex < protect_limit; ex++)
                {
                    for (int ez = 1; ez < protect_limit; ez++)
                    {
                        if ((lenx+ex)*(lenz+ez) > protect_limit) break;
                        if ((lenx+ex)*(lenz+ez) > best_config_chunks) break;
                        int cminx = xdir<0?minx-ex:minx;
                        int cminz = zdir<0?minz-ez:minz;
                        int cmaxx = xdir<0?maxx:maxx+ex;
                        int cmaxz = zdir<0?maxz:maxz+ez;
                        int protecting_chunks = 0;
                        for (int cx = cminx; cx <= cmaxx; cx++)
                        {
                            for (int cz = cminz; cz <= cmaxz; cz++)
                            {
                                int order = (int)get_chunk_order_113(new ColumnPos(cx, cz),size);
                                if (order > order_to_protect)
                                {
                                    protecting_chunks ++;
                                }
                            }
                        }
                        if (protecting_chunks > 100)
                        {
                            int lx = cmaxx - cminx+1;
                            int lz = cmaxz - cminz+1;
                            if (lx*lz<best_config_chunks)
                            {
                                best_config_chunks = lx*lz;
                                best_config_minx = cminx;
                                best_config_maxx = cmaxx;
                                best_config_minz = cminz;
                                best_config_maxz = cmaxz;
                                break;
                            }

                        }
                    }
                }
            }
        }
        if (best_config_chunks != Integer.MAX_VALUE)
        {
            rep.add("you can protect this configuration with "+best_config_chunks+" chunks");
            rep.add("    from block: "+(best_config_minx<<4)+", "+(best_config_minz<<4)+" to "+((best_config_maxx<<4)+15)+", "+((best_config_maxz<<4)+15));
        }
        else
        {
            rep.add("You can't protect this configuration with less than "+protect_limit+" chunks around");
        }
        return rep;
    }


    public static String stringify_chunk_id(ServerChunkManager provider, int index, Long olong, int size)
    {
        WorldChunk chunk = ((ServerChunkManagerAccessor) provider).getLoadedChunksMap().get(olong);

        return String.format(" - %4d: (%d, %d) at X %d, Z %d (order: %d / %d)",
                index+1,
                chunk.field_25365, chunk.field_25366,
                chunk.field_25365 * 16+7, chunk.field_25366*16+7,
                ChunkLoading.getChunkOrder(new ColumnPos(chunk.field_25365, chunk.field_25366), size),
                size
        );
    }

    public static String stringify_chunk_id_113(ServerChunkManager provider, int index, Long olong, int size)
    {
        WorldChunk chunk = ((ServerChunkManagerAccessor) provider).getLoadedChunksMap().get(olong);

        return String.format(" - %4d: (%d, %d) at X %d, Z %d (order: %d / %d)",
                index+1,
                chunk.field_25365, chunk.field_25366,
                chunk.field_25365 * 16+7, chunk.field_25366*16+7,
                ChunkLoading.get_chunk_order_113(new ColumnPos(chunk.field_25365, chunk.field_25366), size),
                size
        );
    }

    public static List<String> tick_reportive_no_action(ServerWorld world, BlockPos pos, boolean verbose)
    {
        ServerChunkManager provider = world.getChunkManager();
        List<String> rep = new ArrayList<>();
        int test_chunk_xpos = 0;
        int test_chunk_zpos = 0;
        if (pos != null)
        {
            test_chunk_xpos = pos.getX() >> 4;
            test_chunk_zpos = pos.getZ() >> 4;
        }
        int current_size = ChunkLoading.getCurrentHashSize(world);
        if (!world.savingDisabled)
        {
            Set<Long> droppedChunks = ((ServerChunkManagerAccessor) provider).getDroppedChunks();
            if (!droppedChunks.isEmpty())
            {
                Iterator<Long> iterator = droppedChunks.iterator();
                List<Long> chunks_ids_order = new ArrayList<>();
                int selected_chunk = -1;
                int iti = 0;
                int i = 0;
                for (i = 0; iterator.hasNext(); iterator.remove())
                {
                    Long olong = iterator.next();
                    WorldChunk chunk = ((ServerChunkManagerAccessor) provider).getLoadedChunksMap().get(olong);

                    if (chunk != null && chunk.field_25367)
                    {
                        if ( pos != null && chunk.field_25365 == test_chunk_xpos && chunk.field_25366 == test_chunk_zpos) selected_chunk = i;
                        chunks_ids_order.add(olong);
                        ++i;
                    }
                    ++iti;
                }
                if (i != iti)
                {
                    rep.add("There were some ineligible chunks to be unloaded,");
                    rep.add("so the actual 100 chunk mark might not be accurate");
                }
                int total = chunks_ids_order.size();
                List<Integer> llll = Arrays.asList(0, 1,2, -1, 97, 98, 99, -2, 100, 101, 102, -1,
                        total-3, total-2, total-1);
                if (total <= 100)
                {
                    rep.add(String.format("There is only %d chunks to unload, all will be unloaded", total));
                    llll = (total > 5)?Arrays.asList(0, 1, -1, total - 2, total - 1):Arrays.asList(-2);
                }
                if (verbose)
                {
                    for (int iii=0; iii<chunks_ids_order.size(); iii++)
                    {
                        rep.add(stringify_chunk_id(provider, iii, chunks_ids_order.get(iii), current_size));
                    }
                }
                else
                {
                    for (int idx: llll)
                    {
                        if (idx < 0)
                        {
                            if (idx == -1)
                            {
                                rep.add("    ....");
                            }
                            else
                            {
                                rep.add("--------");
                            }
                        }
                        else
                        {
                            if (idx >= total)
                            {
                                continue;
                            }
                            rep.add(stringify_chunk_id(provider, idx, chunks_ids_order.get(idx), current_size));
                        }
                    }
                }
                if (pos != null)
                {

                    if (selected_chunk == -1)
                    {
                        rep.add("Selected chunk was not marked for unloading");
                    }
                    else
                    {
                        rep.add(String.format("Selected chunk was %d on the list", selected_chunk+1) );
                    }
                }
            }
            else
            {
                rep.add("There are no chunks to get unloaded");
            }
        }
        else
        {
            rep.add("Level Saving is disabled.");
        }
        return rep;
    }


    public static List<String> tick_reportive_no_action_113(ServerWorld world, BlockPos pos, boolean verbose)
    {
        ServerChunkManager provider = world.getChunkManager();
        List<String> rep = new ArrayList<>();
        int test_chunk_xpos = 0;
        int test_chunk_zpos = 0;
        if (pos != null)
        {
            test_chunk_xpos = pos.getX() >> 4;
            test_chunk_zpos = pos.getZ() >> 4;
        }
        if (!world.savingDisabled)
        {
            if (!droppedChunksSet_new.isEmpty())
            {
                Iterator<Long> iterator = droppedChunksSet_new.iterator();
                List<Long> chunks_ids_order = new ArrayList<>();
                Map<Long,Integer> chunk_to_len = new HashMap<>();
                int selected_chunk = -1;
                int iti = 0;
                int i = 0;
                int current_size = ChunkLoading.getCurrentHashSize_113();
                for (i = 0; iterator.hasNext(); iterator.remove())
                {

                    Long olong = iterator.next();
                    WorldChunk chunk = ((ServerChunkManagerAccessor) provider).getLoadedChunksMap().get(olong);
                    ((ServerChunkManagerAccessor) provider).getDroppedChunks().remove(olong);

                    if (chunk != null && chunk.field_25367)
                    {
                        if ( pos != null && chunk.field_25365 == test_chunk_xpos && chunk.field_25366 == test_chunk_zpos) selected_chunk = i;
                        chunks_ids_order.add(olong);
                        chunk_to_len.put(olong, current_size);
                        current_size = ChunkLoading.getCurrentHashSize_113();
                        ++i;
                    }
                    ++iti;
                }
                if (i != iti)
                {
                    rep.add("There were some ineligible chunks to be unloaded,");
                    rep.add("so the actual 100 chunk mark might not be accurate");
                }
                int total = chunks_ids_order.size();
                List<Integer> llll = Arrays.asList(0, 1,2, -1, 97, 98, 99, -2, 100, 101, 102, -1,
                        total-3, total-2, total-1);
                if (total <= 100)
                {
                    rep.add(String.format("There is only %d chunks to unload, all will be unloaded", total));
                    llll = (total > 5)?Arrays.asList(0, 1, -1,total-2, total -1 ):Arrays.asList(-2);
                }
                if (verbose)
                {
                    for (int iii=0; iii<chunks_ids_order.size(); iii++)
                    {
                        rep.add(stringify_chunk_id_113(provider, iii, chunks_ids_order.get(iii), chunk_to_len.get(chunks_ids_order.get(iii))));
                    }
                }
                else
                {
                    for (int idx: llll)
                    {
                        if (idx < 0)
                        {
                            if (idx == -1)
                            {
                                rep.add("    ....");
                            }
                            else
                            {
                                rep.add("--------");
                            }
                        }
                        else
                        {
                            if (idx >= total)
                            {
                                continue;
                            }
                            rep.add(stringify_chunk_id_113(provider, idx, chunks_ids_order.get(idx), chunk_to_len.get(chunks_ids_order.get(idx))));
                        }
                    }
                }
                if (pos != null)
                {

                    if (selected_chunk == -1)
                    {
                        rep.add("Selected chunk was not marked for unloading");
                    }
                    else
                    {
                        rep.add(String.format("Selected chunk was %d on the list", selected_chunk+1) );
                        rep.add(stringify_chunk_id_113(provider, selected_chunk, chunks_ids_order.get(selected_chunk), chunk_to_len.get(chunks_ids_order.get(selected_chunk))));
                    }
                }
            }
            else
            {
                rep.add("There are no chunks to get unloaded");
            }
        }
        else
        {
            rep.add("Level Saving is disabled.");
        }
        return rep;
    }

    public static int getSavedChunkSize(WorldChunk chunk)
    {
        CompoundTag chunkTag = new CompoundTag();
        CompoundTag levelTag = new CompoundTag();
        chunkTag.put("Level", levelTag);
        chunkTag.putInt("DataVersion", 1343);
        ((ThreadedAnvilChunkStorageAccessor) getChunkLoader(chunk)).invokeWriteChunkToNBT(chunk, chunk.getWorld(), levelTag);
        CountingOutputStream counter = new CountingOutputStream(NullOutputStream.NULL_OUTPUT_STREAM);
        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new DeflaterOutputStream(counter)));
            NbtIo.write(chunkTag, out);
            out.flush();
            out.close();
        }
        catch (IOException ignore) {}
        return counter.getCount();
    }

    public static ThreadedAnvilChunkStorage getChunkLoader(WorldChunk chunk) {
        return (ThreadedAnvilChunkStorage) ((ServerChunkManagerAccessor) chunk.getWorld().getChunkManager()).getChunkLoader();
    }
}
