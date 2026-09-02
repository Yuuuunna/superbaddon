package com.yy.superbaddon.mixin;

import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.yy.superbaddon.knockback.KnockbackCarrier;
import com.yy.superbaddon.knockback.KnockbackRuntime;
import com.yy.superbaddon.knockback.KnockbackSpec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ProjectileEntity.class, remap = false)
public abstract class ProjectileEntityMixin implements KnockbackCarrier {
    @Unique
    private KnockbackSpec superbaddon$knockbackSpec = KnockbackSpec.inherit();

    @Override
    public void superbaddon$setKnockbackSpec(KnockbackSpec spec) {
        this.superbaddon$knockbackSpec = spec == null ? KnockbackSpec.inherit() : spec;
    }

    @Override
    public KnockbackSpec superbaddon$getKnockbackSpec() {
        return this.superbaddon$knockbackSpec;
    }

    @Inject(
            method = "setGunItemId(Lnet/minecraft/world/item/ItemStack;)Lcom/atsuishio/superbwarfare/entity/projectile/ProjectileEntity;",
            at = @At("HEAD"),
            remap = false
    )
    private void superbaddon$attachKnockbackProfile(ItemStack stack, CallbackInfoReturnable<ProjectileEntity> cir) {
        KnockbackRuntime.attachToProjectile((Entity) (Object) this);
    }

    /**
     * {@code onHitEntity(EntityHitResult)} 是 Minecraft {@code Projectile} 的重写方法，
     * 生产环境里它的名字是 SRG 名 {@code m_5790_}，只有开发环境的 deobf 依赖才叫 {@code onHitEntity}。
     * 本 mixin 类是 {@code remap = false}（目标是主模组类），所以方法名不会被重映射——
     * 必须像 {@link CustomExplosionMixin} 那样把两个名字都列出来，否则装了正式版主模组就会
     * "could not find any targets matching 'onHitEntity'" 直接崩服/崩客户端。
     * {@code performOnHit} 是主模组自己的方法，名字不混淆，@At 保持 remap = false。
     */
    @ModifyArg(
            method = {
                    "onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V",
                    "m_5790_(Lnet/minecraft/world/phys/EntityHitResult;)V"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/atsuishio/superbwarfare/entity/projectile/ProjectileEntity;performOnHit(Lnet/minecraft/world/entity/Entity;FZD)V"
            ),
            index = 3,
            require = 1,
            remap = false
    )
    private double superbaddon$modifyDirectKnockback(double original) {
        return this.superbaddon$knockbackSpec.direct().apply(original);
    }
}
