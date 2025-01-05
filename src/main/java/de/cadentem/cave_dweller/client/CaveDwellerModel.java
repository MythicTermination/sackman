package de.cadentem.cave_dweller.client;

import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller.util.Utils;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.model.provider.data.EntityModelData;

public class CaveDwellerModel extends AnimatedGeoModel<CaveDwellerEntity> {
   public ResourceLocation getModelResource(CaveDwellerEntity ignored) {
      return new ResourceLocation("cave_dweller", "geo/cave_dweller.geo" + Utils.getTextureAppend() + ".json");
   }

   public ResourceLocation getTextureResource(CaveDwellerEntity ignored) {
      return new ResourceLocation("cave_dweller", "textures/entity/cave_dweller_texture" + Utils.getTextureAppend() + ".png");
   }

   public ResourceLocation getAnimationResource(CaveDwellerEntity ignored) {
      return new ResourceLocation("cave_dweller", "animations/cave_dweller.animation.json");
   }

   public void setCustomAnimations(CaveDwellerEntity animatable, int instanceId, AnimationEvent animationEvent) {
      IBone head = this.getAnimationProcessor().getBone("head");
      if (head != null) {
         EntityModelData entityData = (EntityModelData)animationEvent.getExtraDataOfType(EntityModelData.class).get(0);
         head.setRotationX(entityData.headPitch * 0.017453292F);
         head.setRotationY(entityData.netHeadYaw * 0.017453292F);
      }

      super.setCustomAnimations(animatable, instanceId, animationEvent);
   }
}
