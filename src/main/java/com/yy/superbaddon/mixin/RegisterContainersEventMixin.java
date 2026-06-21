package com.yy.superbaddon.mixin;

import com.yy.superbaddon.content.ContentControlManager;
import com.atsuishio.superbwarfare.api.event.RegisterContainersEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// remap=false: RegisterContainersEvent 是主模组(非 Minecraft)的类，方法名不会被混淆，
// Mixin 必须按原名 add(...) 引用，否则编译期找不到混淆映射、运行期(reobf)也匹配不到目标。
@Mixin(value = RegisterContainersEvent.class, remap = false)
public abstract class RegisterContainersEventMixin {
    @Inject(method = "add(Lnet/minecraft/world/entity/EntityType;)V", at = @At("HEAD"), cancellable = true)
    private <T extends Entity> void superbaddon$skipBlockedContainer(EntityType<T> type, CallbackInfo ci) {
        if (blocks(type)) ci.cancel();
    }

    @Inject(method = "add(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void superbaddon$skipBlockedContainer(Entity entity, CallbackInfo ci) {
        if (entity != null && blocks(entity.getType())) ci.cancel();
    }

    private static boolean blocks(EntityType<?> type) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        return ContentControlManager.blocksVehicleEntity(id);
    }
}
