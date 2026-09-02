package com.yy.superbaddon.event;

import com.yy.superbaddon.SuperbAddonMod;
import com.yy.superbaddon.command.ShellEjectionCommands;
import com.yy.superbaddon.content.ContentControlManager;
import com.yy.superbaddon.shell.DisposalMode;
import com.yy.superbaddon.shell.ShellContext;
import com.yy.superbaddon.shell.ShellContextFactory;
import com.yy.superbaddon.shell.ShellEjectionPoint;
import com.yy.superbaddon.shell.ShellEjectionSpec;
import com.yy.superbaddon.shell.ShellRule;
import com.yy.superbaddon.shell.ShellRuleReloadListener;
import com.yy.superbaddon.shell.ShellRuleSet;
import com.atsuishio.superbwarfare.api.event.ShootEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = SuperbAddonMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShellEjectionEvents {

    private ShellEjectionEvents() {
    }

    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ShellRuleReloadListener());
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        ShellEjectionCommands.register(event.getDispatcher());
    }


    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ItemEntity itemEntity) {
            if (ContentControlManager.blocksItemStack(itemEntity.getItem())) event.setCanceled(true);
            return;
        }

        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (ContentControlManager.blocksVehicleEntity(id)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onShootPost(ShootEvent.Post event) {
        Optional<ShellContext> optionalContext = ShellContextFactory.fromShootParameters(event.getParameters());
        if (optionalContext.isEmpty()) return;

        ShellContext context = optionalContext.get();
        ServerLevel level = context.level();
        Entity shooter = context.shooter();
        Entity storageTarget = context.storageTarget();

        Optional<ShellRule> matched = ShellRuleSet.matchEjection(context);
        if (matched.isEmpty()) return;

        ShellRule rule = matched.get();
        Optional<ShellEjectionSpec> selected = rule.selectedEjection(context);
        if (selected.isEmpty()) return;

        ShellEjectionSpec ejection = selected.get();
        if (ejection.mode() == DisposalMode.NONE) return;

        ShellEjectionPoint storagePoint = ejection.points().get(0);
        if (ejection.mode() == DisposalMode.STORE && level.random.nextFloat() > storagePoint.chance()) return;
        ItemStack stack = casingStack(storagePoint);
        if (stack.isEmpty()) return;

        if (ejection.mode() == DisposalMode.STORE) {
            ItemStack remainder = insertIntoStorage(storageTarget, stack);
            if (!remainder.isEmpty() && ejection.fallbackToDrop()) {
                drop(level, shooter, context, storagePoint, remainder);
            }
            return;
        }

        for (ShellEjectionPoint point : ejection.points()) {
            if (level.random.nextFloat() > point.chance()) continue;
            ItemStack pointStack = casingStack(point);
            if (!pointStack.isEmpty()) {
                drop(level, shooter, context, point, pointStack);
            }
        }
    }

    

    

    

    

    

    

    

    

    

    

    

    

    private static ItemStack casingStack(ShellEjectionPoint point) {
        ResourceLocation id = ResourceLocation.tryParse(point.casingItem());
        if (id == null) return ItemStack.EMPTY;
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item, point.count());
    }

    private static ItemStack insertIntoStorage(Entity target, ItemStack stack) {
        if (target == null || stack.isEmpty()) return stack;
        Optional<IItemHandler> optional = target.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
        if (optional.isEmpty()) return stack;

        IItemHandler handler = optional.get();
        ItemStack remainder = stack.copy();
        for (int slot = 0; slot < handler.getSlots() && !remainder.isEmpty(); slot++) {
            remainder = handler.insertItem(slot, remainder, false);
        }
        return remainder;
    }

    private static void drop(ServerLevel level, Entity shooter, ShellContext context, ShellEjectionPoint point, ItemStack stack) {
        Vec3 forward = safeNormalize(context.shootDirection(), shooter.getLookAngle());
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        if (right.lengthSqr() < 1.0E-6) right = new Vec3(1.0, 0.0, 0.0);
        right = right.normalize();

        Vec3 origin = context.shootPosition() == null ? shooter.position().add(0, shooter.getBbHeight() * 0.5, 0) : context.shootPosition();
        Vec3 pos = origin
                .add(right.scale(point.offset().x))
                .add(up.scale(point.offset().y))
                .add(forward.scale(point.offset().z));

        Vec3 random = new Vec3(
                (level.random.nextDouble() - 0.5) * point.randomVelocity(),
                level.random.nextDouble() * point.randomVelocity(),
                (level.random.nextDouble() - 0.5) * point.randomVelocity()
        );

        Vec3 velocity = right.scale(point.velocity().x)
                .add(up.scale(point.velocity().y))
                .add(forward.scale(point.velocity().z))
                .add(random)
                .add(shooter.getDeltaMovement().scale(0.25));

        ItemEntity entity = new ItemEntity(level, pos.x, pos.y, pos.z, stack.copy());
        entity.setPickUpDelay(point.pickupDelay());
        entity.setDeltaMovement(velocity);
        level.addFreshEntity(entity);
    }

    private static Vec3 safeNormalize(Vec3 preferred, Vec3 fallback) {
        Vec3 value = preferred == null || preferred.lengthSqr() < 1.0E-6 ? fallback : preferred;
        if (value == null || value.lengthSqr() < 1.0E-6) return new Vec3(0.0, 0.0, 1.0);
        return value.normalize();
    }
}
