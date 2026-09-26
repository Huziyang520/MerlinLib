package com.huziyang520.merlinlib.tools.hud;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ConfigManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The combat feedback overlay: the nominal damage above the crosshair and the floating damage numbers.
 *
 * <h2>Both are drawn in the HUD layer</h2>
 *
 * <p>Floating numbers over an entity look like world rendering, but drawing them in the HUD layer and
 * projecting the world position to screen space has three advantages: no world render event is needed
 * (the two loaders differ a lot there), the text always faces the player by construction, and one
 * renderer works on both loaders - Fabric's {@code HudElement} and NeoForge's {@code GuiLayer} hand out
 * the very same {@link GuiGraphicsExtractor} plus a {@link DeltaTracker}.
 *
 * <h2>Where the numbers come from</h2>
 *
 * <ul>
 *   <li><b>Crosshair number</b>: the damage the server resolved for the swing, sent over the wire. The
 *       client cannot recompute it, because the enchantment pipeline needs a {@code ServerLevel}.</li>
 *   <li><b>Floating numbers</b>: the health drop of <i>every</i> living entity in render distance,
 *       sampled each client tick. Tracking only the local player's own targets missed damage dealt by
 *       other players and by the environment.</li>
 * </ul>
 *
 * <h2>Every switch is read where it is used</h2>
 *
 * <p>The two displays and all their parameters come from {@code client.toml} and are read at draw time,
 * not cached in fields. That is what makes the configuration screen honest: switching damage numbers off
 * takes effect on the next frame, without a restart, and switching the crosshair number on takes effect
 * on the next hit.
 */
public final class CombatFeedback {

    /** Base scale of the floating damage numbers, multiplied by the configured size. */
    private static final float POPUP_SCALE = 1.6F;
    /** Extra scale per point of damage, so big hits read as big hits. */
    private static final float POPUP_SCALE_PER_DAMAGE = 0.012F;
    /** Maximum scale of a floating number. */
    private static final float POPUP_MAX_SCALE = 2.4F;
    /** Scale of the crosshair number. */
    private static final float HUD_SCALE = 2.0F;
    /** Vertical distance from the screen centre to the crosshair number. */
    private static final int HUD_OFFSET_Y = 30;
    /** How long a crit flag stays attached to a target after our swing, in ticks. */
    private static final int CRIT_MEMORY_TICKS = 10;
    /** How far a floating number drifts towards the player over its lifetime, in blocks. */
    private static final double POPUP_DRIFT = 1.25D;

    private static final CombatFeedback INSTANCE = new CombatFeedback();

    private final List<Popup> popups = new ArrayList<>();
    /** Last seen health of every tracked living entity, keyed by entity id. */
    private final Map<Integer, Float> lastHealth = new HashMap<>();
    /** Until which tick a target is marked as critically hit by the local player. */
    private final Map<Integer, Integer> criticalUntil = new HashMap<>();
    private long tickCounter;

    private Float hudDamage;
    private boolean hudCritical;
    private int hudTicks;

    private CombatFeedback() {
    }

    /**
     * @return the shared overlay state.
     */
    public static CombatFeedback instance() {
        return INSTANCE;
    }

    /** A floating damage number. */
    private static final class Popup {
        /** The entity that took the hit, kept so the number can ride its interpolated position. */
        private final LivingEntity target;
        /** Last known anchor, used once the entity is gone. */
        private Vec3 lastAnchor;
        private final float damage;
        private final float scale;
        private final boolean critical;
        private final int riseIndex;
        private int age;

        private Popup(LivingEntity target, Vec3 anchor, float damage, float scale, boolean critical, int riseIndex) {
            this.target = target;
            this.lastAnchor = anchor;
            this.damage = damage;
            this.scale = scale;
            this.critical = critical;
            this.riseIndex = riseIndex;
        }
    }

    // ------------------------------------------------------------------ input

