package de.cadentem.cave_dweller.datagen;

import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.Tags.Biomes;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

public class ModBiomeTagsProvider extends BiomeTagsProvider {
   public static TagKey<Biome> CAVE_DWELLER_SURFACE_BIOMES;

   public ModBiomeTagsProvider(DataGenerator generator, String modId, @Nullable ExistingFileHelper existingFileHelper) {
      super(generator, modId, existingFileHelper);
   }

   protected void addTags() {
      this.tag(CAVE_DWELLER_SURFACE_BIOMES).addOptionalTag(Biomes.IS_SPOOKY.location());
   }

   static {
      CAVE_DWELLER_SURFACE_BIOMES = TagKey.create(Registry.BIOME_REGISTRY, new ResourceLocation("cave_dweller", "cave_dweller_surface_biomes"));
   }
}
