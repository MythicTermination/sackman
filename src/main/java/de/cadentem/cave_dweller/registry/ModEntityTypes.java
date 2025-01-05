package de.cadentem.cave_dweller.registry;

import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntityTypes {
   public static final DeferredRegister<EntityType<?>> ENTITY_TYPES;
   public static final RegistryObject<EntityType<CaveDwellerEntity>> CAVE_DWELLER;

   public static void register(IEventBus eventBus) {
      ENTITY_TYPES.register(eventBus);
   }

   static {
      ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "cave_dweller");
      CAVE_DWELLER = ENTITY_TYPES.register("cave_dweller", () -> {
         return Builder.of(CaveDwellerEntity::new, MobCategory.MONSTER).sized(0.5F, 2.7F).build((new ResourceLocation("cave_dweller", "cave_dweller")).toString());
      });
   }
}
