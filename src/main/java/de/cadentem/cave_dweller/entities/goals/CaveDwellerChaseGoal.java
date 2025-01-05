package de.cadentem.cave_dweller.entities.goals;

import de.cadentem.cave_dweller.config.ServerConfig;
import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller.util.Utils;
import java.util.EnumSet;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

public class CaveDwellerChaseGoal extends Goal {
   private final CaveDwellerEntity caveDweller;
   private final int maxSpeedReached;
   private final boolean followTargetEvenIfNotSeen;
   private long lastGameTimeCheck;
   private int ticksUntilLeave;
   private int ticksUntilNextAttack;
   private int speedUp;
   public int sackcooldown = 0;
   public static int dismountcooldown = 0;

   public CaveDwellerChaseGoal(CaveDwellerEntity caveDweller, boolean followTargetEvenIfNotSeen) {
      this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      this.caveDweller = caveDweller;
      this.followTargetEvenIfNotSeen = followTargetEvenIfNotSeen;
      this.ticksUntilLeave = Utils.secondsToTicks((Integer)ServerConfig.TIME_UNTIL_LEAVE_CHASE.get());
      this.maxSpeedReached = Utils.secondsToTicks(3);
   }

   public static float updateRotation(float angle, float targetAngle, float maxIncrease) {
      float f = Mth.wrapDegrees(targetAngle - angle);
      if (f > maxIncrease) {
         f = maxIncrease;
      }

      if (f < -maxIncrease) {
         f = -maxIncrease;
      }

      return angle + f;
   }

   public boolean  canUse() {
      if (this.caveDweller.isInvisible()) {
         return false;
      } else if (this.caveDweller.currentRoll != Roll.CHASE) {
         return false;
      } else if (!this.caveDweller.targetIsFacingMe) {
         return false;
      } else {
         long ticks = this.caveDweller.level().getGameTime();
         if (ticks - this.lastGameTimeCheck < 20L) {
            return false;
         } else {
            this.lastGameTimeCheck = ticks;
            LivingEntity target = this.caveDweller.getTarget();
            if (!Utils.isValidPlayer(target)) {
               return false;
            } else {
               Path path = this.caveDweller.getNavigation().createPath(target, 0);
               if (path != null) {
                  return true;
               } else {
                  boolean canAttack = this.getAttackReachSqr(target) >= this.caveDweller.distanceToSqr(target);
                  if (canAttack) {
                     return true;
                  } else {
                     path = this.caveDweller.getNavigation().createPath(target, 0);
                     return path != null;
                  }
               }
            }
         }
      }
   }

   public boolean canContinueToUse() {
      LivingEntity target = this.caveDweller.getTarget();
      if (!Utils.isValidPlayer(target)) {
         this.caveDweller.disappear();
         return false;
      } else if (!this.followTargetEvenIfNotSeen) {
         return !this.caveDweller.getNavigation().isDone();
      } else {
         return this.caveDweller.isWithinRestriction(target.blockPosition());
      }
   }

   public void start() {
      this.caveDweller.setAggressive(true);
      this.ticksUntilNextAttack = 0;
   }

   public void stop() {
      LivingEntity target = this.caveDweller.getTarget();
      if (!Utils.isValidPlayer(target)) {
         this.caveDweller.setTarget((LivingEntity)null);
      }

      this.speedUp = 0;
      this.caveDweller.setAggressive(false);
      this.caveDweller.getEntityData().set(CaveDwellerEntity.CRAWLING_ACCESSOR, false);
      this.caveDweller.getNavigation().stop();
      this.caveDweller.refreshDimensions();
   }

   public boolean requiresUpdateEveryTick() {
      return true;
   }

   public static boolean canDismount() {
      return dismountcooldown <= 0;
   }

