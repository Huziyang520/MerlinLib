// Reads the two client members whose names differ between 26.1 and 26.2, so one jar serves both.

package com.huziyang520.merlinlib.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ClientAccess {
    private static Method cameraGetter;
    private static Field screenField;
    private static Field guiField;
    private static Method guiScreenGetter;
    private static boolean probed;

    private ClientAccess() {
    }

    public static Camera camera(GameRenderer renderer) {
        if (!probed) {
            probe();
        }
        Object found = call(cameraGetter, renderer);
        return found instanceof Camera camera ? camera : null;
    }

    public static Screen screen(Minecraft minecraft) {
        if (!probed) {
            probe();
        }
        if (screenField != null) {
            Object open = read(screenField, minecraft);
            return open instanceof Screen screen ? screen : null;
        }
        Object open = call(guiScreenGetter, read(guiField, minecraft));
        return open instanceof Screen screen ? screen : null;
    }

    private static void probe() {
        probed = true;
        cameraGetter = method(GameRenderer.class, "getMainCamera");
        if (cameraGetter == null) {
            cameraGetter = method(GameRenderer.class, "mainCamera");
        }
        screenField = field(Minecraft.class, "screen");
        if (screenField == null) {
            guiField = field(Minecraft.class, "gui");
            Class<?> guiClass = guiField == null ? null : guiField.getType();
            guiScreenGetter = guiClass == null ? null : method(guiClass, "screen");
        }
    }

    private static Method method(Class<?> owner, String name) {
        try {
            Method found = owner.getMethod(name);
            found.setAccessible(true);
            return found;
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

    private static Object call(Method method, Object target) {
        if (method == null || target == null) {
            return null;
        }
        try {
            return method.invoke(target);
        } catch (ReflectiveOperationException failure) {
            return null;
        }
    }

    private static Object read(Field field, Object target) {
        if (field == null || target == null) {
            return null;
        }
        try {
            return field.get(target);
        } catch (ReflectiveOperationException failure) {
            return null;
        }
    }
}
