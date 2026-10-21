package tech.maloandre.chomagerie.blockstate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.List;
import java.util.function.LongPredicate;

/** Dimension-owned saved data. No global world references, scans or per-interaction disk writes. */
public final class BlockStateLocks extends SavedData {
    private record Entry(long pos, BlockState state) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("pos").forGetter(Entry::pos),
                BlockState.CODEC.fieldOf("state").forGetter(Entry::state)
        ).apply(i, Entry::new));
    }
    private static final Codec<BlockStateLocks> CODEC = Entry.CODEC.listOf().xmap(BlockStateLocks::new, BlockStateLocks::entries);
    public static final SavedDataType<BlockStateLocks> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("chomagerie", "block_state_locks"), BlockStateLocks::new, CODEC, null);
    private final Long2ObjectOpenHashMap<BlockState> locks = new Long2ObjectOpenHashMap<>();

    // Rebuilt from saved entries; queries never scan all locks in a dimension or load chunks.
    private final Long2ObjectOpenHashMap<LongSet> chunks = new Long2ObjectOpenHashMap<>();

    private final Long2LongOpenHashMap chunkRevisions = new Long2LongOpenHashMap();
    private long revision;
    public long chunkRevision(long chunk) { return chunkRevisions.get(chunk); }
    public long revision() { return revision; }

    public BlockStateLocks() { }
    private BlockStateLocks(List<Entry> entries) {
        entries.forEach(e -> {
            var pos = BlockPos.of(e.pos);
            locks.put(e.pos, e.state);
            index(pos);
            chunkRevisions.put(ChunkPos.pack(pos), ++revision);
        });
    }
    private List<Entry> entries() {
        return locks.long2ObjectEntrySet().stream().map(e -> new Entry(e.getLongKey(), e.getValue())).toList();
    }
    public static BlockStateLocks get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(TYPE); }
    public boolean contains(BlockPos pos) { return locks.containsKey(pos.asLong()); }
    public void lock(BlockPos pos, BlockState state) {
        if (locks.put(pos.asLong(), state) == null) {
            index(pos);
            chunkRevisions.put(ChunkPos.pack(pos), ++revision);
        }
        setDirty();
    }
    private void index(BlockPos pos) {
        chunks.computeIfAbsent(ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4), key -> new LongOpenHashSet()).add(pos.asLong());
    }
    public void remove(BlockPos pos) {
        if (locks.remove(pos.asLong()) == null) return;
        revision++;
        long chunk = ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
        LongSet positions = chunks.get(chunk);
        positions.remove(pos.asLong());
        if (positions.isEmpty()) {
            chunks.remove(chunk);
            chunkRevisions.remove(chunk);
        } else chunkRevisions.put(chunk, revision);
        setDirty();
    }

    /** Spherical distance from block centers, limited to already available chunks. */
    public LongSet nearby(Vec3 center, int distance, LongPredicate availableChunk) {
        LongSet result = new LongOpenHashSet();
        int minX = ((int) Math.floor(center.x - distance)) >> 4;
        int maxX = ((int) Math.floor(center.x + distance)) >> 4;
        int minZ = ((int) Math.floor(center.z - distance)) >> 4;
        int maxZ = ((int) Math.floor(center.z + distance)) >> 4;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                long chunk = ChunkPos.pack(x, z);
                LongSet positions = chunks.get(chunk);
                if (positions == null || !availableChunk.test(chunk)) continue;
                for (long packed : positions) {
                    if (Vec3.atCenterOf(BlockPos.of(packed)).distanceToSqr(center) <= (double) distance * distance) result.add(packed);
                }
            }
        }
        return result;
    }
    /** Full snapshot, including properties unrelated to connections and replacement by updateShape. */
    public BlockState preserve(BlockPos pos, BlockState calculated) {
        return locks.getOrDefault(pos.asLong(), calculated);
    }

    /** Guard direct state writes as well as neighbor updates (e.g. furnace block-entity ticks).
     * Explicit replacement/destruction still removes the lock through the cleanup hook. */
    public boolean blocksWrite(BlockPos pos, BlockState requested) {
        BlockState reference = locks.get(pos.asLong());
        return reference != null && reference.getBlock() == requested.getBlock() && reference != requested;
    }

    /** Publish the new reference BEFORE the write, so guards and recursive neighbor updates agree.
     * Failed writes roll back the reference, and no broad bypass is left active. Server thread only. */
    public boolean setManually(ServerLevel level, BlockPos pos, BlockState state, int flags) {
        BlockState previous = locks.get(pos.asLong());
        if (previous == null) return level.setBlock(pos, state, flags);
        locks.put(pos.asLong(), state);
        boolean success = false;
        try {
            success = level.setBlock(pos, state, flags);
            if (success) setDirty();
            return success;
        } finally {
            if (!success) locks.put(pos.asLong(), previous);
        }
    }
}
