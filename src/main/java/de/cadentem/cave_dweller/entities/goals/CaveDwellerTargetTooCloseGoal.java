package de.cadentem.cave_dweller.entities.goals;

import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller.util.Utils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

public class CaveDwellerTargetTooCloseGoal extends NearestAttackableTargetGoal<Player> {
   private final CaveDwellerEntity caveDweller;
   private final float distanceThreshold;

   public CaveDwellerTargetTooCloseGoal(CaveDwellerEntity mob, float distanceThreshold) {
      super(mob, Player.class, false);
      this.caveDweller = mob;
      this.distanceThreshold = distanceThreshold;
   }

   public boolean canUse() {
      if (!this.caveDweller.isInvisible()) {
         LivingEntity target = this.caveDweller.level.getNearestPlayer(this.caveDweller, (double)this.distanceThreshold);
         if (Utils.isValidPlayer(target)) {
            this.target = target;
            return true;
         }
      }

      return false;
   }

   public void start() {
      this.caveDweller.setAggressive(true);
      this.caveDweller.currentRoll = Roll.CHASE;
      this.caveDweller.setTarget(this.target);
      super.start();
   }

   public void stop() {
      super.stop();
   }

   public boolean canContinueToUse() {
      return Utils.isValidPlayer(this.target);
   }

   public void tick() {
      super.tick();
   }
}
