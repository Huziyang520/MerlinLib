// A read only view of MerlinLib's switches.

package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.content.ContentError;
import java.util.List;

public interface ConfigReader {
    boolean getBoolean(String key, boolean fallback);
    int getInt(String key, int fallback, int min, int max);
    double getDouble(String key, double fallback, double min, double max);
    int getColor(String key, int fallback);
    String getString(String key, String fallback);
    List<ContentError> errors();
}
