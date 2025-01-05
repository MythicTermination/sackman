package de.cadentem.cave_dweller.util;

import de.cadentem.cave_dweller.config.ServerConfig;

import java.util.Optional;

import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.SpawnUtil;
import net.minecraft.util.SpawnUtil.Strategy;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.common.Tags.Biomes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Utils {
   public static int ticksToSeconds(int ticks) {
      return ticks / 20;
   }

   public static int secondsToTicks(int seconds) {
      return seconds * 20;
   }

   public static int minutesToTicks(int minutes) {
      return secondsToTicks(minutes * 60);
   }

   public static String getTextureAppend() {
      return "";
   }

   public static boolean isValidPlayer(Entity entity) {
      if (!(entity instanceof Player)) {
         return false;
      } else {
         Player player = (Player)entity;
         if (!player.isAlive()) {
            return false;
         } else if (!(Boolean)ServerConfig.TARGET_INVISIBLE.get() && player.isInvisible()) {
            return false;
         } else {
            return !player.isCreative() && !player.isSpectator();
         }
      }
   }

   public static LivingEntity getValidTarget(@NotNull CaveDwellerEntity caveDweller) {
      return caveDweller.level.getNearestPlayer(caveDweller.position().x, caveDweller.position().y, caveDweller.position().z, 128.0D, Utils::isValidPlayer);
   }

   public static boolean isOnSurface(@Nullable Entity entity) {
      if (entity == null) {
         return false;
      } else {
         Level var2 = entity.getLevel();
         if (var2 instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)var2;
            BlockPos blockPosition = entity.blockPosition();
            if (serverLevel.canSeeSky(blockPosition)) {
               return true;
            }

            Holder<Biome> biome = serverLevel.getBiome(blockPosition);
            if (biome.is(Biomes.IS_CAVE) || biome.is(Biomes.IS_UNDERGROUND)) {
               return false;
            }

            int baseSkyLightLevel = serverLevel.getBrightness(LightLayer.SKY, blockPosition) - serverLevel.getSkyDarken();
            if (baseSkyLightLevel > 0) {
               return true;
            }
         }

         return false;
      }
   }

   public static <T extends Mob> Optional<T> trySpawnMob(@NotNull Entity currentVictim, EntityType<T> entityType, MobSpawnType spawnType, ServerLevel level, BlockPos blockPosition, int attempts, int xzOffset, int yOffset, Strategy strategy) {
      MutableBlockPos mutableBlockPosition = blockPosition.mutable();

      for(int i = 0; i < attempts; ++i) {
         int xOffset = Mth.randomBetweenInclusive(level.random, -xzOffset, xzOffset);
         int zOffset = Mth.randomBetweenInclusive(level.random, -xzOffset, xzOffset);
         mutableBlockPosition.setWithOffset(blockPosition, xOffset, yOffset, zOffset);
         if (level.getWorldBorder().isWithinBounds(mutableBlockPosition) && SpawnUtil.moveToPossibleSpawnPosition(level, yOffset, mutableBlockPosition, strategy)) {
            T entity = (T)entityType.create(level, (CompoundTag)null, (Component)null, (Player)null, mutableBlockPosition, spawnType, false, false);
            if (entity instanceof CaveDwellerEntity) {
               if (entity.checkSpawnRules(level, spawnType) && entity.checkSpawnObstruction(level)) {
                  boolean isValidSpawn = entity.level.getNearestPlayer(entity, (double)(Integer)ServerConfig.SPAWN_DISTANCE.get()) == null;
                  if (isValidSpawn && (Boolean)ServerConfig.CHECK_PATH_TO_SPAWN.get()) {
                     Path path = entity.getNavigation().createPath(currentVictim, 0);
                     isValidSpawn = path != null && path.canReach();
                  }

                  if (isValidSpawn) {
                     entity.getNavigation().createPath(entity.blockPosition(), 0);
                     entity.getNavigation().stop();
                     level.addFreshEntityWithPassengers(entity);
                     return Optional.of(entity);
                  }
               }

               entity.discard();
            }
         }
      }

      de.cadentem.cave_dweller.CaveDweller.LOG.debug("Cave Dweller could not pass the spawn checks, target: [{}]", currentVictim);
      return Optional.empty();
   }
}
