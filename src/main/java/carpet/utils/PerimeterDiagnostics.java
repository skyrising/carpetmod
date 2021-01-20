package carpet.utils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.class_2278;
import net.minecraft.class_4818;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.List;

public class PerimeterDiagnostics
{
    public static class Result
    {
        public int liquid;
        public int ground;
        public int specific;
        public List<BlockPos> samples;
        Result()
        {
            samples = new ArrayList<>();
        }
    }
    private Biome.SpawnEntry sle;
    private ServerWorld worldServer;
    private EntityCategory ctype;
    private MobEntity el;
    private PerimeterDiagnostics(ServerWorld server, EntityCategory ctype, MobEntity el)
    {
        this.sle = null;
        this.worldServer = server;
        this.ctype = ctype;
        this.el = el;
    }

    public static Result countSpots(ServerWorld worldserver, BlockPos epos, MobEntity el)
    {
        BlockPos pos;
        //List<BlockPos> samples = new ArrayList<BlockPos>();
        //if (el != null) CarpetSettings.LOG.error(String.format("Got %s to check",el.toString()));
        int eY = epos.getY();
        int eX = epos.getX();
        int eZ = epos.getZ();
        Result result = new Result();

        //int ground_spawns = 0;
        //int liquid_spawns = 0;
        //int specific_spawns = 0;
        boolean add_water = false;
        boolean add_ground = false;
        EntityCategory ctype = null;

        if (el != null)
        {
            if (el instanceof WaterCreatureEntity)
            {
                add_water = true;
                ctype = EntityCategory.WATER_CREATURE;
            }
            else if (el instanceof AnimalEntity)
            {
                add_ground = true;
                ctype = EntityCategory.CREATURE;
            }
            else if (el instanceof class_4818)
            {
                add_ground = true;
                ctype = EntityCategory.MONSTER;
            }
            else if (el instanceof AmbientEntity)
            {
                ctype = EntityCategory.AMBIENT;
            }
        }
        PerimeterDiagnostics diagnostic = new PerimeterDiagnostics(worldserver,ctype,el);
        for (int x = -128; x <= 128; ++x)
        {
            for (int z = -128; z <= 128; ++z)
            {
                if (x*x + z*z > 128*128) // cut out a cyllinder first
                {
                    continue;
                }
                for (int y= 0; y < 256; ++y)
                {
                    if ((Math.abs(y-eY)>128) )
                    {
                        continue;
                    }
                    int distsq = (x)*(x)+(eY-y)*(eY-y)+(z)*(z);
                    if (distsq > 128*128 || distsq < 24*24)
                    {
                        continue;
                    }
                    pos = new BlockPos(eX+x, y, eZ+z);

                    BlockState iblockstate = worldserver.getBlockState(pos);
                    BlockState iblockstate_down = worldserver.getBlockState(pos.method_31898());
                    BlockState iblockstate_up = worldserver.getBlockState(pos.up());

                    if ( iblockstate.getMaterial() == Material.WATER && iblockstate_down.getMaterial() == Material.WATER && !iblockstate_up.method_27207())
                    {
                        result.liquid++;
                        if (add_water && diagnostic.check_entity_spawn(pos))
                        {
                            result.specific++;
                            if (result.samples.size() < 10)
                            {
                                result.samples.add(pos);
                            }
                        }
                    }
                    else
                    {
                        if (iblockstate_down.method_27211())
                        {
                            Block block = iblockstate_down.getBlock();
                            boolean flag = block != Blocks.BEDROCK && block != Blocks.BARRIER;
                            if( flag && SpawnHelper.method_26211(iblockstate) && SpawnHelper.method_26211(iblockstate_up))
                            {
                                result.ground ++;
                                if (add_ground && diagnostic.check_entity_spawn(pos))
                                {
                                    result.specific++;
                                    if (result.samples.size() < 10)
                                    {
                                        result.samples.add(pos);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        //ashMap<String,Integer> result= new HashMap<>();
        //result.put("Potential in-water spawning spaces", liquid_spawns);
        //result.put("Potential on-ground spawning spaces", ground_spawns);
        //if (el != null) result.put(String.format("%s spawning spaces",el.getDisplayName().getUnformattedText()),specific_spawns);
        return result;
    }


    private boolean check_entity_spawn(BlockPos pos)
    {
        if (sle == null || !worldServer.method_33469(ctype, sle, pos))
        {
            sle = null;
            for (Biome.SpawnEntry sle: worldServer.getChunkManager().method_33449(ctype, pos))
            {
                if (el.getClass() == sle.field_23703)
                {
                    this.sle = sle;
                    break;
                }
            }
            if (sle == null || !worldServer.method_33469(ctype, sle, pos))
            {
                return false;
            }
        }

        if (SpawnHelper.method_26213(class_2278.method_34819(sle.field_23703), worldServer, pos))
        {
            el.refreshPositionAndAngles((float)pos.getX() + 0.5F, (float)pos.getY(), (float)pos.getZ()+0.5F, 0.0F, 0.0F);
            return el.method_34765() && el.method_34766();
        }
        return false;
    }
}
