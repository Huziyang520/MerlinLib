// The combat feedback overlay: the nominal damage above the crosshair and the floating damage numbers.

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

public final class CombatFeedback {
    private static final float POPUP_SCALE = 1.6F;
    private static final float POPUP_SCALE_PER_DAMAGE = 0.012F;
    private static final float POPUP_MAX_SCALE = 2.4F;
    private static final float HUD_SCALE = 2.0F;
    private static final int HUD_OFFSET_Y = 30;
    private static final int CRIT_MEMORY_TICKS = 10;
    private static final double POPUP_DRIFT = 1.25D;
    private static final CombatFeedback INSTANCE = new CombatFeedback();
    private final List<Popup> popups = new ArrayList<>();
    private final Map<Integer, Float> lastHealth = new HashMap<>();
    private final Map<Integer, Integer> criticalUntil = new HashMap<>();
    private long tickCounter;
    private Float hudDamage;
    private boolean hudCritical;
    private int hudTicks;

    private CombatFeedback() {
    }

    public static CombatFeedback instance() {
        return INSTANCE;
    }

    private static final class Popup {
        private final LivingEntity target;
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

    public void onServerDamage(float damage, boolean critical, int targetId) {
        this.hudDamage = Math.max(0.5F, damage);
        this.hudCritical = critical;
        this.hudTicks = Math.max(1, ConfigManager.client().crosshairDamageTicks());
        this.criticalUntil.put(targetId, (int) (this.tickCounter + CRIT_MEMORY_TICKS));
    }

    public void onLocalSwing(Player player, Entity target, boolean critical) {
        if (critical) {
            this.criticalUntil.put(target.getId(), (int) (this.tickCounter + CRIT_MEMORY_TICKS));
        }
    }

    public static boolean isCritical(Player player) {
        return player.fallDistance > 0.0F
                && !player.onGround()
                && !player.onClimbable()
                && !player.isInWater()
                && !player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)
                && !player.isPassenger()
                && !player.isSprinting();
    }

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
        int index = this.popups.size();
        this.popups.add(new Popup(target, anchorOf(target), damage, scale, critical, index));
        int limit = Math.max(1, ConfigManager.client().floatingTextMaxConcurrent());
        while (this.popups.size() > limit) {
            this.popups.remove(0);
        }
    }

    private static Vec3 anchorOf(LivingEntity target) {
        return target.position().add(0.0D, target.getBbHeight() * 1.1D + 0.4D, 0.0D);
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        int guiWidth = minecraft.getWindow().getGuiScaledWidth();
        int guiHeight = minecraft.getWindow().getGuiScaledHeight();

        if (ConfigManager.client().crosshairDamage()) {
            drawHudDamage(graphics, minecraft, guiWidth, guiHeight);
        }
        if (ConfigManager.client().floatingTextEnabled()) {
            drawPopups(graphics, minecraft, delta, guiWidth, guiHeight);
        }
    }

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

    private static int damageColour(float damage) {
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

    public static void announce() {
        Constants.LOG.info("[MerlinLib] combat overlay registered on the HUD (nominal damage + floating damage numbers)");
    }
}
