// A loader supplied store for MerlinLib's switches.

package com.huziyang520.merlinlib.config;

public interface ConfigBackend {
    ConfigReader view();
    void write(String key, String raw);
    void save();
    String name();
}
