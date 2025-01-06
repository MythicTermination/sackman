package de.cadentem.cave_dweller.mixin;

import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({GroundPathNavigation.class})
public abstract class MixinGroundPathNavigation extends PathNavigation {
   public MixinGroundPathNavigation(Mob mob, Level level) {
      super(mob, level);
   }

   @Inject(
      method = {"canUpdatePath"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void canUpdateWhenClimbing(CallbackInfoReturnable<Boolean> cir) {
      Mob var3 = this.mob;
      if (var3 instanceof CaveDwellerEntity) {
         CaveDwellerEntity caveDweller = (CaveDwellerEntity)var3;
         if (!(Boolean)cir.getReturnValue() && (!caveDweller.hasSpawned() || (Boolean)caveDweller.getEntityData().get(CaveDwellerEntity.CRAWLING_ACCESSOR))) {
            cir.setReturnValue(true);
         }
      }

   }
}
