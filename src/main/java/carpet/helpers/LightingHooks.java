package carpet.helpers;

/*
 * Copyright PhiPro
 */

import javax.annotation.Nullable;
import carpet.mixin.accessors.DirectionAccessor;
import carpet.utils.extensions.NewLightChunk;
import net.minecraft.class_5305;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.PooledMutable;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LightingHooks
{
    private static final LightType[] ENUM_SKY_BLOCK_VALUES = LightType.values();
    private static final AxisDirection[] ENUM_AXIS_DIRECTION_VALUES = AxisDirection.values();

    public static final int FLAG_COUNT = 32; //2 light types * 4 directions * 2 halves * (inwards + outwards)

    public static final int CHUNK_COORD_OVERFLOW_MASK = -1 << 4;
    
    private static final Logger LOGGER = LogManager.getLogger();

    public static void onLoad(final World world, final WorldChunk chunk)
    {
        initChunkLighting(world, chunk);
        initNeighborLight(world, chunk);
        scheduleRelightChecksForChunkBoundaries(world, chunk);
    }

    /*public static void writeLightData(final Chunk chunk, final NBTTagCompound nbt)
    {
        writeNeighborInitsToNBT(chunk, nbt);
        writeNeighborLightChecksToNBT(chunk, nbt);
    }

    public static void readLightData(final Chunk chunk, final NBTTagCompound nbt)
    {
        readNeighborInitsFromNBT(chunk, nbt);
        readNeighborLightChecksFromNBT(chunk, nbt);
    }*/

    public static void fillSkylightColumn(final WorldChunk chunk, final int x, final int z)
    {
        final ChunkSection[] extendedBlockStorage = chunk.method_27413();

        final int height = chunk.method_27386(x, z);

        for (int j = height >> 4; j < extendedBlockStorage.length; ++j)
        {
            final ChunkSection blockStorage = extendedBlockStorage[j];

            if (blockStorage == WorldChunk.EMPTY_SECTION)
                continue;

            final int yMin = Math.max(j << 4, height);

            for (int y = yMin & 15; y < 16; ++y)
                blockStorage.method_27436(x, y, z, LightType.SKY.field_23634);
        }

        chunk.markDirty();
    }

    public static void initChunkLighting(final World world, final WorldChunk chunk)
    {
        if (chunk.method_27428() || ((NewLightChunk) chunk).getPendingNeighborLightInits() != 0)
            return;

        ((NewLightChunk) chunk).setPendingNeighborLightInits(15);

        chunk.markDirty();

        final int xBase = chunk.field_25365 << 4;
        final int zBase = chunk.field_25366 << 4;

        final PooledMutable pos = PooledMutable.get();

        final ChunkSection[] extendedBlockStorage = chunk.method_27413();

        for (int j = 0; j < extendedBlockStorage.length; ++j)
        {
            final ChunkSection blockStorage = extendedBlockStorage[j];

            if (blockStorage == WorldChunk.EMPTY_SECTION)
                continue;

            for (int x = 0; x < 16; ++x)
            {
                for (int z = 0; z < 16; ++z)
                {
                    for (int y = 0; y < 16; ++y)
                    {
                        if (blockStorage.method_27435(x, y, z).method_27195() > 0)
                            world.method_26095(LightType.BLOCK, pos.set(xBase + x, (j << 4) + y, zBase + z));
                    }
                }
            }
        }

        pos.method_31935();

        if (!world.dimension.method_27521())
            return;

        for (int x = 0; x < 16; ++x)
        {
            for (int z = 0; z < 16; ++z)
            {
                final int yMax = chunk.method_27386(x, z);
                int yMin = Math.max(yMax - 1, 0);

                for (final Direction dir : DirectionAccessor.getHorizontals())
                {
                    final int nX = x + dir.getOffsetX();
                    final int nZ = z + dir.getOffsetZ();

                    if (((nX | nZ) & CHUNK_COORD_OVERFLOW_MASK) != 0)
                        continue;

                    yMin = Math.min(yMin, chunk.method_27386(nX, nZ));
                }

                scheduleRelightChecksForColumn(world, LightType.SKY, xBase + x, zBase + z, yMin, yMax - 1);
            }
        }
    }

    private static void initNeighborLight(final World world, final WorldChunk chunk, final WorldChunk nChunk, final Direction nDir)
    {
        final int flag = 1 << nDir.getHorizontal();

        if ((((NewLightChunk) chunk).getPendingNeighborLightInits() & flag) == 0)
            return;

        ((NewLightChunk) chunk).setPendingNeighborLightInits(((NewLightChunk) chunk).getPendingNeighborLightInits() ^ flag);

        if (((NewLightChunk) chunk).getPendingNeighborLightInits() == 0)
            chunk.method_27406(true);

        chunk.markDirty();

        final int xOffset = nDir.getOffsetX();
        final int zOffset = nDir.getOffsetZ();

        final int xMin;
        final int zMin;

        if ((xOffset | zOffset) > 0)
        {
            xMin = 0;
            zMin = 0;
        }
        else
        {
            xMin = 15 * (xOffset & 1);
            zMin = 15 * (zOffset & 1);
        }

        final int xMax = xMin + 15 * (zOffset & 1);
        final int zMax = zMin + 15 * (xOffset & 1);

        final int xBase = nChunk.field_25365 << 4;
        final int zBase = nChunk.field_25366 << 4;

        final PooledMutable pos = PooledMutable.get();

        for (int x = xMin; x <= xMax; ++x)
        {
            for (int z = zMin; z <= zMax; ++z)
            {
                int yMin = chunk.method_27386((x - xOffset) & 15, (z - zOffset) & 15);

                // Restore a value <= initial height
                for (; yMin > 0; --yMin)
                {
                    if (((NewLightChunk) chunk).getCachedLightFor(LightType.SKY, pos.set(xBase + x - xOffset, yMin - 1, zBase + z - zOffset)) < LightType.SKY.field_23634)
                        break;
                }

                int yMax = nChunk.method_27386(x, z) - 1;

                for (final Direction dir : DirectionAccessor.getHorizontals())
                {
                    final int nX = x + dir.getOffsetX();
                    final int nZ = z + dir.getOffsetZ();

                    if (((nX | nZ) & CHUNK_COORD_OVERFLOW_MASK) != 0)
                        continue;

                    yMax = Math.min(yMax, nChunk.method_27386(nX, nZ));
                }

                scheduleRelightChecksForColumn(world, LightType.SKY, xBase + x, zBase + z, yMin, yMax - 1);
            }
        }

        pos.method_31935();
    }

    public static void initNeighborLight(final World world, final WorldChunk chunk)
    {
        final class_5305 provider = world.getChunkManager();

        for (final Direction dir : DirectionAccessor.getHorizontals())
        {
            final WorldChunk nChunk = provider.method_27346(chunk.field_25365 + dir.getOffsetX(), chunk.field_25366 + dir.getOffsetZ());

            if (nChunk == null)
                continue;

            initNeighborLight(world, chunk, nChunk, dir);
            initNeighborLight(world, nChunk, chunk, dir.getOpposite());
        }
    }

    /*public static final String neighborLightInitsKey = "PendingNeighborLightInits";

    private static void writeNeighborInitsToNBT(final Chunk chunk, final NBTTagCompound nbt)
    {
        if (chunk.pendingNeighborLightInits != 0)
            nbt.setShort(neighborLightInitsKey, chunk.pendingNeighborLightInits);
    }

    private static void readNeighborInitsFromNBT(final Chunk chunk, final NBTTagCompound nbt)
    {
        if (nbt.hasKey(neighborLightInitsKey, 2))
            chunk.pendingNeighborLightInits = nbt.getShort(neighborLightInitsKey);
    }*/

    public static void initSkylightForSection(final World world, final WorldChunk chunk, final ChunkSection section)
    {
        if (world.dimension.method_27521())
        {
            for (int x = 0; x < 16; ++x)
            {
                for (int z = 0; z < 16; ++z)
                {
                    if (chunk.method_27386(x, z) <= section.getYOffset())
                    {
                        for (int y = 0; y < 16; ++y)
                        {
                            section.method_27436(x, y, z, LightType.SKY.field_23634);
                        }
                    }
                }
            }
        }
    }

    public static void relightSkylightColumns(final World world, final WorldChunk chunk, @Nullable int[] oldHeightMap)
    {
        if (!world.dimension.method_27521())
            return;

        if (oldHeightMap == null)
            return;

        for (int x = 0; x < 16; ++x)
        {
            for (int z = 0; z < 16; ++z)
                relightSkylightColumn(world, chunk, x, z, oldHeightMap[z << 4 | x], chunk.method_27386(x, z));
        }
    }

    public static void relightSkylightColumn(final World world, final WorldChunk chunk, final int x, final int z, final int height1, final int height2)
    {
        final int yMin = Math.min(height1, height2);
        final int yMax = Math.max(height1, height2) - 1;

        final ChunkSection[] sections = chunk.method_27413();

        final int xBase = (chunk.field_25365 << 4) + x;
        final int zBase = (chunk.field_25366 << 4) + z;

        scheduleRelightChecksForColumn(world, LightType.SKY, xBase, zBase, yMin, yMax);

        if (sections[yMin >> 4] == WorldChunk.EMPTY_SECTION && yMin > 0)
        {
            world.method_26095(LightType.SKY, new BlockPos(xBase, yMin - 1, zBase));
        }

        short emptySections = 0;

        for (int sec = yMax >> 4; sec >= yMin >> 4; --sec)
        {
            if (sections[sec] == WorldChunk.EMPTY_SECTION)
            {
                emptySections |= 1 << sec;
            }
        }

        if (emptySections != 0)
        {
            for (final Direction dir : DirectionAccessor.getHorizontals())
            {
                final int xOffset = dir.getOffsetX();
                final int zOffset = dir.getOffsetZ();

                final boolean neighborColumnExists =
                    (((x + xOffset) | (z + zOffset)) & 16) == 0 //Checks whether the position is at the specified border (the 16 bit is set for both 15+1 and 0-1)
                        || world.getChunkManager().method_27346(chunk.field_25365 + xOffset, chunk.field_25366 + zOffset) != null;

                if (neighborColumnExists)
                {
                    for (int sec = yMax >> 4; sec >= yMin >> 4; --sec)
                    {
                        if ((emptySections & (1 << sec)) != 0)
                        {
                            scheduleRelightChecksForColumn(world, LightType.SKY, xBase + xOffset, zBase + zOffset, sec << 4, (sec << 4) + 15);
                        }
                    }
                }
                else
                {
                    flagChunkBoundaryForUpdate(chunk, emptySections, LightType.SKY, dir, getAxisDirection(dir, x, z), EnumBoundaryFacing.OUT);
                }
            }
        }
    }

    public static void scheduleRelightChecksForArea(final World world, final LightType lightType, final int xMin, final int yMin, final int zMin, final int xMax, final int yMax, final int zMax)
    {
        for (int x = xMin; x <= xMax; ++x)
        {
            for (int z = zMin; z <= zMax; ++z)
            {
                scheduleRelightChecksForColumn(world, lightType, x, z, yMin, yMax);
            }
        }
    }

    private static void scheduleRelightChecksForColumn(final World world, final LightType lightType, final int x, final int z, final int yMin, final int yMax)
    {
        for (int y = yMin; y <= yMax; ++y)
        {
            world.method_26095(lightType, new BlockPos(x, y, z));
        }
    }

    public enum EnumBoundaryFacing
    {
        IN, OUT;

        public EnumBoundaryFacing getOpposite()
        {
            return this == IN ? OUT : IN;
        }
    }

    public static void flagSecBoundaryForUpdate(final WorldChunk chunk, final BlockPos pos, final LightType lightType, final Direction dir, final EnumBoundaryFacing boundaryFacing)
    {
        flagChunkBoundaryForUpdate(chunk, (short) (1 << (pos.getY() >> 4)), lightType, dir, getAxisDirection(dir, pos.getX(), pos.getZ()), boundaryFacing);
    }

    public static void flagChunkBoundaryForUpdate(final WorldChunk chunk, final short sectionMask, final LightType lightType, final Direction dir, final AxisDirection axisDirection, final EnumBoundaryFacing boundaryFacing)
    {
        initNeighborLightChecks((NewLightChunk) chunk);
        ((NewLightChunk) chunk).getNeighborLightChecks()[getFlagIndex(lightType, dir, axisDirection, boundaryFacing)] |= sectionMask;
        chunk.markDirty();
    }

    public static int getFlagIndex(final LightType lightType, final int xOffset, final int zOffset, final AxisDirection axisDirection, final EnumBoundaryFacing boundaryFacing)
    {
        return (lightType == LightType.BLOCK ? 0 : 16) | ((xOffset + 1) << 2) | ((zOffset + 1) << 1) | (axisDirection.offset() + 1) | boundaryFacing.ordinal();
    }

    public static int getFlagIndex(final LightType lightType, final Direction dir, final AxisDirection axisDirection, final EnumBoundaryFacing boundaryFacing)
    {
        return getFlagIndex(lightType, dir.getOffsetX(), dir.getOffsetZ(), axisDirection, boundaryFacing);
    }

    private static AxisDirection getAxisDirection(final Direction dir, final int x, final int z)
    {
        return ((dir.getAxis() == Axis.X ? z : x) & 15) < 8 ? AxisDirection.NEGATIVE : AxisDirection.POSITIVE;
    }

    public static void scheduleRelightChecksForChunkBoundaries(final World world, final WorldChunk chunk)
    {
        for (final Direction dir : DirectionAccessor.getHorizontals())
        {
            final int xOffset = dir.getOffsetX();
            final int zOffset = dir.getOffsetZ();

            final WorldChunk nChunk = world.getChunkManager().method_27346(chunk.field_25365 + xOffset, chunk.field_25366 + zOffset);

            if (nChunk == null)
            {
                continue;
            }

            for (final LightType lightType : ENUM_SKY_BLOCK_VALUES)
            {
                for (final AxisDirection axisDir : ENUM_AXIS_DIRECTION_VALUES)
                {
                    //Merge flags upon loading of a chunk. This ensures that all flags are always already on the IN boundary below
                    mergeFlags(lightType, chunk, nChunk, dir, axisDir);
                    mergeFlags(lightType, nChunk, chunk, dir.getOpposite(), axisDir);

                    //Check everything that might have been canceled due to this chunk not being loaded.
                    //Also, pass in chunks if already known
                    //The boundary to the neighbor chunk (both ways)
                    scheduleRelightChecksForBoundary(world, chunk, nChunk, null, lightType, xOffset, zOffset, axisDir);
                    scheduleRelightChecksForBoundary(world, nChunk, chunk, null, lightType, -xOffset, -zOffset, axisDir);
                    //The boundary to the diagonal neighbor (since the checks in that chunk were aborted if this chunk wasn't loaded, see scheduleRelightChecksForBoundary)
                    scheduleRelightChecksForBoundary(world, nChunk, null, chunk, lightType, (zOffset != 0 ? axisDir.offset() : 0), (xOffset != 0 ? axisDir.offset() : 0), dir.getDirection() == AxisDirection.POSITIVE ? AxisDirection.NEGATIVE : AxisDirection.POSITIVE);
                }
            }
        }
    }

    private static void mergeFlags(final LightType lightType, final WorldChunk inChunk, final WorldChunk outChunk, final Direction dir, final AxisDirection axisDir)
    {
        if (((NewLightChunk) outChunk).getNeighborLightChecks() == null)
        {
            return;
        }

        initNeighborLightChecks(((NewLightChunk) inChunk));

        final int inIndex = getFlagIndex(lightType, dir, axisDir, EnumBoundaryFacing.IN);
        final int outIndex = getFlagIndex(lightType, dir.getOpposite(), axisDir, EnumBoundaryFacing.OUT);

        ((NewLightChunk) inChunk).getNeighborLightChecks()[inIndex] |= ((NewLightChunk) outChunk).getNeighborLightChecks()[outIndex];
        //no need to call Chunk.setModified() since checks are not deleted from outChunk
    }

    private static void scheduleRelightChecksForBoundary(final World world, final WorldChunk chunk, WorldChunk nChunk, WorldChunk sChunk, final LightType lightType, final int xOffset, final int zOffset, final AxisDirection axisDir)
    {
        if (((NewLightChunk) chunk).getNeighborLightChecks() == null)
        {
            return;
        }

        final int flagIndex = getFlagIndex(lightType, xOffset, zOffset, axisDir, EnumBoundaryFacing.IN); //OUT checks from neighbor are already merged

        final int flags = ((NewLightChunk) chunk).getNeighborLightChecks()[flagIndex];

        if (flags == 0)
        {
            return;
        }

        if (nChunk == null)
        {
            nChunk = world.getChunkManager().method_27346(chunk.field_25365 + xOffset, chunk.field_25366 + zOffset);

            if (nChunk == null)
            {
                return;
            }
        }

        if (sChunk == null)
        {
            sChunk = world.getChunkManager().method_27346(chunk.field_25365 + (zOffset != 0 ? axisDir.offset() : 0), chunk.field_25366 + (xOffset != 0 ? axisDir.offset() : 0));

            if (sChunk == null)
            {
                return; //Cancel, since the checks in the corner columns require the corner column of sChunk
            }
        }

        final int reverseIndex = getFlagIndex(lightType, -xOffset, -zOffset, axisDir, EnumBoundaryFacing.OUT);

        ((NewLightChunk) chunk).getNeighborLightChecks()[flagIndex] = 0;

        if (((NewLightChunk) nChunk).getNeighborLightChecks() != null)
        {
            ((NewLightChunk) nChunk).getNeighborLightChecks()[reverseIndex] = 0; //Clear only now that it's clear that the checks are processed
        }

        chunk.markDirty();
        nChunk.markDirty();

        //Get the area to check
        //Start in the corner...
        int xMin = chunk.field_25365 << 4;
        int zMin = chunk.field_25366 << 4;

        //move to other side of chunk if the direction is positive
        if ((xOffset | zOffset) > 0)
        {
            xMin += 15 * xOffset;
            zMin += 15 * zOffset;
        }

        //shift to other half if necessary (shift perpendicular to dir)
        if (axisDir == AxisDirection.POSITIVE)
        {
            xMin += 8 * (zOffset & 1); //x & 1 is same as abs(x) for x=-1,0,1
            zMin += 8 * (xOffset & 1);
        }

        //get maximal values (shift perpendicular to dir)
        final int xMax = xMin + 7 * (zOffset & 1);
        final int zMax = zMin + 7 * (xOffset & 1);

        for (int y = 0; y < 16; ++y)
        {
            if ((flags & (1 << y)) != 0)
            {
                scheduleRelightChecksForArea(world, lightType, xMin, y << 4, zMin, xMax, (y << 4) + 15, zMax);
            }
        }
    }

    public static void initNeighborLightChecks(final NewLightChunk chunk)
    {
        if (chunk.getNeighborLightChecks() == null)
        {
            chunk.setNeighborLightChecks(new short[FLAG_COUNT]);
        }
    }

    public static final String neighborLightChecksKey = "NeighborLightChecks";

    private static void writeNeighborLightChecksToNBT(final WorldChunk chunk, final CompoundTag nbt)
    {
        if (((NewLightChunk) chunk).getNeighborLightChecks() == null)
        {
            return;
        }

        boolean empty = true;
        final ListTag list = new ListTag();

        for (final short flags : ((NewLightChunk) chunk).getNeighborLightChecks())
        {
            list.add(new ShortTag(flags));

            if (flags != 0)
            {
                empty = false;
            }
        }

        if (!empty)
        {
            nbt.put(neighborLightChecksKey, list);
        }
    }

    private static void readNeighborLightChecksFromNBT(final WorldChunk chunk, final CompoundTag nbt)
    {
        if (nbt.contains(neighborLightChecksKey, 9))
        {
            final ListTag list = nbt.getList(neighborLightChecksKey, 2);

            if (list.size() == FLAG_COUNT)
            {
                initNeighborLightChecks((NewLightChunk) chunk);

                for (int i = 0; i < FLAG_COUNT; ++i)
                {
                    ((NewLightChunk) chunk).getNeighborLightChecks()[i] = ((ShortTag) list.method_32113(i)).getShort();
                }
            }
            else
            {
            	LOGGER.warn("Chunk field {} had invalid length, ignoring it (chunk coordinates: {} {})", neighborLightChecksKey, chunk.field_25365, chunk.field_25366);
            }
        }
    }
}
