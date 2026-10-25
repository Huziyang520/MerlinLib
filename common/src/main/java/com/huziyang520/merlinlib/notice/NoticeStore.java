// Which notices a world save has already shown.

package com.huziyang520.merlinlib.notice;

import com.huziyang520.merlinlib.Constants;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class NoticeStore extends SavedData {
    public static final SavedDataType<NoticeStore> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "join_notices"),
            NoticeStore::new,
            Codec.STRING.listOf().xmap(NoticeStore::new, store -> new ArrayList<>(store.keys)),
            DataFixTypes.LEVEL);

    private final Set<String> keys = new HashSet<>();

    public NoticeStore() {
    }

    private NoticeStore(List<String> keys) {
        this.keys.addAll(keys);
    }

    public boolean remember(String key) {
        return this.keys.add(key);
    }

    public boolean knows(String key) {
        return this.keys.contains(key);
    }
}
