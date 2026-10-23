package com.huziyang520.merlinlib.client;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;

import java.lang.reflect.Method;

/**
 * Reads the camera out of the game renderer on whichever of 26.1 and 26.2 is running.
 *
 * <h2>Why this cannot be a plain call</h2>
 *
 * <p>The getter was renamed between the two releases this branch covers and each spelling exists on only
 * one side:
 *
 * <ul>
 *   <li><b>26.1.2</b>: {@code GameRenderer#getMainCamera()}; {@code mainCamera()} does not exist;</li>
 *   <li><b>26.2</b>: {@code GameRenderer#mainCamera()} (records and rename-cleanups); {@code getMainCamera()}
 *       does not exist.</li>
 * </ul>
 *
 * <p>A jar compiled against 26.1.2 therefore links {@code getMainCamera()} and dies with
 * {@code NoSuchMethodError} on 26.2 the first time a floating damage number is drawn - a crash inside the
 * HUD layer, which takes the whole frame with it. The probe below runs once, keeps the method it found and
 * logs it, exactly like {@link ScreenSwitcher} does for the screen switch.
 */
public final class CameraAccess {

    /** The method that was found, or {@code null} when this build has neither spelling. */
    private static Method cameraGetter;

    /** Whether the probe already ran, so a failure is not retried every frame. */
    private static boolean probed;

    /** Set once a call has failed, so the log does not repeat the same error every frame. */
    private static boolean exhausted;

    private CameraAccess() {
    }

    /**
     * @param renderer the game renderer
     * @return the camera in use, or {@code null} when this build exposes no getter this class knows
     */
    public static Camera camera(GameRenderer renderer) {
        if (!probed) {
            probe();
        }
        if (cameraGetter == null) {
            return null;
        }
        try {
            Object camera = cameraGetter.invoke(renderer);
            return camera instanceof Camera found ? found : null;
        } catch (ReflectiveOperationException failure) {
            if (!exhausted) {
                exhausted = true;
                com.huziyang520.merlinlib.Constants.LOG.error(
                        "[MerlinLib] reading the camera failed through {}", cameraGetter, failure);
            }
            return null;
        }
    }

    /**
     * Finds the getter this build actually has. The 26.1 name is tried first: it is the one the sources are
     * compiled against, so on 26.1 nothing else is needed.
     */
    private static void probe() {
        probed = true;
        cameraGetter = find("getMainCamera");
        if (cameraGetter != null) {
            com.huziyang520.merlinlib.Constants.LOG.info(
                    "[MerlinLib] the camera is read through GameRenderer#getMainCamera (26.1)");
            return;
        }
        cameraGetter = find("mainCamera");
        if (cameraGetter != null) {
            com.huziyang520.merlinlib.Constants.LOG.info(
                    "[MerlinLib] the camera is read through GameRenderer#mainCamera (26.2 and later)");
            return;
        }
        com.huziyang520.merlinlib.Constants.LOG.error(
                "[MerlinLib] no camera getter was found on this Minecraft build; floating damage numbers "
                        + "stay off");
    }

    /**
     * @param name the method name to look for
     * @return the method, or {@code null} when this build does not have it
     */
    private static Method find(String name) {
        try {
            Method method = GameRenderer.class.getMethod(name);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException missing) {
            return null;
        }
    }
}
