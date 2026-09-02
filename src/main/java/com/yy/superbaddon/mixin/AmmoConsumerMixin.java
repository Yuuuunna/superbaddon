package com.yy.superbaddon.mixin;

import com.yy.superbaddon.compat.mts.MtsBulletAmmo;
import com.yy.superbaddon.compat.mts.MtsCompatConfig;
import com.atsuishio.superbwarfare.data.gun.AmmoConsumer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Teaches SuperbWarfare's {@link AmmoConsumer} to speak MinecraftTransportSimulator bullet rounds.
 *
 * <p>An {@code ITEM} consumer assumes "one item count = one round" and matches by full NBT, but an
 * MTS bullet packs its round count into the {@code bulletQty} NBT and one item stack equals many
 * rounds.  When (and only when) this consumer's configured item is detected as an MTS bullet, these
 * injectors reroute matching / counting / consuming / withdrawing through {@link MtsBulletAmmo}.
 * For every other consumer the original SuperbWarfare logic runs untouched.</p>
 *
 * <p>{@code remap = false} because the target is a mod class (SuperbWarfare), not a Minecraft class.</p>
 */
@Mixin(value = AmmoConsumer.class, remap = false)
public abstract class AmmoConsumerMixin {

    // Kotlin backing fields on AmmoConsumer.  No public setter exists for stack/initialized, so shadow
    // them directly; type/loadAmount are set through their public setters via the cast below.
    @Shadow
    private ItemStack stack;

    @Shadow
    private boolean initialized;

    /**
     * SuperbWarfare's {@code init()} rejects MTS ids because its regex forbids the dot in
     * {@code packID.systemName}.  Detect that case first and wire the consumer up as a plain ITEM
     * pointing at the MTS bullet, so guns and vehicle weapons alike end up with a usable {@code stack}.
     */
    @Inject(method = "init", at = @At("HEAD"), cancellable = true, remap = false)
    private void superbaddon$initMtsBullet(CallbackInfo ci) {
        if (!MtsCompatConfig.enabled()) return;

        AmmoConsumer self = (AmmoConsumer) (Object) this;
        Item item = MtsBulletAmmo.resolveBulletItem(self.getAmmo());
        if (item == null) return;

        this.stack = new ItemStack(item);
        self.setType(AmmoConsumer.AmmoConsumeType.ITEM);
        self.setLoadAmount(1);
        this.initialized = true;
        ci.cancel();
    }

    private boolean superbaddon$isMtsBulletConsumer() {
        if (!MtsCompatConfig.enabled()) return false;
        AmmoConsumer self = (AmmoConsumer) (Object) this;
        return MtsBulletAmmo.isBullet(self.stack());
    }

    /**
     * Injection order makes these HEAD hooks see a freshly built consumer before upstream's lazy
     * {@code init()} has run, when the MTS stack is still empty.  Running init here (the init
     * injector intercepts it for MTS consumers) closes that first-call window; for regular
     * consumers this just runs upstream init a moment earlier, which is harmless.
     */
    private boolean superbaddon$ensureMtsReady() {
        AmmoConsumer self = (AmmoConsumer) (Object) this;
        if (!this.initialized) {
            self.init();
        }
        return superbaddon$isMtsBulletConsumer();
    }

    private Item superbaddon$bulletItem() {
        return ((AmmoConsumer) (Object) this).stack().getItem();
    }

