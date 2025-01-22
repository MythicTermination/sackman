package de.cadentem.cave_dweller.entities.goals;

import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller.util.Utils;
import java.util.List;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

public class CaveDwellerTargetSeesMeGoal extends NearestAttackableTargetGoal<Player> {
   private final CaveDwellerEntity caveDweller;

   public CaveDwellerTargetSeesMeGoal(CaveDwellerEntity mob) {
      super(mob, Player.class, false);
      this.caveDweller = mob;
   }

   public boolean  canUse() {
      if (this.caveDweller.isInvisible()) {
         return false;
      } else {
         this.target = Utils.getValidTarget(this.caveDweller);
         return !Utils.isValidPlayer(this.target) ? false : this.caveDweller.isLookingAtMe(this.target, true);
      }
   }

   public void start() {
      this.caveDweller.setTarget(this.target);
      this.caveDweller.getEntityData().set(CaveDwellerEntity.SPOTTED_ACCESSOR, true);
      if (this.target != null) {
         this.caveDweller.pickRoll(List.of(Roll.CHASE, Roll.STARE, Roll.STARE, Roll.FLEE));
      }

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
