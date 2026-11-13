package fr.pixelradar;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

public final class ClientInit {
    private ClientInit() {}

    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientInit::clientSetup);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        String cat = "key.categories.pixelradar";
        RadarHud.KEY_TOGGLE = new KeyBinding("key.pixelradar.toggle", GLFW.GLFW_KEY_O, cat);
        RadarHud.KEY_MODE = new KeyBinding("key.pixelradar.mode", GLFW.GLFW_KEY_P, cat);
        RadarHud.KEY_ZOOM = new KeyBinding("key.pixelradar.zoom", GLFW.GLFW_KEY_LEFT_BRACKET, cat);
        ClientRegistry.registerKeyBinding(RadarHud.KEY_TOGGLE);
        ClientRegistry.registerKeyBinding(RadarHud.KEY_MODE);
        ClientRegistry.registerKeyBinding(RadarHud.KEY_ZOOM);
        MinecraftForge.EVENT_BUS.register(new RadarHud());
    }
}
