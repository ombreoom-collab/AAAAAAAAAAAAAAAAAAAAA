package fr.pixelradar;

import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Lit les infos d'un Pokemon par reflexion : aucune dependance de compilation sur Pixelmon. */
final class PixelmonBridge {
    static final class Info {
        String name = "?";
        boolean shiny, legendary, owned;
    }

    private static final Map<String, Method> CACHE = new ConcurrentHashMap<>();
    private static final Method MISSING;

    static {
        try { MISSING = Object.class.getMethod("hashCode"); } catch (Exception e) { throw new RuntimeException(e); }
    }

    private PixelmonBridge() {}

    static boolean isPixelmon(Entity e) {
        ResourceLocation id = e.getType().getRegistryName();
        return id != null && "pixelmon".equals(id.getNamespace()) && "pixelmon".equals(id.getPath());
    }

    private static Object call(Object target, String name) {
        if (target == null) return null;
        String key = target.getClass().getName() + "#" + name;
        Method m = CACHE.get(key);
        if (m == null) {
            try { m = target.getClass().getMethod(name); } catch (Exception ex) { m = MISSING; }
            CACHE.put(key, m);
        }
        if (m == MISSING) return null;
        try { return m.invoke(target); } catch (Exception ex) { return null; }
    }

    private static boolean bool(Object target, String name) {
        Object r = call(target, name);
        return r instanceof Boolean && (Boolean) r;
    }

    static Info read(Entity e) {
        Object pk = call(e, "getPokemon");
        if (pk == null) return null;
        Info i = new Info();
        i.shiny = bool(pk, "isShiny");
        i.legendary = bool(pk, "isLegendary") || bool(pk, "isMythical") || bool(pk, "isUltraBeast");
        i.owned = bool(e, "hasOwner");
        Object n = call(pk, "getLocalizedName");
        if (n instanceof ITextComponent) i.name = ((ITextComponent) n).getString();
        else if (n != null) i.name = n.toString();
        else i.name = e.getName().getString();
        return i;
    }
}
