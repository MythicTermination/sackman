package de.cadentem.cave_dweller.entities.goals;

import de.cadentem.cave_dweller.config.ServerConfig;
import de.cadentem.cave_dweller.util.Utils;
import java.util.function.Predicate;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;

public class CaveDwellerBreakDoorGoal extends BreakDoorGoal {
   public CaveDwellerBreakDoorGoal(Mob mob, Predicate<Difficulty> validDifficulties) {
      super(mob, validDifficulties);
   }

   protected int m_25100_() {
      return Utils.secondsToTicks((Integer)ServerConfig.BREAK_DOOR_TIME.get());
   }
}
