package com.yy.superbaddon.compat.mts;

import com.yy.superbaddon.SuperbAddonMod;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.atomic.AtomicInteger;

@Mod.EventBusSubscriber(modid = SuperbAddonMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MtsCompatEvents {
    private static final String JERRYCAN_FLUID_TAG = "jerrycanFluid";

    private MtsCompatEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickVehicle(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getLevel() instanceof ServerLevel)) return;
        VehicleEntity vehicle = asVehicle(event.getTarget());
        if (vehicle == null) return;

        if (tryUseJerrycan(event.getEntity(), event.getItemStack(), vehicle)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickVehicleSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getLevel() instanceof ServerLevel)) return;
        VehicleEntity vehicle = asVehicle(event.getTarget());
        if (vehicle == null) return;

        if (tryUseJerrycan(event.getEntity(), event.getItemStack(), vehicle)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private static VehicleEntity asVehicle(Entity target) {
        if (target instanceof VehicleEntity vehicle) return vehicle;
        Entity root = target.getRootVehicle();
        return root instanceof VehicleEntity vehicle ? vehicle : null;
    }

    private static boolean tryUseJerrycan(Player player, ItemStack stack, VehicleEntity vehicle) {
        if (!isMtsLoaded() || !MtsCompatConfig.jerrycanEnabled()) return false;
        if (stack.isEmpty()) return false;

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(JERRYCAN_FLUID_TAG)) return false;

        String fluid = tag.getString(JERRYCAN_FLUID_TAG);
        if (fluid.isBlank()) return false;
        if (!MtsCompatConfig.isAllowedJerrycan(stack)) return false;

        int offer = MtsCompatConfig.energyForBucket(fluid);
        if (offer <= 0) {
            player.displayClientMessage(Component.translatable("message.superbaddon.mts_jerrycan_bad_fuel"), true);
            return true;
        }

        AtomicInteger simulatedAccepted = new AtomicInteger(0);
        vehicle.getCapability(ForgeCapabilities.ENERGY).ifPresent(energy -> {
            if (energy.canReceive()) simulatedAccepted.set(energy.receiveEnergy(offer, true));
        });

        // The jerrycan holds a single bucket and cannot store a partial amount.  As long as the
        // vehicle can take *any* energy, empty the whole can and discard whatever does not fit, so
        // it still works for emergency top-offs instead of only on a near-empty tank.
        if (simulatedAccepted.get() <= 0) {
            player.displayClientMessage(Component.translatable("message.superbaddon.mts_jerrycan_full"), true);
            return true;
        }

        vehicle.getCapability(ForgeCapabilities.ENERGY).ifPresent(energy -> energy.receiveEnergy(offer, false));

        tag.remove(JERRYCAN_FLUID_TAG);
        if (tag.isEmpty()) stack.setTag(null);
        player.displayClientMessage(Component.translatable("message.superbaddon.mts_jerrycan_success"), true);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!isMtsLoaded() || !MtsCompatConfig.fuelPumpEnabled()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        // Keep MTS's native item interactions intact.  Empty hand = bridge to nearby SuperbWarfare vehicle.
        if (!event.getItemStack().isEmpty()) return;

        BlockPos pos = event.getPos();
        if (!MtsFuelPumpLinks.tryConnect(level, pos, event.getEntity())) return;

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!isMtsLoaded() || !MtsCompatConfig.fuelPumpEnabled()) return;
        if (event.level instanceof ServerLevel level) {
            MtsFuelPumpLinks.tickLevel(level);
        }
    }

    private static boolean isMtsLoaded() {
        ModList modList = ModList.get();
        return modList.isLoaded("mts")
                || modList.isLoaded("immersivevehicles")
                || modList.isLoaded("minecrafttransportsimulator");
    }
}
