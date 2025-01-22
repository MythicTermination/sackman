package de.cadentem.cave_dweller.entities.goals;

import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class CaveDwellerBreakInvisGoal extends Goal {
   private final CaveDwellerEntity caveDweller;

   public CaveDwellerBreakInvisGoal(CaveDwellerEntity caveDweller) {
      this.caveDweller = caveDweller;
   }

   public boolean  canUse() {
      return this.caveDweller.isInvisible() && !this.caveDweller.targetIsFacingMe;
   }

   public void start() {
      super.start();
      this.caveDweller.setInvisible(false);
   }
}