   public void tick() {
      LivingEntity target = this.caveDweller.getTarget();
      --this.sackcooldown;
      --dismountcooldown;
      System.out.println("dismounttick:" + dismountcooldown);
      double distance;
      if (this.caveDweller.hasPassenger(target)) {
         double dx = this.caveDweller.getX() + 30.0D;
         distance = this.caveDweller.getZ() + 30.0D;
         Path path = this.caveDweller.getNavigation().createPath(dx, 0.0D, distance, 0);
         this.fixPath(path);
         this.caveDweller.getNavigation().moveTo(path, this.caveDweller.getSpeedModifier());
         if (this.caveDweller.hurtMarked) {
            this.caveDweller.ejectPassengers();
         }
      } else {
         if (this.ticksUntilLeave <= 0 && !this.caveDweller.targetIsFacingMe) {
            this.caveDweller.disappear();
         }

         if (this.caveDweller.distanceTo(target) < 2.0F && this.sackcooldown <= 0) {
            this.sackcooldown = 300;
            dismountcooldown = 100;
            target.startRiding(this.caveDweller);
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 90));
         }

         if (!Utils.isValidPlayer(target)) {
            return;
         }

         Path path = this.caveDweller.getNavigation().getPath();
         this.fixPath(path);
         boolean targetMoved = path != null && path.getEndNode() != null && path.getEndNode().distanceTo(target.blockPosition()) > 2.0F;
         if (path == null || this.caveDweller.getNavigation().isStuck() || targetMoved || path.isDone() && !this.shouldClimb(path) || this.caveDweller.getNavigation().shouldRecomputePath(target.blockPosition()) && this.caveDweller.tickCount % 20 == 0) {
            path = this.caveDweller.getNavigation().createPath(target, 0);
            this.fixPath(path);
         }

         if (path != null && !path.isDone() && this.caveDweller.hasLineOfSight(target)) {
            this.caveDweller.playChaseSound();
         }

         this.caveDweller.getNavigation().moveTo(path, this.caveDweller.getSpeedModifier());
         if (!this.caveDweller.isCrawling()) {
            if (this.caveDweller.isAggressive()) {
               this.caveDweller.getLookControl().setLookAt(target, 90.0F, 90.0F);
            } else {
               this.caveDweller.getLookControl().setLookAt(target, 180.0F, 1.0F);
            }
         }

         this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
         distance = this.caveDweller.distanceToSqr(target);
         this.checkAndPerformAttack(target, distance);
         --this.ticksUntilLeave;
         if (this.speedUp < this.maxSpeedReached) {
            ++this.speedUp;
         }
      }

   }

   private void fixPath(Path path) {
      LivingEntity target = this.caveDweller.getTarget();
      if (target != null) {
         if (this.shouldClimb(path) && (double)path.getNode(0).distanceTo(this.caveDweller.getTarget().blockPosition()) > 0.1D) {
            path.replaceNode(0, path.getNode(0).cloneAndMove(target.blockPosition().getX(), target.blockPosition().getY(), target.blockPosition().getZ()));
         }

      }
   }

   private boolean shouldClimb(Path path) {
      if (this.caveDweller.getTarget() == null) {
         return false;
      } else {
         return path != null && path.getNodeCount() == 1 && (float)this.caveDweller.getTarget().blockPosition().getY() > (float)this.caveDweller.blockPosition().getY() + this.caveDweller.getStepHeight();
      }
   }

   private void checkAndPerformAttack(LivingEntity target, double distanceToTarget) {
      double attackReach = this.getAttackReachSqr(target);
      if (distanceToTarget <= attackReach && this.ticksUntilNextAttack <= 0) {
         this.resetAttackCooldown();
         this.caveDweller.swing(InteractionHand.MAIN_HAND);
         this.caveDweller.doHurtTarget(target);
      }

   }

   private void resetAttackCooldown() {
      this.ticksUntilNextAttack = this.adjustedTickDelay(20);
   }

   private double getAttackReachSqr(LivingEntity target) {
      return (double)(this.caveDweller.getBbWidth() * 4.0F * this.caveDweller.getBbWidth() * 4.0F + target.getBbWidth());
   }
}