    /**
     * Receives the server's verdict for one resolved hit and arms the crosshair number with it.
     *
     * <p>The server resolves enchantments, the weapon's own bonus and every modded override while it
     * applies the hit, so its number is the truth; the client cannot recompute it because the
     * enchantment pipeline needs a {@code ServerLevel}.
     *
     * <p>The display duration is read here rather than at draw time, because it is the length of the
     * countdown that is being started.
     *
     * @param damage   the unclamped swing damage the server resolved
     * @param critical whether the server classified the swing as critical
     * @param targetId the id of the entity that was hit
     */
    public void onServerDamage(float damage, boolean critical, int targetId) {

        this.hudDamage = Math.max(0.5F, damage);
        this.hudCritical = critical;
        this.hudTicks = Math.max(1, ConfigManager.client().crosshairDamageTicks());
        this.criticalUntil.put(targetId, (int) (this.tickCounter + CRIT_MEMORY_TICKS));
    }

    /**
     * Records one local swing so a resulting popup can be coloured as critical. The number itself comes
     * from the server, see {@link #onServerDamage(float, boolean, int)}.
     *
     * @param player   the local player
     * @param target   the entity being hit
     * @param critical whether the swing looks critical on the client
     */
    public void onLocalSwing(Player player, Entity target, boolean critical) {

        if (critical) {
            this.criticalUntil.put(target.getId(), (int) (this.tickCounter + CRIT_MEMORY_TICKS));
        }
    }

