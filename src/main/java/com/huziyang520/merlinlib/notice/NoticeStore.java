// Which notices a world save has already shown.

package com.huziyang520.merlinlib.notice;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashSet;
import java.util.Set;

public final class NoticeStore extends SavedData {
    public static final String STORAGE_KEY = "merlinlib_join_notices";
    private static final String KEYS_TAG = "keys";
    private final Set<String> keys = new HashSet<>();

    public NoticeStore() {
    }

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

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (String key : this.keys) {
            list.add(StringTag.valueOf(key));
        }
        tag.put(KEYS_TAG, list);
        return tag;
    }

    public boolean remember(String key) {
        return this.keys.add(key);
    }

    public boolean knows(String key) {
        return this.keys.contains(key);
    }
}
