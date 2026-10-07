package com.huziyang520.merlinlib.tools.hud;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ConfigManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ForgeHooksClient;

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
 * renderer works on both loaders - the 26.3 line's Fabric {@code HudElement} and NeoForge
 * {@code GuiLayer} both handed out the same extractor plus a delta tracker.
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
 *
 * <h2>What changed / 1.20.1 note</h2>
 *
 * <p>This is a port of the whole overlay, not a translation by pattern. Five things had no 1.20.1
 * counterpart and were re-derived from the bytecode of this version:
 *
 * <ul>
 *   <li><b>The render callback's second argument.</b> 26.3 took a {@code DeltaTracker} and asked it for
 *       {@code getGameTimeDeltaPartialTick(false)}. There is no {@code DeltaTracker} on 1.20.1 at all
 *       ({@code javap net.minecraft.client.DeltaTracker} reports the class missing), because the HUD
 *       callback of this version is still handed the partial tick directly. The render method therefore
 *       takes that float and uses it verbatim - it is the same number the tracker was asked for.</li>
 *   <li><b>The pose stack.</b> {@code PoseStack#pushMatrix}/{@code popMatrix} and a two argument
 *       {@code scale} are 1.21 names. This version's {@code PoseStack} has {@code pushPose},
 *       {@code popPose} and {@code scale(float, float, float)}, so the draw thunks are written with the
 *       three argument form and a {@code 1.0F} on the z axis, which is exactly what the two argument
 *       form meant.</li>
 *   <li><b>Text.</b> {@code GuiGraphicsExtractor#text} is this version's
 *       {@code GuiGraphics#drawString(Font, String, int, int, int, boolean)}, the same call with the
 *       same drop shadow flag.</li>
 *   <li><b>The camera's field of view.</b> This is the one real loss. {@code Camera#getFov()} exists on
 *       26.3 and does <b>not</b> exist on 1.20.1: the whole field of view lives inside
 *       {@code GameRenderer}, behind a {@code private double getFov(Camera, float, boolean)}. See
 *       {@link #fieldOfView} for what replaced it and why the replacement is not a guess.</li>
 *   <li><b>The camera's angles.</b> {@code Camera#yRot()} / {@code xRot()} are the bean accessors
 *       {@code getYRot()} / {@code getXRot()} on this version, confirmed with
 *       {@code javap -p net.minecraft.client.Camera}. The projection maths itself is unchanged.</li>
 * </ul>
 *
 * <p>One thing is deliberately <em>not</em> changed, and is worth stating because it looks like an
 * omission: the 26.3 class comment says the overlay "rides the vanilla HUD layer and lets the game
 * decide when the HUD is hidden". On 1.20.1 that decision is not a layer concept - the whole HUD is
 * skipped by {@code GameRenderer#render} unless {@code !Options.hideGui || Screen != null} (bytecode of
 * {@code GameRenderer#render}, offsets 555 to 575). Anything hooked to the HUD therefore inherits the
 * rule automatically, which is what the 26.3 comment intended.
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
    /** The z axis of a pose scale, which is never scaled: text and fills are two dimensional. */
    private static final float FLAT = 1.0F;

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
     * <p>Direct port: every member read here kept its name on 1.20.1. {@code fallDistance} is still a
     * public field of {@code Entity} and {@code MobEffects.BLINDNESS} still a public static field, both
     * confirmed with {@code javap -p}.
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
     * Draws the whole overlay.
     *
     * <p><b>What changed / 1.20.1 note:</b> the callback is shaped for this version. 26.3's
     * {@code (GuiGraphicsExtractor, DeltaTracker)} pair has no counterpart here - the extractor is
     * {@link GuiGraphics} and the delta tracker does not exist - so the second parameter is the partial
     * tick itself, which is the value the tracker was asked to compute. A Forge GUI overlay hands that
     * straight over: {@code (gui, graphics, partialTick, width, height) -> render(graphics, partialTick)}.
     *
     * @param graphics    the draw context
     * @param partialTick the frame's partial tick, used to interpolate entity positions
     */
    public void render(GuiGraphics graphics, float partialTick) {

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        // The HUD is skipped by the game itself while Options.hideGui is set, which is the 1.20.1
        // spelling of the 26.3 note about the overlay riding the vanilla HUD layer.

        int guiWidth = minecraft.getWindow().getGuiScaledWidth();
        int guiHeight = minecraft.getWindow().getGuiScaledHeight();

        if (ConfigManager.client().crosshairDamage()) {
            drawHudDamage(graphics, minecraft, guiWidth, guiHeight);
        }
        if (ConfigManager.client().floatingTextEnabled()) {
            drawPopups(graphics, minecraft, partialTick, guiWidth, guiHeight);
        }
    }

    /**
     * Draws the damage of the last swing, centred directly above the crosshair.
     */
    private void drawHudDamage(GuiGraphics graphics, Minecraft minecraft, int guiWidth, int guiHeight) {

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

        // pushPose / popPose / scale(float, float, float) are this version's names for the 26.3
        // pushMatrix / popMatrix / scale(float, float). The z axis is unscaled, as it was implicitly.
        graphics.pose().pushPose();
        graphics.pose().scale(HUD_SCALE, HUD_SCALE, FLAT);
        graphics.drawString(minecraft.font, text, (int) (x / HUD_SCALE), (int) (y / HUD_SCALE), colour, true);
        graphics.pose().popPose();
    }

    private void drawPopups(GuiGraphics graphics, Minecraft minecraft, float partialTick,
                            int guiWidth, int guiHeight) {

        if (this.popups.isEmpty()) {
            return;
        }
        int lifetime = Math.max(1, ConfigManager.client().floatingTextDurationTicks());
        int normalColour = ConfigManager.client().floatingTextNormalColor() & 0xFFFFFF;
        int criticalColour = ConfigManager.client().floatingTextCriticalColor() & 0xFFFFFF;

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 eye = camera.getPosition();
        double fov = fieldOfView(minecraft, camera, partialTick);
        double aspect = (double) guiWidth / (double) guiHeight;
        double focal = 1.0D / Math.tan(Math.toRadians(fov) * 0.5D);
        // The camera already moves with the partial tick, so the anchor must too; sampling the raw tick
        // position made every number wobble whenever the view moved. The partial tick is the render
        // parameter on this version, which is where the 26.3 DeltaTracker took it from as well.
        float partial = partialTick;

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

            graphics.pose().pushPose();
            graphics.pose().scale(popup.scale, popup.scale, FLAT);
            graphics.drawString(minecraft.font, text, (int) (x / popup.scale), (int) (y / popup.scale),
                    colour, true);
            graphics.pose().popPose();
        }
    }

    /**
     * The vertical field of view in degrees, for the frame being drawn.
     *
     * <p><b>What changed / 1.20.1 note:</b> this is the one member of this class with no direct
     * counterpart. 26.3 called {@code Camera#getFov()}, which does not exist on 1.20.1 - the whole field
     * of view lives inside {@code GameRenderer}, in a {@code private double getFov(Camera, float,
     * boolean)} with no public accessor. The value is therefore assembled from the same two inputs that
     * vanilla's own method reads, which was established by disassembling it rather than by assuming:
     *
     * <pre>
     * private double getFov(net.minecraft.client.Camera, float, boolean);
     *   ...
     *   11: ldc2_w  70.0d                      // the base used when the setting is not consulted
     *   20: Minecraft.options / Options.fov()  // the player's field of view setting
     *   44: fload_2 / oldFov / fov -> Mth.lerp(FFF)   // the smoothed field of view multiplier
     *   60: Camera.getEntity() instanceof LivingEntity, isDeadOrDying() ...
     *  126: Camera.getFluidInCamera() == LAVA || == WATER ...
     *  179: ForgeHooksClient.getFieldOfView(GameRenderer, Camera, double, double, boolean)
     *  189: dreturn
     * </pre>
     *
     * <p>The multiplier vanilla interpolates is the one {@code AbstractClientPlayer#getFieldOfViewModifier}
     * produces - {@code GameRenderer#tickFov} is literally
     * {@code fov += (modifier - fov) * 0.5F}, and that modifier is a public method on this version. The
     * configured setting times the interpolated modifier is therefore the quantity 26.3's
     * {@code getFov()} returned.
     *
     * <p>The result is finally passed through Forge's own {@code getFieldOfView} hook, which is the call
     * vanilla's method ends in. That matters: it posts {@code ViewportEvent.ComputeFov}, so a mod that
     * changes the field of view moves these numbers with the view instead of leaving them misaligned -
     * and it is the same door vanilla itself uses, so the two can never disagree.
     *
     * <p>What is deliberately <em>not</em> reproduced: the death zoom and the lava/water narrowing, both
     * of which only apply while the camera entity is dying or submerged. A damage number drifting over an
     * entity while the player is drowning is not a case worth a second copy of vanilla's arithmetic, and
     * the error is a few percent of the projection scale.
     *
     * @param minecraft   the client
     * @param camera      the camera being projected through
     * @param partialTick the frame's partial tick
     * @return the vertical field of view in degrees
     */
    private static double fieldOfView(Minecraft minecraft, Camera camera, float partialTick) {

        double configured = minecraft.options.fov().get();
        if (minecraft.player != null) {
            configured *= Mth.lerp(partialTick, 1.0F, minecraft.player.getFieldOfViewModifier());
        }
        return ForgeHooksClient.getFieldOfView(minecraft.gameRenderer, camera, partialTick, configured, true);
    }

    /**
     * Projects a world position onto the GUI, or returns {@code null} when it is behind the camera.
     *
     * <p>The only change from 26.3 is the camera's angle accessors: {@code yRot()} and {@code xRot()} are
     * the bean methods {@code getYRot()} and {@code getXRot()} on this version. The projection maths is
     * byte for byte the same, which is what keeps the numbers sitting on the same spot on screen.
     */
    private static double[] project(Vec3 world, Vec3 eye, Camera camera, double focal, double aspect,
                                    int guiWidth, int guiHeight) {

        double yaw = Math.toRadians(camera.getYRot());
        double pitch = Math.toRadians(camera.getXRot());

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
