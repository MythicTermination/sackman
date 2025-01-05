package de.cadentem.cave_dweller.entities.goals;

import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

public class CaveDwellerStrollGoal extends WaterAvoidingRandomStrollGoal {
   public CaveDwellerStrollGoal(CaveDwellerEntity mob, double speedModifier) {
      super(mob, speedModifier);
   }

   public boolean  canUse() {
      return ((CaveDwellerEntity)this.mob).currentRoll == Roll.STROLL && super. canUse();
   }

   public boolean canContinueToUse() {
      return ((CaveDwellerEntity)this.mob).currentRoll == Roll.STROLL && super.canContinueToUse();
   }
}
