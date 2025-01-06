package de.cadentem.cave_dweller.entities.goals;

import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller.util.Utils;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class CaveDwellerStareGoal extends Goal {
   private final CaveDwellerEntity caveDweller;
   private boolean wasNotLookingPreviously;
   private int lookedAtCount;
   private final int lookedAtMax;

   public CaveDwellerStareGoal(CaveDwellerEntity caveDweller) {
      this.caveDweller = caveDweller;
      this.lookedAtMax = caveDweller.getRandom().nextIntBetweenInclusive(8, 15);
   }

   public boolean  canUse() {
      if (this.caveDweller.isInvisible()) {
         return false;
      } else if (!Utils.isValidPlayer(this.caveDweller.getTarget())) {
         return false;
      } else {
         return this.caveDweller.currentRoll == Roll.STARE;
      }
   }

   public boolean canContinueToUse() {
      if (!Utils.isValidPlayer(this.caveDweller.getTarget())) {
         return false;
      } else {
         return this.caveDweller.currentRoll == Roll.STARE;
      }
   }

   public boolean requiresUpdateEveryTick() {
      return true;
   }

   public void stop() {
      super.stop();
      this.lookedAtCount = 0;
      this.wasNotLookingPreviously = false;
      this.caveDweller.pleaseStopMoving = false;
      this.caveDweller.getEntityData().set(CaveDwellerEntity.SPOTTED_ACCESSOR, false);
   }

   public void tick() {
      LivingEntity target = this.caveDweller.getTarget();
      if (target == null) {
         this.caveDweller.disappear();
      } else {
         boolean actuallyLooking = this.caveDweller.targetIsFacingMe && target.hasLineOfSight(this.caveDweller);
         if (this.wasNotLookingPreviously && actuallyLooking) {
            ++this.lookedAtCount;
         }

         if (this.lookedAtCount > this.lookedAtMax && !actuallyLooking) {
            if (this.caveDweller.getRandom().nextDouble() < 0.1D) {
               this.caveDweller.disappear();
            } else if (this.caveDweller.getRandom().nextDouble() < 0.3D) {
               this.caveDweller.pickRoll(List.of(Roll.CHASE, Roll.FLEE));
            }
         }

         if (!actuallyLooking) {
            this.caveDweller.pleaseStopMoving = false;
            this.caveDweller.getNavigation().moveTo(target, 1.0D);
         } else {
            this.caveDweller.pleaseStopMoving = true;
            this.caveDweller.getNavigation().stop();
            this.caveDweller.setDeltaMovement(Vec3.ZERO);
         }

         this.caveDweller.getLookControl().setLookAt(target);
         this.wasNotLookingPreviously = !actuallyLooking;
      }
   }
}
