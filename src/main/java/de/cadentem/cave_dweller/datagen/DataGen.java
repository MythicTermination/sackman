package de.cadentem.cave_dweller.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

@EventBusSubscriber(
   modid = "cave_dweller",
   bus = Bus.MOD
)
public class DataGen {
   @SubscribeEvent
   public static void configureDataGen(GatherDataEvent event) {
      DataGenerator generator = event.getGenerator();
      ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
      generator.addProvider(new ModItemModelProvider(generator, "cave_dweller", existingFileHelper));
      generator.addProvider(new ModBiomeTagsProvider(generator, "cave_dweller", existingFileHelper));
   }
}
