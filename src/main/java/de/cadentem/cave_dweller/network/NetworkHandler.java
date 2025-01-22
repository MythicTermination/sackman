package de.cadentem.cave_dweller.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
   private static final String PROTOCOL_VERSION = "2";
   public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation("cave_dweller", "main"), () -> {
      return "2";
   }, "2"::equals, "2"::equals);

   public static void register() {
      CHANNEL.registerMessage(0, CaveSound.class, CaveSound::encode, CaveSound::decode, CaveSound::handle);
   }
}
