package com.huziyang520.merlinlib.notice;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/**
 * Which notices a world save has already shown.
 */
public final class NoticeStore extends SavedData {

    /**
     * The name the overworld's data storage keeps this under, which is also the file name
     * ({@code <world>/data/merlinlib_join_notices.dat}).
 */
    public static final String STORAGE_KEY = "merlinlib_join_notices";

    /** The {@code CompoundTag} key holding the recorded notices. */
    private static final String KEYS_TAG = "keys";

    /** The keys already shown in this save. */
    private final Set<String> keys = new HashSet<>();

    /** Creates an empty store, for a save that has never shown a notice. */
    public NoticeStore() {
    }

    /**
     * Reads a store out of a save's data file.
     *
     * <p>Handed to {@code DimensionDataStorage#computeIfAbsent} as the load function. A file written by a
     * different or a broken build must never stop a player from joining, so anything that is not a list of
     * strings is treated as "nothing was recorded yet" rather than as an error.
     *
     * @param tag the compound the save was read into
     * @return the store holding whatever had been recorded
 */
    public static NoticeStore load(CompoundTag tag) {
        NoticeStore store = new NoticeStore();
        ListTag list = tag.getList(KEYS_TAG, Tag.TAG_STRING);
        for (int index = 0; index < list.size(); index++) {
            String key = list.getString(index);
            if (!key.isEmpty()) {
                store.keys.add(key);
            }
        }
        return store;
    }

    /**
     * Writes the recorded keys into the save.
     *
     * <p>The compound is filled in place rather than replaced: the 1.20.1 contract passes in a tag that the
     * storage already prepared, and returning it keeps the two shapes interchangeable.
     *
     * @param tag the compound to write into
     * @return the same compound, for chaining
 */
    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (String key : this.keys) {
            list.add(StringTag.valueOf(key));
        }
        tag.put(KEYS_TAG, list);
        return tag;
    }

    /**
     * Records a key, reporting whether it is new.
     *
     * @param key the key to record
     * @return true when the key had not been recorded before
 */
    public boolean remember(String key) {
        return this.keys.add(key);
    }

    /**
     * @param key the key to look up
     * @return true when the key has been recorded
 */
    public boolean knows(String key) {
        return this.keys.contains(key);
    }
}
