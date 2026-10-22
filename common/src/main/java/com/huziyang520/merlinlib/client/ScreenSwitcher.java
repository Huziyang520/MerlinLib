package com.huziyang520.merlinlib.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Switches the open screen, on whichever of 26.1 and 26.2 is running.
 *
 * <h2>Why this cannot be a plain call</h2>
 *
 * <p>The method that opens a screen moved between the two releases this branch covers, and each one only exists
 * on one side of the move:
 *
 * <ul>
 *   <li><b>26.1.2</b>: {@code Minecraft#setScreen(Screen)} exists; {@code Gui} is the HUD and has no screen;</li>
 *   <li><b>26.2</b>: {@code Minecraft#setScreen} is <b>gone</b> - the screen moved to {@code Minecraft#gui},
 *       that is {@code Gui#setScreen(Screen)}. Calling the old method throws
 *       {@code NoSuchMethodError: 'void net.minecraft.client.Minecraft.setScreen(...)'}, which is what crashed
 *       the client the moment the configuration screen was closed.</li>
 * </ul>
 *
 * <p>A single jar therefore has to pick at run time. The first call probes the candidates once, keeps the one
 * that worked and logs it, so the cost is a few reflective lookups for the lifetime of the process.
 *
 * <p>{@code setScreenAndShow} exists on both releases and is used only as a last resort: it also forces a whole
 * render frame, which makes the dimming flash - the reason the other two are preferred.
 */
public final class ScreenSwitcher {

    /** The 26.1 method, {@code Minecraft#setScreen(Screen)}. */
    private static Method minecraftSetScreen;
    /** The 26.2 method, {@code Gui#setScreen(Screen)}. */
    private static Method guiSetScreen;
    /** The 26.2 field that holds the Gui, {@code Minecraft#gui}. */
    private static Field guiField;
    /** The fallback that exists on both, {@code Minecraft#setScreenAndShow(Screen)}. */
    private static Method setScreenAndShow;

    /** Whether the probe already ran, so a failure is not retried every frame. */
    private static boolean probed;
    /** Whether no usable method was found at all. */
    private static boolean exhausted;

    private ScreenSwitcher() {
    }

    /**
     * Opens a screen, or closes the current one when {@code screen} is {@code null}.
     *
     * @param minecraft the client instance
     * @param screen    the screen to show, or {@code null} to go back to the world
     */
    public static void show(Minecraft minecraft, Screen screen) {
        if (!probed) {
            probe();
        }
        if (minecraftSetScreen != null) {
            invoke(minecraftSetScreen, minecraft, screen);
            return;
        }
        if (guiSetScreen != null && guiField != null) {
            Object gui = read(guiField, minecraft);
            if (gui != null) {
                invoke(guiSetScreen, gui, screen);
                return;
            }
        }
        if (setScreenAndShow != null) {
            invoke(setScreenAndShow, minecraft, screen);
        }
    }

    /**
     * Finds the method this build actually has. Order matters: the two non-forcing calls are preferred over
     * {@code setScreenAndShow}, which renders an extra frame.
     */
    private static void probe() {
        probed = true;
        minecraftSetScreen = find(Minecraft.class, "setScreen", Screen.class);
        if (minecraftSetScreen != null) {
            com.huziyang520.merlinlib.Constants.LOG.info(
                    "[MerlinLib] screens are opened through Minecraft#setScreen (26.1)");
            return;
        }
        guiField = field(Minecraft.class, "gui");
        Class<?> guiClass = guiField == null ? null : guiField.getType();
        guiSetScreen = guiClass == null ? null : find(guiClass, "setScreen", Screen.class);
        if (guiSetScreen != null) {
            com.huziyang520.merlinlib.Constants.LOG.info(
                    "[MerlinLib] screens are opened through Gui#setScreen (26.2 and later)");
            return;
        }
        setScreenAndShow = find(Minecraft.class, "setScreenAndShow", Screen.class);
        if (setScreenAndShow != null) {
            com.huziyang520.merlinlib.Constants.LOG.warn(
                    "[MerlinLib] no plain screen switch found, falling back to setScreenAndShow");
            return;
        }
        exhausted = true;
        com.huziyang520.merlinlib.Constants.LOG.error(
                "[MerlinLib] no way to open a screen was found on this Minecraft build");
    }

    private static Method find(Class<?> owner, String name, Class<?>... parameters) {
        try {
            Method method = owner.getMethod(name, parameters);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException missing) {
            return null;
        }
    }

    private static Field field(Class<?> owner, String name) {
        try {
            Field found = owner.getField(name);
            found.setAccessible(true);
            return found;
        } catch (NoSuchFieldException missing) {
            return null;
        }
    }

    private static void invoke(Method method, Object target, Screen screen) {
        try {
            method.invoke(target, screen);
        } catch (ReflectiveOperationException failure) {
            if (!exhausted) {
                exhausted = true;
                com.huziyang520.merlinlib.Constants.LOG.error(
                        "[MerlinLib] opening a screen failed through {}", method, failure);
            }
        }
    }

    private static Object read(Field field, Object target) {
        try {
            return field.get(target);
        } catch (ReflectiveOperationException failure) {
            return null;
        }
    }
}
