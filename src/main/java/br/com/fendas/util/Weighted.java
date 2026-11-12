package br.com.fendas.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Lista com sorteio por peso. */
public final class Weighted<T> {

    private final List<T> items = new ArrayList<>();
    private final List<Integer> weights = new ArrayList<>();
    private int total;

    public Weighted<T> add(T item, int weight) {
        items.add(item);
        weights.add(weight);
        total += weight;
        return this;
    }

    public T pick(Random rnd) {
        int roll = rnd.nextInt(total);
        for (int i = 0; i < items.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) return items.get(i);
        }
        return items.get(items.size() - 1);
    }

    public List<T> items() {
        return items;
    }
}
