package de.cadentem.cave_dweller.mixin;

import java.util.Set;

import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PathNavigation.class})
public abstract class MixinPathNavigation {
   @Unique
   private boolean cave_dweller$wasCrawling;
   @Shadow
   @Final
   protected Mob f_26494_;

   @Inject(
      method = {"createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;"},
      at = {@At("HEAD")}
   )
   public void setCrawling_true(Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy, float followRange, CallbackInfoReturnable<Path> callback) {
      Mob var8 = this.f_26494_;
      if (var8 instanceof CaveDwellerEntity) {
         CaveDwellerEntity caveDweller = (CaveDwellerEntity)var8;
         this.cave_dweller$wasCrawling = caveDweller.isCrawling();
         caveDweller.setCrawling(true);
      }

   }

   @Inject(
      method = {"createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;"},
      at = {@At("RETURN")}
   )
   public void setCrawling_false(Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy, float followRange, CallbackInfoReturnable<Path> callback) {
      Mob var8 = this.f_26494_;
      if (var8 instanceof CaveDwellerEntity) {
         CaveDwellerEntity caveDweller = (CaveDwellerEntity)var8;
         caveDweller.setCrawling(this.cave_dweller$wasCrawling);
      }

   }
}
