package de.cadentem.cave_dweller.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class ClientConfig {
   public static final Builder BUILDER = new Builder();
   public static final ForgeConfigSpec SPEC;
   public static ConfigValue<Boolean> USE_UPDATED_TEXTURES;

   static {
      USE_UPDATED_TEXTURES = BUILDER.comment("Use updated textures by the user '...'").define("use_updated_textures", true);
      SPEC = BUILDER.build();
   }
}
