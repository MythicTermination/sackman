package de.cadentem.cave_dweller.config;

import de.cadentem.cave_dweller.datagen.ModBiomeTagsProvider;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.registries.ForgeRegistries;

public class ServerConfig {
   public static final Builder BUILDER = new Builder();
   public static final ForgeConfigSpec SPEC;
   public static IntValue CAN_SPAWN_MIN;
   public static IntValue CAN_SPAWN_MAX;
   public static IntValue CAN_SPAWN_COOLDOWN;
   public static DoubleValue CAN_SPAWN_COOLDOWN_CHANCE;
   public static IntValue RESET_NOISE_MIN;
   public static IntValue RESET_NOISE_MAX;
   public static IntValue TIME_UNTIL_LEAVE;
   public static IntValue TIME_UNTIL_LEAVE_CHASE;
   public static DoubleValue SURFACE_TIMER_MULTIPLIER;
   public static DoubleValue SPAWN_CHANCE_PER_TICK;
   public static ConfigValue<Integer> SPAWN_HEIGHT;
   public static BooleanValue ALLOW_SURFACE_SPAWN;
   public static IntValue SKY_LIGHT_LEVEL;
   public static IntValue BLOCK_LIGHT_LEVEL;
   public static IntValue MAXIMUM_AMOUNT;
   public static IntValue SPAWN_DISTANCE;
   public static BooleanValue CHECK_PATH_TO_SPAWN;
   public static ConfigValue<List<? extends String>> DIMENSION_WHITELIST;
   public static BooleanValue OVERRIDE_BIOME_DATAPACK_CONFIG;
   public static BooleanValue SURFACE_BIOMES_IS_WHITELIST;
   public static ConfigValue<List<? extends String>> SURFACE_BIOMES;
   public static IntValue SPOTTING_RANGE;
   public static BooleanValue CAN_CLIMB;
   public static BooleanValue CAN_BREAK_DOOR;
   public static IntValue BREAK_DOOR_TIME;
   public static BooleanValue ALLOW_RIDING;
   public static BooleanValue TARGET_INVISIBLE;
   public static DoubleValue MAX_HEALTH;
   public static DoubleValue ATTACK_DAMAGE;
   public static DoubleValue ATTACK_SPEED;
   public static DoubleValue MOVEMENT_SPEED;
   public static DoubleValue DEPTH_STRIDER_BONUS;
   public static BooleanValue ONLY_PLAY_NOISE_TO_TARGET;

   private static boolean resourcePredicate(Object element) {
      if (element == null) {
         return false;
      } else if (element instanceof String) {
         String string = (String)element;
         return string.split(":").length == 2;
      } else if (element instanceof List) {
         List<?> list = (List)element;
         Iterator var2 = list.iterator();

         String string;
         do {
            if (!var2.hasNext()) {
               return true;
            }

            Object listElement = var2.next();
            if (!(listElement instanceof String)) {
               return false;
            }

            string = (String)listElement;
         } while(string.split(":").length == 2);

         return false;
      } else {
         return false;
      }
   }

   public static boolean isValidDimension(String key) {
      return ((List)DIMENSION_WHITELIST.get()).contains(key);
   }

   public static boolean isInValidBiome(Entity entity) {
      if (entity == null) {
         return false;
      } else {
         Level var2 = entity.getLevel();
         if (!(var2 instanceof ServerLevel)) {
            return false;
         } else {
            ServerLevel serverLevel = (ServerLevel)var2;
            Holder<Biome> biome = serverLevel.getBiome(entity.blockPosition());
            boolean isWhitelist = (Boolean)SURFACE_BIOMES_IS_WHITELIST.get();
            boolean isBiomeInList;
            if ((Boolean)OVERRIDE_BIOME_DATAPACK_CONFIG.get()) {
               ResourceLocation resource = ForgeRegistries.BIOMES.getKey((Biome)biome.get());
               isBiomeInList = resource != null && ((List)SURFACE_BIOMES.get()).contains(resource.toString());
            } else {
               isBiomeInList = biome.is(ModBiomeTagsProvider.CAVE_DWELLER_SURFACE_BIOMES);
            }

            return isWhitelist && isBiomeInList || !isWhitelist && !isBiomeInList;
         }
      }
   }

