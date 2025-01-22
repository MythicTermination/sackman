package de.cadentem.cave_dweller.util;

import de.cadentem.cave_dweller.config.ServerConfig;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;

public class Timer {
   @Nullable
   public Entity currentVictim;
   public int currentSpawn;
   public int currentNoise;
   public int targetSpawn;
   public int targetNoise;

   public Timer() {
      this.resetSpawnTimer();
      this.resetNoiseTimer();
   }

   public Timer(@Nullable Entity currentVictim) {
      this.currentVictim = currentVictim;
   }

   public boolean isSpawnTimerReached() {
      if (Utils.isOnSurface(this.currentVictim)) {
         return this.currentSpawn >= (int)((double)this.targetSpawn * (Double)ServerConfig.SURFACE_TIMER_MULTIPLIER.get());
      } else {
         return this.currentSpawn >= this.targetSpawn;
      }
   }

   public boolean isNoiseTimerReached() {
      if (Utils.isOnSurface(this.currentVictim)) {
         return this.currentNoise >= (int)((double)this.targetNoise * (Double)ServerConfig.SURFACE_TIMER_MULTIPLIER.get());
      } else {
         return this.currentNoise >= this.targetNoise;
      }
   }

   public void resetNoiseTimer() {
      int min = (Integer)ServerConfig.RESET_NOISE_MIN.get();
      int max = (Integer)ServerConfig.RESET_NOISE_MAX.get();
      int noiseTimer;
      if (max < min) {
         noiseTimer = min;
         min = max;
         max = noiseTimer;
         de.cadentem.cave_dweller.CaveDweller.LOG.error("Configuration for `RESET_NOISE` was wrong - max [{}] was smaller than min [{}] - values have been switched to prevent a crash", noiseTimer, min);
      }

      noiseTimer = de.cadentem.cave_dweller.CaveDweller.RANDOM.nextInt(Utils.secondsToTicks(min), Utils.secondsToTicks(max + 1));
      this.currentNoise = 0;
      this.targetNoise = noiseTimer;
   }

   public void resetSpawnTimer() {
      int spawnTimer;
      if (de.cadentem.cave_dweller.CaveDweller.RANDOM.nextDouble() <= (Double)ServerConfig.CAN_SPAWN_COOLDOWN_CHANCE.get()) {
         spawnTimer = Utils.secondsToTicks((Integer)ServerConfig.CAN_SPAWN_COOLDOWN.get());
      } else {
         int min = (Integer)ServerConfig.CAN_SPAWN_MIN.get();
         int max = (Integer)ServerConfig.CAN_SPAWN_MAX.get();
         if (max < min) {
            int temp = min;
            min = max;
            max = temp;
            de.cadentem.cave_dweller.CaveDweller.LOG.error("Configuration for `RESET_CALM` was wrong - max [{}] was smaller than min [{}] - values have been switched to prevent a crash", temp, min);
         }

         spawnTimer = de.cadentem.cave_dweller.CaveDweller.RANDOM.nextInt(Utils.secondsToTicks(min), Utils.secondsToTicks(max + 1));
      }

      this.currentSpawn = 0;
      this.targetSpawn = spawnTimer;
   }

   public String toString() {
      String name = this.currentVictim != null ? this.currentVictim.getName().getString() : "NONE";
      return name + " | " + this.currentSpawn + "/" + this.targetSpawn + " | " + this.currentNoise + "/" + this.targetNoise;
   }
}
