package com.huziyang520.merlinlib.notice;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.api.NoticeMode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * One join notice, from wherever it came.
 *
 * @param id                the notice's id; its namespace is the mod the notice belongs to, which is what the
 *                          settings screen groups by
 * @param mode              when it is shown
 * @param lines             the lines to send, each already carrying its own colours and styles
 * @param enabledByDefault  whether the owning mod wants it on before anyone touches a setting
 */
public record NoticeEntry(Identifier id, NoticeMode mode, List<Component> lines, boolean enabledByDefault) {

    public NoticeEntry {
        lines = List.copyOf(lines);
    }

    /** @return the mod the notice belongs to, taken from the id's namespace */
    public String owner() {
        return this.id.getNamespace();
    }

    /** @return true when the notice belongs to MerlinLib itself rather than to a business mod */
    public boolean ownedByLibrary() {
        return Constants.MOD_ID.equals(this.owner());
    }
}