    @Inject(method = "isAmmoItem", at = @At("HEAD"), cancellable = true, remap = false)
    private void superbaddon$matchMtsBullet(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!superbaddon$isMtsBulletConsumer()) return;
        // Match by item identity, ignoring the per-stack bulletQty NBT that the vanilla path compares.
        cir.setReturnValue(stack != null && !stack.isEmpty() && stack.getItem() == superbaddon$bulletItem());
    }

    @Inject(method = "count(Lcom/atsuishio/superbwarfare/data/gun/GunData;Lnet/minecraftforge/items/IItemHandler;)I",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void superbaddon$countMtsBullet(GunData data, IItemHandler handler, CallbackInfoReturnable<Integer> cir) {
        if (!superbaddon$ensureMtsReady()) return;
        cir.setReturnValue(MtsBulletAmmo.countQty(handler, superbaddon$bulletItem()));
    }

    @Inject(method = "consume(Lcom/atsuishio/superbwarfare/data/gun/GunData;Lnet/minecraftforge/items/IItemHandler;I)I",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void superbaddon$consumeMtsBullet(GunData data, IItemHandler handler, int count, CallbackInfoReturnable<Integer> cir) {
        if (!superbaddon$ensureMtsReady()) return;
        cir.setReturnValue(MtsBulletAmmo.consumeQty(handler, superbaddon$bulletItem(), count));
    }

    /**
     * 0.8.9.1's strategy rewrite routes the entity overloads through AmmoConsumeStrategy instead of
     * delegating to the handler overloads, so the MTS reroute needs its own hooks on this path.
     * Upstream resolves the same {@code ITEM_HANDLER} capability for entities (ItemAmmoStrategy),
     * so counting/consuming rounds through {@link MtsBulletAmmo#handlerOf} stays equivalent.
     */
    @Inject(method = "count(Lcom/atsuishio/superbwarfare/data/gun/GunData;Lnet/minecraft/world/entity/Entity;)I",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void superbaddon$countMtsBulletEntity(GunData data, Entity entity, CallbackInfoReturnable<Integer> cir) {
        if (!superbaddon$ensureMtsReady()) return;
        // Cancelling at HEAD also skips upstream's null guard — replicate it.
        if (entity == null) {
            cir.setReturnValue(0);
            return;
        }
        IItemHandler handler = MtsBulletAmmo.handlerOf(entity);
        cir.setReturnValue(handler == null ? 0 : MtsBulletAmmo.countQty(handler, superbaddon$bulletItem()));
    }

    @Inject(method = "consume(Lcom/atsuishio/superbwarfare/data/gun/GunData;Lnet/minecraft/world/entity/Entity;I)I",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void superbaddon$consumeMtsBulletEntity(GunData data, Entity shooter, int count, CallbackInfoReturnable<Integer> cir) {
        if (!superbaddon$ensureMtsReady()) return;
        // Cancelling at HEAD also skips upstream's early returns — replicate them
        // (zero counts are no-ops and creative players never pay).
        if (count <= 0 || shooter instanceof Player player && player.isCreative()) {
            cir.setReturnValue(0);
            return;
        }
        IItemHandler handler = MtsBulletAmmo.handlerOf(shooter);
        cir.setReturnValue(handler == null ? 0 : MtsBulletAmmo.consumeQty(handler, superbaddon$bulletItem(), count));
    }

    @Inject(method = "withdraw(Lnet/minecraftforge/items/IItemHandler;I)I",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void superbaddon$withdrawMtsBulletHandler(IItemHandler handler, int count, CallbackInfoReturnable<Integer> cir) {
        if (!superbaddon$ensureMtsReady()) return;
        cir.setReturnValue(MtsBulletAmmo.insertQty(handler, superbaddon$bulletItem(), count));
    }

    @Inject(method = "withdraw(Lnet/minecraft/world/entity/Entity;I)I",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void superbaddon$withdrawMtsBulletEntity(Entity ammoSupplier, int count, CallbackInfoReturnable<Integer> cir) {
        if (!superbaddon$ensureMtsReady()) return;
        Item item = superbaddon$bulletItem();
        if (ammoSupplier instanceof Player player) {
            cir.setReturnValue(MtsBulletAmmo.insertQty(player, item, count));
        } else {
            IItemHandler handler = MtsBulletAmmo.handlerOf(ammoSupplier);
            cir.setReturnValue(handler == null ? 0 : MtsBulletAmmo.insertQty(handler, item, count));
        }
    }
}
