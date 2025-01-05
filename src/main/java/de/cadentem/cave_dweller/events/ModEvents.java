package de.cadentem.cave_dweller.events;

import de.cadentem.cave_dweller.client.CaveDwellerEyesLayer;
import de.cadentem.cave_dweller.config.ClientConfig;
import de.cadentem.cave_dweller.config.ServerConfig;
import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller.registry.ModEntityTypes;
import de.cadentem.cave_dweller.util.Utils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.config.ModConfigEvent.Reloading;

@EventBusSubscriber(
   modid = "cave_dweller",
   bus = Bus.MOD
)
public class ModEvents {
   @SubscribeEvent
   public static void entityAttributeEvent(EntityAttributeCreationEvent event) {
      event.put((EntityType)ModEntityTypes.CAVE_DWELLER.get(), CaveDwellerEntity.getAttributeBuilder());
   }

   @SubscribeEvent
   public static void reloadConfiguration(Reloading event) {
      if (event.getConfig().getSpec() == ClientConfig.SPEC) {
         ClientConfig.SPEC.acceptConfig(event.getConfig().getConfigData());
         CaveDwellerEyesLayer.TEXTURE = new ResourceLocation("cave_dweller", "textures/entity/cave_dweller_eyes_texture" + Utils.getTextureAppend() + ".png");
         de.cadentem.cave_dweller.CaveDweller.LOG.info("Client configuration has been reloaded");
      }

      if (event.getConfig().getSpec() == ServerConfig.SPEC) {
         ServerConfig.SPEC.acceptConfig(event.getConfig().getConfigData());
         de.cadentem.cave_dweller.CaveDweller.RELOAD_ALL = true;
         de.cadentem.cave_dweller.CaveDweller.LOG.info("Server configuration has been reloaded");
      }

   }
}