    /**
     * Evaluates vanilla's critical hit condition for the local player.
     *
     * @param player the attacker
     * @return {@code true} when the swing counts as critical
     */
    public static boolean isCritical(Player player) {
        return player.fallDistance > 0.0F
                && !player.onGround()
                && !player.onClimbable()
                && !player.isInWater()
                && !player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)
                && !player.isPassenger()
                && !player.isSprinting();
    }

    // ------------------------------------------------------------------ tick

    /**
     * Advances the overlay by one client tick and samples the health of every nearby living entity, so
     * damage from any player or source shows up, not just the local player's own hits.
     *
     * <p>Sampling is skipped entirely while damage numbers are switched off, which is the point of the
     * switch: an overlay that is not wanted costs nothing.
     *
     * @param minecraft the client
     */
    public void tick(Minecraft minecraft) {

        this.tickCounter++;

        if (this.hudTicks > 0) {
            this.hudTicks--;
        } else {
            this.hudDamage = null;
        }

        boolean numbersEnabled = ConfigManager.client().floatingTextEnabled();
        if (numbersEnabled && minecraft.level != null) {
            List<Integer> seen = new ArrayList<>();
            for (Entity entity : minecraft.level.entitiesForRendering()) {
                if (!(entity instanceof LivingEntity living)) {
                    continue;
                }
                Integer id = living.getId();
                seen.add(id);
                Float previous = this.lastHealth.put(id, living.getHealth());
                if (previous != null && living.getHealth() < previous) {
                    float damage = previous - living.getHealth();
                    if (damage > 0.001F) {
                        addPopup(living, damage, this.criticalUntil.getOrDefault(id, -1) >= this.tickCounter);
                    }
                }
            }
            // Forget entities that are gone so the map cannot grow without bound.
            this.lastHealth.keySet().removeIf(id -> !seen.contains(id));
        } else if (!numbersEnabled) {
            this.lastHealth.clear();
        }
        this.criticalUntil.values().removeIf(until -> until < this.tickCounter);

        int lifetime = Math.max(1, ConfigManager.client().floatingTextDurationTicks());
        for (Iterator<Popup> it = this.popups.iterator(); it.hasNext(); ) {
            Popup popup = it.next();
            if (++popup.age > lifetime) {
                it.remove();
            }
        }
    }

    private void addPopup(LivingEntity target, float damage, boolean critical) {

        float base = POPUP_SCALE * (float) ConfigManager.client().floatingTextBaseScale();
        float scale = Math.min(POPUP_MAX_SCALE, base + damage * POPUP_SCALE_PER_DAMAGE);
        // Every hit gets its own vertical slot so simultaneous hits stay readable.
        int index = this.popups.size();
        this.popups.add(new Popup(target, anchorOf(target), damage, scale, critical, index));
        int limit = Math.max(1, ConfigManager.client().floatingTextMaxConcurrent());
        while (this.popups.size() > limit) {
            this.popups.remove(0);
        }
    }

    /**
     * The anchor above a living entity's head; kept well clear of the body so the number is easy to
     * read even when the camera is close.
     *
     * @param target the hit entity
     * @return the world position the number starts at
     */
    private static Vec3 anchorOf(LivingEntity target) {
        return target.position().add(0.0D, target.getBbHeight() * 1.1D + 0.4D, 0.0D);
    }

    // ------------------------------------------------------------------ render

    /**
     * Draws the whole overlay; shaped exactly like both loaders' HUD callbacks.
     *
     * @param graphics the render state extractor
     * @param delta    the frame delta tracker
     */
    public void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        // 26.3 has neither Minecraft.screen nor Options.hideGui, so the overlay rides the vanilla HUD
        // layer and lets the game decide when the HUD is hidden.

        int guiWidth = minecraft.getWindow().getGuiScaledWidth();
        int guiHeight = minecraft.getWindow().getGuiScaledHeight();

        if (ConfigManager.client().crosshairDamage()) {
            drawHudDamage(graphics, minecraft, guiWidth, guiHeight);
        }
        if (ConfigManager.client().floatingTextEnabled()) {
            drawPopups(graphics, minecraft, delta, guiWidth, guiHeight);
        }
    }

    /**
     * Draws the damage of the last swing, centred directly above the crosshair.
     */
    private void drawHudDamage(GuiGraphicsExtractor graphics, Minecraft minecraft, int guiWidth, int guiHeight) {

        Float damage = this.hudDamage;
        if (damage == null || this.hudTicks <= 0) {
            return;
        }
        int fade = Math.max(1, ConfigManager.client().crosshairDamageTicks());
        int alpha = this.hudTicks > fade / 3 ? 0xFF : (int) (255.0F * this.hudTicks / (fade / 3.0F));
        int rgb = this.hudCritical
                ? ConfigManager.client().floatingTextCriticalColor() & 0xFFFFFF
                : damageColour(damage);
        int colour = (Math.max(24, alpha) << 24) | rgb;

        String text = format(damage);
        int textWidth = minecraft.font.width(text);
        int x = guiWidth / 2 - (int) (textWidth * HUD_SCALE / 2.0F);
        int y = guiHeight / 2 - HUD_OFFSET_Y;

        graphics.pose().pushMatrix();
        graphics.pose().scale(HUD_SCALE, HUD_SCALE);
        graphics.text(minecraft.font, text, (int) (x / HUD_SCALE), (int) (y / HUD_SCALE), colour, true);
        graphics.pose().popMatrix();
    }

    private void drawPopups(GuiGraphicsExtractor graphics, Minecraft minecraft, DeltaTracker delta,
                            int guiWidth, int guiHeight) {

        if (this.popups.isEmpty()) {
            return;
        }
        int lifetime = Math.max(1, ConfigManager.client().floatingTextDurationTicks());
        int normalColour = ConfigManager.client().floatingTextNormalColor() & 0xFFFFFF;
        int criticalColour = ConfigManager.client().floatingTextCriticalColor() & 0xFFFFFF;

        Camera camera = minecraft.gameRenderer.mainCamera();
        Vec3 eye = camera.position();
        float fov = camera.getFov();
        double aspect = (double) guiWidth / (double) guiHeight;
        double focal = 1.0D / Math.tan(Math.toRadians(fov) * 0.5D);
        // The camera already moves with the partial tick, so the anchor must too; sampling the raw tick
        // position made every number wobble whenever the view moved.
        float partial = delta.getGameTimeDeltaPartialTick(false);

        for (Popup popup : this.popups) {
            float progress = (float) popup.age / (float) lifetime;
            Vec3 origin;
            if (popup.target.isRemoved()) {
                origin = popup.lastAnchor;
            } else {
                origin = popup.target.getPosition(partial).add(
                        0.0D, popup.target.getBbHeight() * 1.1D + 0.4D, 0.0D);
                popup.lastAnchor = origin;
            }
            Vec3 towards = eye.subtract(origin).normalize().scale(POPUP_DRIFT * progress);
            Vec3 world = origin.add(towards).add(0.0D, 0.35D * progress + 0.02D * popup.riseIndex, 0.0D);

            double[] screen = project(world, eye, camera, focal, aspect, guiWidth, guiHeight);
            if (screen == null) {
                continue;
            }
            int alpha = (int) (255.0F * (1.0F - progress * progress));
            int rgb = popup.critical ? criticalColour : normalColour;
            int colour = (Math.max(16, alpha) << 24) | rgb;

            String text = format(popup.damage);
            int textWidth = (int) (minecraft.font.width(text) * popup.scale);
            int x = (int) screen[0] - textWidth / 2;
            int y = (int) screen[1];

            graphics.pose().pushMatrix();
            graphics.pose().scale(popup.scale, popup.scale);
            graphics.text(minecraft.font, text, (int) (x / popup.scale), (int) (y / popup.scale), colour, true);
            graphics.pose().popMatrix();
        }
    }

    /**
     * Projects a world position onto the GUI, or returns {@code null} when it is behind the camera.
     */
    private static double[] project(Vec3 world, Vec3 eye, Camera camera, double focal, double aspect,
                                    int guiWidth, int guiHeight) {

        double yaw = Math.toRadians(camera.yRot());
        double pitch = Math.toRadians(camera.xRot());

        double forwardX = -Math.sin(yaw) * Math.cos(pitch);
        double forwardY = -Math.sin(pitch);
        double forwardZ = Math.cos(yaw) * Math.cos(pitch);

        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);

        double upX = -forwardY * rightZ;
        double upY = forwardZ * rightX - forwardX * rightZ;
        double upZ = forwardY * rightX;

        Vec3 delta = world.subtract(eye);
        double depth = delta.x * forwardX + delta.y * forwardY + delta.z * forwardZ;
        if (depth < 0.06D) {
            return null;
        }
        double side = delta.x * rightX + delta.z * rightZ;
        double up = delta.x * upX + delta.y * upY + delta.z * upZ;

        double ndcX = (side / depth) * (focal / aspect);
        double ndcY = (up / depth) * focal;

        double x = (ndcX + 1.0D) * 0.5D * guiWidth;
        double y = (1.0D - ndcY) * 0.5D * guiHeight;
        if (x < -96.0D || y < -96.0D || x > guiWidth + 96.0D || y > guiHeight + 96.0D) {
            return null;
        }
        return new double[] { x, y };
    }

    /**
     * The crosshair number's colour by swing damage: white at low damage, through yellow, to red for
     * heavy hits (40+), so the number itself communicates the scale of the swing.
     *
     * <p>Only a normal hit uses this gradient; a critical hit uses the configured critical colour, so one
     * switch controls how criticals look everywhere.
     *
     * @param damage the swing damage
     * @return an RGB colour
     */
    private static int damageColour(float damage) {

        // two segments: white -> yellow over 0..20, yellow -> red over 20..40
        float ratio = Math.min(1.0F, damage / 40.0F);
        float r;
        float g;
        float b;
        if (ratio < 0.5F) {
            float t = ratio * 2.0F;
            r = 1.0F;
            g = 1.0F - t * (1.0F - 0.667F);
            b = 1.0F - t;
        } else {
            float t = (ratio - 0.5F) * 2.0F;
            r = 1.0F;
            g = 0.667F * (1.0F - t);
            b = 0.0F;
        }
        return (Math.round(r * 255.0F) << 16) | (Math.round(g * 255.0F) << 8) | Math.round(b * 255.0F);
    }

    private static String format(float damage) {
        if (damage == Math.floor(damage)) {
            return Integer.toString((int) damage);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", damage);
    }

    /**
     * Logs the overlay being registered, once, so the log proves the HUD hook is live.
     */
    public static void announce() {
        Constants.LOG.info("[MerlinLib] combat overlay registered on the HUD (nominal damage + floating damage numbers)");
    }
}
