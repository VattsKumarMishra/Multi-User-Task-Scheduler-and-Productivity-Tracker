package com.btech.productivitytracker.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// Generic repository to demonstrate reusable type-safe storage.
public class Repository<T> {
    private final Map<String, T> storage = new HashMap<>();

    public void save(String key, T value) {
        storage.put(key, value);
    }

    public Optional<T> findByKey(String key) {
        return Optional.ofNullable(storage.get(key));
    }

    public Collection<T> findAll() {
        return storage.values();
    }

    public void delete(String key) {
        storage.remove(key);
    }

    public Map<String, T> asMap() {
        return storage;
    }
}
