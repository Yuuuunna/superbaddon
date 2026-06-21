package com.yy.superbaddon.mixin;

import com.yy.superbaddon.content.ContentControlManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void superbaddon$skipBlockedVehicleRender(E entity,
                                                                        double x,
                                                                        double y,
                                                                        double z,
                                                                        float rotationYaw,
                                                                        float partialTicks,
                                                                        PoseStack poseStack,
                                                                        MultiBufferSource buffer,
                                                                        int packedLight,
                                                                        CallbackInfo ci) {
        if (entity == null) return;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (ContentControlManager.blocksVehicleEntity(id)) ci.cancel();
    }
}
