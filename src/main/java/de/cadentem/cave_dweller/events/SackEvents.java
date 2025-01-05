package de.cadentem.cave_dweller.events;

import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller.entities.goals.CaveDwellerChaseGoal;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent.Key;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "cave_dweller",
   value = {Dist.CLIENT}
)
public class SackEvents {
   @SubscribeEvent
   public static void keyPress(Key event) {
      System.out.println("SackEvent:" + CaveDwellerChaseGoal.dismountcooldown);
      System.out.println("canDismount:" + CaveDwellerChaseGoal.canDismount());
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.options.keyShift.isDown() && minecraft.player.getVehicle() instanceof CaveDwellerEntity) {
         if (!CaveDwellerChaseGoal.canDismount()) {
            minecraft.options.keyShift.setDown(false);
         } else {
            minecraft.options.keyShift.setDown(true);
         }
      }

   }
}
