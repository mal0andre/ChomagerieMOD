package tech.maloandre.chomagerie.network;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.List;

/** Tracks what was actually delivered, allowing large snapshots to be sent progressively. */
public final class LockOverlayDelta {
    public record Change(boolean locked, long[] positions) { }
    private final LongSet sent = new LongOpenHashSet();
    private final LongArrayFIFOQueue additions = new LongArrayFIFOQueue();
    private final LongArrayFIFOQueue removals = new LongArrayFIFOQueue();

    public void clear() {
        sent.clear();
        additions.clear();
        removals.clear();
    }

    public void update(LongSet desired) {
        additions.clear();
        removals.clear();
        for (long pos : sent) if (!desired.contains(pos)) removals.enqueue(pos);
        for (long pos : desired) if (!sent.contains(pos)) additions.enqueue(pos);
    }

    public List<Change> drain(int budget) {
        List<Change> result = new ArrayList<>(2);
        for (boolean locked : new boolean[]{false, true}) {
            LongArrayFIFOQueue queue = locked ? additions : removals;
            while (budget > 0 && !queue.isEmpty()) {
                long[] positions = new long[Math.min(Math.min(queue.size(), budget), LockOverlayPayload.MAX_POSITIONS)];
                for (int i = 0; i < positions.length; i++) {
                    long pos = queue.dequeueLong();
                    positions[i] = pos;
                    if (locked) sent.add(pos); else sent.remove(pos);
                }
                result.add(new Change(locked, positions));
                budget -= positions.length;
            }
        }
        return result;
    }
}
