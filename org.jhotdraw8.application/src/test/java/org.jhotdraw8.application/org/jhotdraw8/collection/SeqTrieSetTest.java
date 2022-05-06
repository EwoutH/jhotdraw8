/*
 * @(#)TrieMapTest.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */

package org.jhotdraw8.collection;

import org.jhotdraw8.annotation.NonNull;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.Set;

public class SeqTrieSetTest extends AbstractSequencedSetTest {

    @Override
    protected @NonNull <T> Set<T> create(int expectedMaxSize, float maxLoadFactor) {
        return new SeqTrieSet<>();
    }

    @Test

    public void testDumpStructure() {
        SeqTrieSet<HashCollider> instance = new SeqTrieSet<>();
        Random rng = new Random(0);
        for (int i = 0; i < 30; i++) {
            HashCollider key = new HashCollider(rng.nextInt(1_000), ~0xff00);
            String value = "v" + i;
            instance.add(key);
        }

        System.out.println(instance.dump());

    }

}