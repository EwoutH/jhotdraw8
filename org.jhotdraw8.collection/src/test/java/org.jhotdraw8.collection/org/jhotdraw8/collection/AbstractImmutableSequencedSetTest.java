package org.jhotdraw8.collection;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.collection.immutable.ImmutableSequencedSet;
import org.jhotdraw8.collection.immutable.ImmutableSet;
import org.jhotdraw8.collection.readonly.ReadOnlySequencedSet;
import org.jhotdraw8.collection.sequenced.SequencedSet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public abstract class AbstractImmutableSequencedSetTest extends AbstractImmutableSetTest {
    @Override
    protected abstract @NonNull <E> ImmutableSequencedSet<E> newInstance();

    @Override
    protected abstract @NonNull <E> SequencedSet<E> toMutableInstance(ImmutableSet<E> m);

    @Override
    protected abstract @NonNull <E> ImmutableSequencedSet<E> toImmutableInstance(Set<E> m);

    @Override
    protected abstract @NonNull <E> ImmutableSequencedSet<E> toClonedInstance(ImmutableSet<E> m);

    @Override
    protected abstract @NonNull <E> ImmutableSequencedSet<E> newInstance(Iterable<E> m);


    @ParameterizedTest
    @MethodSource("dataProvider")
    public void testCopyRemoveLastWithEmptySetShouldThrowNoSuchElementException(@NonNull SetData data) throws Exception {
        ImmutableSequencedSet<HashCollider> instance = newInstance(data.a());
        instance = instance.removeAll(data.a().asSet());
        assertThrows(NoSuchElementException.class, instance::removeLast);
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void testRemoveLastShouldNotChangeSequence(@NonNull SetData data) throws Exception {
        ImmutableSequencedSet<HashCollider> instance = newInstance(data.a());
        List<HashCollider> expected = new ArrayList<>(data.a().asSet());
        while (!expected.isEmpty()) {
            ImmutableSequencedSet<HashCollider> instance2 = instance.removeLast();
            assertNotSame(instance, instance2);
            expected.remove(expected.size() - 1);
            assertEqualSequence(expected, instance2, "removeLast");
            instance = instance2;
        }
    }

    @ParameterizedTest
    @MethodSource("dataProvider")
    public void testRemoveLastStartingWithEmptySetShouldNotChangeSequence(@NonNull SetData data) throws Exception {
        ImmutableSequencedSet<HashCollider> instance = newInstance();
        instance = instance.addAll(data.a.asSet());
        List<HashCollider> expected = new ArrayList<>(data.a().asSet());
        while (!expected.isEmpty()) {
            ImmutableSequencedSet<HashCollider> instance2 = instance.removeLast();
            assertNotSame(instance, instance2);
            expected.remove(expected.size() - 1);
            assertEqualSequence(expected, instance2, "removeLast");
            instance = instance2;
        }
    }


    protected <E> void assertEqualSequence(Collection<E> expected, ReadOnlySequencedSet<E> actual, String message) {
        ArrayList<E> expectedList = new ArrayList<>(expected);
        if (!expected.isEmpty()) {
            assertEquals(expectedList.get(0), actual.getFirst(), message);
            assertEquals(expectedList.get(0), actual.iterator().next(), message);
            assertEquals(expectedList.get(expectedList.size() - 1), actual.getLast(), message);
            //assertEquals(expectedList.get(expectedList.size() - 1), actual.reversed().iterator().next(), message);
        }
        assertEquals(expectedList, new ArrayList<>(actual.asSet()), message);
        assertEquals(expected.toString(), actual.toString(), message);
    }
}
