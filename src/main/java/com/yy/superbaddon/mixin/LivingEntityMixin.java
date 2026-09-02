package com.yy.superbaddon.mixin;

import com.yy.superbaddon.api.IBypassHurt;
import com.yy.superbaddon.compat.eca.EpicCoreApiBridge;
import com.yy.superbaddon.util.CopiedHurtCalculator;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements IBypassHurt {
    @Shadow
    @Final
    private static EntityDataAccessor<Float> DATA_HEALTH_ID;

    @Shadow
    public int hurtDuration;

    @Shadow
    public int hurtTime;

    @Shadow
    public abstract float getHealth();

    @Shadow
    public abstract float getMaxHealth();

    @Shadow
    protected abstract float getDamageAfterArmorAbsorb(DamageSource source, float amount);

    @Shadow
    protected abstract float getDamageAfterMagicAbsorb(DamageSource source, float amount);

    @Override
    public CopiedHurtCalculator.Result superbaddon$calculateCopiedHurt(
            DamageSource source,
            float amount,
            float penetration
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        return CopiedHurtCalculator.calculate(
                self,
                source,
                amount,
                penetration,
                this::getDamageAfterArmorAbsorb,
                this::getDamageAfterMagicAbsorb
        );
    }

    @Override
    public boolean superbaddon$setHealthHurtBypass(DamageSource source, float amount) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (self.level().isClientSide || self.isDeadOrDying()) {
            return false;
        }

        float oldHealth = this.getHealth();
        float newHealth = Mth.clamp(oldHealth - amount, 0.0F, this.getMaxHealth());

        if (!EpicCoreApiBridge.setHealth(self, newHealth)) {
            self.setHealth(newHealth);
        }

        this.hurtDuration = 10;
        this.hurtTime = this.hurtDuration;

        return true;
    }
}