   static {
      BUILDER.push("Timers");
      BUILDER.push("Spawn");
      CAN_SPAWN_MIN = BUILDER.comment("Minimum time between spawns in seconds").defineInRange("can_spawn_min", 200, 0, 86400);
      CAN_SPAWN_MAX = BUILDER.comment("Maximum time between spawns in seconds").defineInRange("can_spawn_max", 500, 0, 86400);
      CAN_SPAWN_COOLDOWN_CHANCE = BUILDER.comment("Chance for a spawn cooldown to occur").defineInRange("can_spawn_cooldown_chance", 0.4D, 0.0D, 1.0D);
      CAN_SPAWN_COOLDOWN = BUILDER.comment("Spawn cooldown length in seconds").defineInRange("can_spawn_cooldown", 1000, 0, 86400);
      BUILDER.pop();
      BUILDER.push("Noise");
      RESET_NOISE_MIN = BUILDER.comment("Minimum time between noise occurrences in seconds").defineInRange("reset_noise_min", 220, 0, 86400);
      RESET_NOISE_MAX = BUILDER.comment("Maximum time between noise occurrences in seconds").defineInRange("reset_noise_max", 340, 0, 86400);
      BUILDER.pop();
      BUILDER.push("Leave");
      TIME_UNTIL_LEAVE = BUILDER.comment("Time (in seconds) it takes for the CaveDweller to leave").defineInRange("time_until_leave", 280, 1, 6000);
      TIME_UNTIL_LEAVE_CHASE = BUILDER.comment("Time (in seconds) it takes for the CaveDweller to leave once a chase begins").defineInRange("time_until_leave_chase", 30, 1, 600);
      BUILDER.pop();
      SURFACE_TIMER_MULTIPLIER = BUILDER.comment("Modify the timers for the surface (to reduce or increase them), based on the general timer set (e.g. spawn timer of 300 seconds -> 0.3 turns it into 90 seconds and 1.7 turns it into 510 seconds for the surface)").defineInRange("surface_timer_multiplier", 1.0D, 0.0D, 5.0D);
      BUILDER.pop();
      BUILDER.push("Spawn Conditions");
      SPAWN_CHANCE_PER_TICK = BUILDER.comment("The spawn chance per tick (once the spawn timer is finished)").defineInRange("spawn_chance_per_tick", 0.008D, 0.0D, 1.0D);
      SPAWN_HEIGHT = BUILDER.comment("Depth at which the CaveDweller can start to spawn").define("spawn_height", 50);
      ALLOW_SURFACE_SPAWN = BUILDER.comment("Whether the CaveDweller can spawn on the surface or not").define("allow_surface_spawn", false);
      SKY_LIGHT_LEVEL = BUILDER.comment("The maximum sky light level the CaveDweller can spawn at").defineInRange("sky_light_level", 15, 0, 15);
      BLOCK_LIGHT_LEVEL = BUILDER.comment("The maximum block light level the CaveDweller can spawn at").defineInRange("block_light_level", 15, 0, 15);
      MAXIMUM_AMOUNT = BUILDER.comment("The maximum amount of cave dwellers which can exist at the same time").defineInRange("maximum_amount", 3, 0, 100);
      SPAWN_DISTANCE = BUILDER.comment("How close to players the cave dweller is allowed to spawn (in blocks)").defineInRange("spawn_distance", 16, 0, 64);
      CHECK_PATH_TO_SPAWN = BUILDER.comment("If set to true the cave dweller will try to find a spawn position with a possible path to the player").define("check_path_to_spawn", true);
      BUILDER.push("Dimensions");
      DIMENSION_WHITELIST = BUILDER.comment("The dimensions where the CaveDweller can spawn in (Whitelist)").defineList("dimension_whitelist", List.of("minecraft:overworld"), ServerConfig::resourcePredicate);
      BUILDER.pop();
      BUILDER.push("Biomes");
      OVERRIDE_BIOME_DATAPACK_CONFIG = BUILDER.comment("If you don't want to create a datapack to configure the biomes").define("override_biome_datapack_config", false);
      SURFACE_BIOMES_IS_WHITELIST = BUILDER.comment("Use the surface biome list either as white- or blacklist").define("surface_biomes_is_whitelist", true);
      SURFACE_BIOMES = BUILDER.comment("Either white- or blacklist of the surface biomes the CaveDweller can spawn in (Syntax: modid:biome, e.g. `minecraft:plains`)").defineList("surface_biomes", List.of(), ServerConfig::resourcePredicate);
      BUILDER.pop();
      BUILDER.pop();
      BUILDER.push("Behaviour");
      SPOTTING_RANGE = BUILDER.comment("The distance in blocks at which the CaveDweller can detect whether a player is looking at it or not").defineInRange("spotting_range", 60, 0, 128);
      CAN_CLIMB = BUILDER.comment("Whether the cave dweller can climb or not").define("can_climb", false);
      CAN_BREAK_DOOR = BUILDER.comment("Whether the cave dweller can break down doors or not").define("can_break_door", true);
      BREAK_DOOR_TIME = BUILDER.comment("Time (in seconds) it takes the CaveDweller to break down a door").defineInRange("break_door_time", 3, 1, 60);
      ALLOW_RIDING = BUILDER.comment("Allow the CaveDweller to follow vanilla riding logic (e.g. boats)").define("allow_riding", false);
      TARGET_INVISIBLE = BUILDER.comment("Whether invisible players can be targets or not").define("target_invisible", true);
      BUILDER.pop();
      BUILDER.push("Attributes");
      MAX_HEALTH = BUILDER.comment("Maximum health").defineInRange("maximum_health", 60.0D, 1.0D, 100000.0D);
      ATTACK_DAMAGE = BUILDER.comment("Attack damage").defineInRange("attack_damage", 6.0D, 0.0D, 1000.0D);
      ATTACK_SPEED = BUILDER.comment("Attack speed").defineInRange("attack_speed", 0.35D, 0.0D, 10.0D);
      MOVEMENT_SPEED = BUILDER.comment("Movement speed").defineInRange("movement_speed", 0.5D, 0.0D, 5.0D);
      DEPTH_STRIDER_BONUS = BUILDER.comment("Depth Strider (movement speed in water) bonus").defineInRange("depth_strider_bonus", 1.5D, 0.0D, 3.0D);
      BUILDER.pop();
      BUILDER.push("Misc");
      ONLY_PLAY_NOISE_TO_TARGET = BUILDER.comment("Only play the ambient noises to the current (spawn) target (Note: The target can change when a spawn attempt is not successful)").define("only_play_noise_to_target", false);
      BUILDER.pop();
      SPEC = BUILDER.build();
   }
}
