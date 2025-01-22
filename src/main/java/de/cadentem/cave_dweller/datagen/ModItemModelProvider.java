package de.cadentem.cave_dweller.datagen;

import de.cadentem.cave_dweller.registry.ModItems;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
   public ModItemModelProvider(DataGenerator generator, String modId, ExistingFileHelper existingFileHelper) {
      super(generator, modId, existingFileHelper);
   }

   protected void registerModels() {
      this.withExistingParent(ModItems.CAVE_DWELLER_SPAWN_EGG.getId().getPath(), this.mcLoc("item/template_spawn_egg"));
   }
}
