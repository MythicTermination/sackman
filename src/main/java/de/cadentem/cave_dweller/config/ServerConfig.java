package de.cadentem.cave_dweller.config;

import de.cadentem.cave_dweller.datagen.ModBiomeTagsProvider;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Optional;

public class ServerConfig {
   public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
   public static final ForgeConfigSpec SPEC;

   // Timers
   // Spawn
   public static ForgeConfigSpec.IntValue CAN_SPAWN_MIN;
   public static ForgeConfigSpec.IntValue CAN_SPAWN_MAX;
   public static ForgeConfigSpec.IntValue CAN_SPAWN_COOLDOWN;
   public static ForgeConfigSpec.DoubleValue CAN_SPAWN_COOLDOWN_CHANCE;
   // Noise
   public static ForgeConfigSpec.IntValue RESET_NOISE_MIN;
   public static ForgeConfigSpec.IntValue RESET_NOISE_MAX;
   // Leave
   public static ForgeConfigSpec.IntValue TIME_UNTIL_LEAVE;
   public static ForgeConfigSpec.IntValue TIME_UNTIL_LEAVE_CHASE;
   // Other
   public static ForgeConfigSpec.DoubleValue SURFACE_TIMER_MULTIPLIER;

   // Spawn Conditions
   public static ForgeConfigSpec.DoubleValue SPAWN_CHANCE_PER_TICK;
   public static ForgeConfigSpec.ConfigValue<Integer> SPAWN_HEIGHT;
   public static ForgeConfigSpec.BooleanValue ALLOW_SURFACE_SPAWN;
   public static ForgeConfigSpec.IntValue SKY_LIGHT_LEVEL;
   public static ForgeConfigSpec.IntValue BLOCK_LIGHT_LEVEL;
   public static ForgeConfigSpec.IntValue MAXIMUM_AMOUNT;
   public static ForgeConfigSpec.IntValue SPAWN_DISTANCE;
   public static ForgeConfigSpec.BooleanValue CHECK_PATH_TO_SPAWN;
   // Dimensions
   public static ForgeConfigSpec.ConfigValue<List<? extends String>> DIMENSION_WHITELIST;
   // Biomes
   public static ForgeConfigSpec.BooleanValue OVERRIDE_BIOME_DATAPACK_CONFIG;
   public static ForgeConfigSpec.BooleanValue SURFACE_BIOMES_IS_WHITELIST;
   public static ForgeConfigSpec.ConfigValue<List<? extends String>> SURFACE_BIOMES;

   // Behaviour
   public static ForgeConfigSpec.IntValue SPOTTING_RANGE;
   public static ForgeConfigSpec.BooleanValue CAN_DISABLE_SHIELDS;
   public static ForgeConfigSpec.BooleanValue CAN_CLIMB;
   public static ForgeConfigSpec.BooleanValue CAN_BREAK_DOOR;
   public static ForgeConfigSpec.IntValue BREAK_DOOR_TIME;
   public static ForgeConfigSpec.BooleanValue ALLOW_RIDING;
   public static ForgeConfigSpec.BooleanValue TARGET_INVISIBLE;

   // Attributes
   public static ForgeConfigSpec.DoubleValue MAX_HEALTH;
   public static ForgeConfigSpec.DoubleValue ATTACK_DAMAGE;
   public static ForgeConfigSpec.DoubleValue ATTACK_SPEED;
   public static ForgeConfigSpec.DoubleValue MOVEMENT_SPEED;
   public static ForgeConfigSpec.DoubleValue DEPTH_STRIDER_BONUS;

   // Misc
   public static ForgeConfigSpec.BooleanValue ONLY_PLAY_NOISE_TO_TARGET;

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

   private static boolean resourcePredicate(final Object element) {
      if (element == null) {
         return false;
      }

      if (element instanceof String string) {
         return string.split(":").length == 2;
      }

      if (element instanceof List<?> list) {
         for (Object listElement : list) {
            if (listElement instanceof String string) {
               if (!(string.split(":").length == 2)) {
                  return false;
               }
            } else {
               return false;
            }
         }

         return true;
      }

      return false;
   }

   public static boolean isValidDimension(final String key) {
      return DIMENSION_WHITELIST.get().contains(key);
   }

   public static boolean isInValidBiome(final Entity entity) {
      if (entity == null) {
         return false;
      }

      if (entity.level() instanceof ServerLevel serverLevel) {
         Holder<Biome> biome = serverLevel.getBiome(entity.blockPosition());

         boolean isWhitelist = SURFACE_BIOMES_IS_WHITELIST.get();
         boolean isBiomeInList = false;

         if (OVERRIDE_BIOME_DATAPACK_CONFIG.get()) {
            Optional<ResourceKey<Biome>> unwrapped = biome.unwrapKey();

            if (unwrapped.isPresent()) {
               isBiomeInList = SURFACE_BIOMES.get().contains(unwrapped.get().location().toString());
            }
         } else {
            isBiomeInList = biome.is(ModBiomeTagsProvider.CAVE_DWELLER_SURFACE_BIOMES);
         }

         return (/* Whitelist */ isWhitelist && isBiomeInList) || (/* Blacklist */ !isWhitelist && !isBiomeInList);
      }

      return false;
   }
}