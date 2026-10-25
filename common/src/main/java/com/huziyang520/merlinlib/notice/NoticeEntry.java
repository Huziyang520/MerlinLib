// One join notice, from wherever it came.

package com.huziyang520.merlinlib.notice;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.api.NoticeMode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.List;

public record NoticeEntry(Identifier id, NoticeMode mode, List<Component> lines, boolean enabledByDefault) {
    public NoticeEntry {
        lines = List.copyOf(lines);
    }

    public String owner() {
        return this.id.getNamespace();
    }

    public boolean ownedByLibrary() {
        return Constants.MOD_ID.equals(this.owner());
    }
}
