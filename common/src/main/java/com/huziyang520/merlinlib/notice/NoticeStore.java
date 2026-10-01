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

/**
 * Which notices a world save has already shown.
 *
 * <h2>Why the save and not the player</h2>
 *
 * <p>"Once per world" is what the mode means, and a world's own data is the only place that stays true to
 * that: it disappears with the world, comes back with it, and does not have to be kept in step with anything
 * else. A player who copies a world elsewhere still sees the notice once there, which is correct - it is a
 * different world.
 *
 * <p>Player specific entries carry the player's id in the key, so one player seeing a notice does not silence
 * it for the others.
 */
public final class NoticeStore extends SavedData {

    /** The type the overworld's storage keeps this under. */
    public static final SavedDataType<NoticeStore> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "join_notices"),
            NoticeStore::new,
            Codec.STRING.listOf().xmap(NoticeStore::new, store -> new ArrayList<>(store.keys)),
            DataFixTypes.LEVEL);

    /** The keys already shown in this save. */
    private final Set<String> keys = new HashSet<>();

    /** Creates an empty store, for a save that has never shown a notice. */
    public NoticeStore() {
    }

    private NoticeStore(List<String> keys) {
        this.keys.addAll(keys);
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
