package de.cadentem.cave_dweller.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller.util.Utils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CaveDwellerRenderer extends GeoEntityRenderer<CaveDwellerEntity> {
   public CaveDwellerRenderer(final EntityRendererProvider.Context context) {
      super(context, new CaveDwellerModel());
      this.shadowRadius = 0.3F;

      addRenderLayer(new CaveDwellerEyesLayer(this));
   }

   @NotNull
   public ResourceLocation getTextureLocation(@NotNull CaveDwellerEntity instance) {
      return new ResourceLocation("cave_dweller", "textures/entity/cave_dweller_texture" + Utils.getTextureAppend() + ".png");
   }

   public void render(CaveDwellerEntity entity, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
      poseStack.pushPose();
      if (entity.isBaby()) {
         poseStack.scale(0.1F, 0.1F, 0.1F);
      } else {
         poseStack.
      scale(1.3F, 1.3F, 1.3F);
      }

      super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
      poseStack.popPose();
   }
}
