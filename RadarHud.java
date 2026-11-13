package fr.pixelradar;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RadarHud {
    static KeyBinding KEY_TOGGLE, KEY_MODE, KEY_ZOOM;

    private static final int[] RANGES = {32, 64, 96, 128};
    private static final String[] MODES = {"Tous les Pokemon", "Rares (shiny + legendaires)", "Shiny seulement"};
    private static final int PINK = 0xFFFF10F0, GOLD = 0xFFFFC800, WHITE = 0xFFFFFFFF;

    private static boolean enabled = true;
    private static int mode = 1;     // par defaut : rares seulement
    private static int rangeIdx = 1; // 64 blocs

    private static final class Target {
        String name; boolean shiny; double dist, dy;
    }

    private static void msg(String s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.displayClientMessage(new StringTextComponent("[PixelRadar] " + s), true);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        while (KEY_TOGGLE.consumeClick()) { enabled = !enabled; msg(enabled ? "active" : "desactive"); }
        while (KEY_MODE.consumeClick()) { mode = (mode + 1) % MODES.length; msg(MODES[mode]); }
        while (KEY_ZOOM.consumeClick()) { rangeIdx = (rangeIdx + 1) % RANGES.length; msg("portee " + RANGES[rangeIdx] + " blocs"); }
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post e) {
        if (e.getType() != RenderGameOverlayEvent.ElementType.ALL || !enabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;

        MatrixStack ms = e.getMatrixStack();
        int size = 90, margin = 6;
        int w = mc.getWindow().getGuiScaledWidth();
        int x0 = w - size - margin, y0 = margin;
        int cx = x0 + size / 2, cy = y0 + size / 2;

        AbstractGui.fill(ms, x0 - 1, y0 - 1, x0 + size + 1, y0 + size + 1, 0xCC000000);
        AbstractGui.fill(ms, x0, y0, x0 + size, y0 + size, 0x88203020);
        AbstractGui.fill(ms, cx, y0, cx + 1, y0 + size, 0x33FFFFFF);
        AbstractGui.fill(ms, x0, cy, x0 + size, cy + 1, 0x33FFFFFF);

        double range = RANGES[rangeIdx];
        double half = size / 2.0 - 4;
        double rad = Math.toRadians(mc.player.yRot);
        double sin = Math.sin(rad), cos = Math.cos(rad);
        List<Target> rares = new ArrayList<>();

        for (Entity en : mc.level.entitiesForRendering()) {
            if (!PixelmonBridge.isPixelmon(en)) continue;
            PixelmonBridge.Info info = PixelmonBridge.read(en);
            if (info == null || info.owned) continue;
            boolean rare = info.shiny || info.legendary;
            if (mode == 1 && !rare) continue;
            if (mode == 2 && !info.shiny) continue;

            double dx = en.getX() - mc.player.getX();
            double dz = en.getZ() - mc.player.getZ();
            double dy = en.getY() - mc.player.getY();
            double fwd = -sin * dx + cos * dz;   // devant le joueur
            double right = -cos * dx - sin * dz; // a droite du joueur
            double sx = right / range * half;
            double sy = -fwd / range * half;

            double m = Math.max(Math.abs(sx), Math.abs(sy));
            if (m > half) {
                if (!rare) continue;       // les rares restent colles au bord = direction
                sx *= half / m; sy *= half / m;
            }
            int px = (int) Math.round(cx + sx), py = (int) Math.round(cy + sy);
            int color = info.shiny ? PINK : (info.legendary ? GOLD : WHITE);
            if (rare) {
                AbstractGui.fill(ms, px - 3, py - 3, px + 4, py + 4, 0xFF000000);
                AbstractGui.fill(ms, px - 2, py - 2, px + 3, py + 3, color);
                if (info.shiny && info.legendary) AbstractGui.fill(ms, px - 1, py - 1, px + 2, py + 2, GOLD);
                Target t = new Target();
                t.name = info.name; t.shiny = info.shiny;
                t.dist = Math.sqrt(dx * dx + dz * dz); t.dy = dy;
                rares.add(t);
            } else {
                AbstractGui.fill(ms, px - 1, py - 1, px + 2, py + 2, color);
            }
        }

        // joueur au centre
        AbstractGui.fill(ms, cx - 1, cy - 1, cx + 2, cy + 2, 0xFF55FF55);

        // liste des cibles rares sous la carte
        rares.sort(Comparator.comparingDouble(t -> t.dist));
        int ty = y0 + size + 4;
        for (int i = 0; i < Math.min(6, rares.size()); i++) {
            Target t = rares.get(i);
            String arrow = t.dy > 8 ? " ^" : (t.dy < -8 ? " v" : "");
            String line = (t.shiny ? "[S] " : "[L] ") + t.name + " " + (int) t.dist + "m" + arrow;
            mc.font.drawShadow(ms, line, w - margin - mc.font.width(line), ty, t.shiny ? PINK : GOLD);
            ty += 10;
        }
    }
}
