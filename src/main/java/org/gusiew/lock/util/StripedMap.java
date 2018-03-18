package org.gusiew.lock.util;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StripedMap<K, V> {

    //FIXME test
    //TODO implement map ?
    //TODO javadoc

    private final Map<Integer, Map<K, V>> mapsByStripe;

    @SuppressWarnings("unchecked")
    public StripedMap(int numberOfStripes) {
        mapsByStripe = IntStream.range(0, numberOfStripes)
                .boxed()
                .map(i -> StripeMapPair.pair(i, new HashMap<K, V>()))
                .collect(Collectors.toMap(p -> p.stripeIndex, p -> p.map));
    }

    public V get(K key) {
        return mapsByStripe.get(getStripeIndex(key)).get(key);
    }

    public void put(K key, V value) {
        mapsByStripe.get(getStripeIndex(key)).put(key, value);
    }

    public void remove(K key) {
        mapsByStripe.get(getStripeIndex(key)).remove(key);
    }

    public boolean isEmpty() {
        return mapsByStripe.values().stream().mapToInt(Map::size).sum() == 0;
    }

    public Map<K, V> getStripe(K key) {
        return mapsByStripe.get(getStripeIndex(key));
    }

    private int getStripeIndex(K key) {
        return key.hashCode() % mapsByStripe.size();
    }

    private static class StripeMapPair<K, V> {

        final Integer stripeIndex;
        final Map<K, V> map;

        private StripeMapPair(Integer stripeIndex, Map<K, V> map) {
            this.stripeIndex = stripeIndex;
            this.map = map;
        }

        static <K, V> StripeMapPair pair(Integer index, Map<K, V> map) {
            return new StripeMapPair<>(index, map);
        }
    }
}
