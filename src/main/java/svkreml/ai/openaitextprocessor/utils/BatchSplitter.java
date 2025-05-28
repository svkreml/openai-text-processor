package svkreml.ai.openaitextprocessor.utils;

import java.util.*;

public class BatchSplitter {
    public static List<List<Map.Entry<Object, Object>>> splitIntoBatches(Set<Map.Entry<Object, Object>> entries, int batchSize) {
        List<List<Map.Entry<Object, Object>>> batches = new ArrayList<>();
        Iterator<Map.Entry<Object, Object>> iterator = entries.iterator();

        while (iterator.hasNext()) {
            List<Map.Entry<Object, Object>> batch = new ArrayList<>(batchSize);
            for (int i = 0; i < batchSize && iterator.hasNext(); i++) {
                batch.add(iterator.next());
            }
            batches.add(batch);
        }
        return batches;
    }
}