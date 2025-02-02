package org.jhotdraw8.collection;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.collection.champ.ChampMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChampMapTest extends AbstractMapTest {
    @Override
    protected <K, V> @NonNull Map<K, V> newInstance() {
        return new ChampMap<>();
    }

    @Override
    protected <K, V> @NonNull Map<K, V> newInstance(int numElements, float loadFactor) {
        return new ChampMap<>();
    }

    @Override
    protected <K, V> @NonNull Map<K, V> newInstance(Map<K, V> m) {
        return new ChampMap<>(m);
    }

    @Override
    protected <K, V> @NonNull Map<K, V> newInstance(@NonNull Iterable<Map.Entry<K, V>> m) {
        return new ChampMap<>(m);
    }


    @Override
    protected <K, V> @NonNull Map<K, V> toClonedInstance(Map<K, V> m) {
        return ((ChampMap<K, V>) m).clone();
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void testNewInstanceReadOnlyArgOfDifferentTypeShouldBeEqualToArg(MapData data) {
        Map<HashCollider, HashCollider> actual = new ChampMap<>(data.a());
        assertEqualMap(data.a(), actual);
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void testIteratorShouldYieldExpectedEntries(MapData data) throws Exception {
        Map<HashCollider, HashCollider> instance = newInstance(data.a);
        List<Map.Entry<HashCollider, HashCollider>> actualList = new ArrayList<>();
        LinkedHashMap<HashCollider, HashCollider> actualMap = new LinkedHashMap<>();
        ((ChampMap<HashCollider, HashCollider>) instance).iterator().forEachRemaining(actualList::add);
        ((ChampMap<HashCollider, HashCollider>) instance).iterator().forEachRemaining(e -> actualMap.put(e.getKey(), e.getValue()));
        assertEquals(data.a.size(), actualList.size());
        assertEqualMap(data.a, actualMap);
    }
}
