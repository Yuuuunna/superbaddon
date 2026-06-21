package com.yy.superbaddon.compat.mts;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

final class MtsFuelPumpLinks {
    private static final Map<PumpKey, PumpLink> LINKS = new HashMap<>();

    private MtsFuelPumpLinks() {
    }

    static boolean tryConnect(ServerLevel level, BlockPos pumpPos, Player player) {
        if (!MtsCompatConfig.fuelPumpEnabled()) return false;

        BlockEntity blockEntity = level.getBlockEntity(pumpPos);
        if (!MtsReflect.isLikelyFuelPump(blockEntity)) return false;

        Optional<MtsReflect.TankAccess> tank = MtsReflect.tankFrom(MtsReflect.pumpTile(blockEntity));
        if (tank.isEmpty() || tank.get().level() <= 0 || tank.get().fluid().isBlank()) {
            player.displayClientMessage(Component.translatable("message.superbaddon.mts_pump_empty"), true);
            return false;
        }

        VehicleEntity vehicle = findNearestVehicle(level, Vec3.atCenterOf(pumpPos), MtsCompatConfig.pumpSearchRadius());
        if (vehicle == null) {
            player.displayClientMessage(Component.translatable("message.superbaddon.mts_pump_no_vehicle"), true);
            return false;
        }
        if (!hasReceivableEnergyCapability(vehicle)) {
            player.displayClientMessage(Component.translatable("message.superbaddon.mts_pump_no_energy_storage"), true);
            return false;
        }

        PumpKey key = new PumpKey(level.dimension().location().toString(), pumpPos.immutable());
        LINKS.put(key, new PumpLink(vehicle.getUUID(), player.getUUID()));
        player.displayClientMessage(Component.translatable("message.superbaddon.mts_pump_connected"), true);
        return true;
    }

    static void tickLevel(ServerLevel level) {
        if (LINKS.isEmpty() || !MtsCompatConfig.fuelPumpEnabled()) return;

        String dimension = level.dimension().location().toString();
        Iterator<Map.Entry<PumpKey, PumpLink>> iterator = LINKS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<PumpKey, PumpLink> entry = iterator.next();
            PumpKey key = entry.getKey();
            if (!key.dimension().equals(dimension)) continue;

            PumpLink link = entry.getValue();
            if (!tickLink(level, key.pos(), link)) {
                iterator.remove();
            }
        }
    }

    private static boolean tickLink(ServerLevel level, BlockPos pumpPos, PumpLink link) {
        BlockEntity blockEntity = level.getBlockEntity(pumpPos);
        if (!MtsReflect.isLikelyFuelPump(blockEntity)) return false;
        Object pump = MtsReflect.pumpTile(blockEntity);

        Entity entity = level.getEntity(link.vehicleUuid());
        if (!(entity instanceof VehicleEntity vehicle) || vehicle.isRemoved()) return false;

        double radius = MtsCompatConfig.pumpSearchRadius() + 1.0D;
        if (vehicle.getBoundingBox().distanceToSqr(Vec3.atCenterOf(pumpPos)) > radius * radius) {
            notifyPlayer(level, link.playerUuid(), "message.superbaddon.mts_pump_too_far");
            return false;
        }

        Optional<MtsReflect.TankAccess> optionalTank = MtsReflect.tankFrom(pump);
        if (optionalTank.isEmpty()) return false;
        MtsReflect.TankAccess tank = optionalTank.get();

        String fluid = tank.fluid();
        if (fluid.isBlank() || tank.level() <= 0) {
            notifyPlayer(level, link.playerUuid(), "message.superbaddon.mts_pump_empty");
            return false;
        }

        int maxMb = (int) Math.min(Math.floor(tank.level()), MtsCompatConfig.pumpMbPerTick());
        if (maxMb <= 0) return false;

        int offerEnergy = MtsCompatConfig.energyForMb(fluid, maxMb);
        if (offerEnergy <= 0) return false;

        AtomicInteger simulatedAccepted = new AtomicInteger(0);
        vehicle.getCapability(ForgeCapabilities.ENERGY).ifPresent(energy -> {
            if (energy.canReceive()) {
                simulatedAccepted.set(energy.receiveEnergy(offerEnergy, true));
            }
        });

        if (simulatedAccepted.get() <= 0) {
            notifyPlayer(level, link.playerUuid(), "message.superbaddon.mts_pump_full");
            return false;
        }

        int mbToDrain = Math.min(maxMb, MtsCompatConfig.mbForEnergyCeil(fluid, simulatedAccepted.get()));
        if (mbToDrain <= 0) return false;

        double drained = tank.drain(mbToDrain, true);
        if (drained <= 0) return false;

        int actualEnergy = MtsCompatConfig.energyForMb(fluid, drained);
        if (actualEnergy <= 0) return false;

        AtomicInteger accepted = new AtomicInteger(0);
        vehicle.getCapability(ForgeCapabilities.ENERGY).ifPresent(energy -> accepted.set(receive(energy, actualEnergy)));

        if (accepted.get() <= 0) return false;

        MtsReflect.recordPumpDispense(pump, drained);
        blockEntity.setChanged();

        if (MtsReflect.isPurchaseComplete(pump)) {
            notifyPlayer(level, link.playerUuid(), "message.superbaddon.mts_pump_complete");
            return false;
        }
        return true;
    }

    private static int receive(IEnergyStorage energy, int amount) {
        if (!energy.canReceive()) return 0;
        return energy.receiveEnergy(amount, false);
    }

    private static VehicleEntity findNearestVehicle(ServerLevel level, Vec3 center, int radius) {
        AABB box = new AABB(center, center).inflate(radius);
        List<VehicleEntity> vehicles = level.getEntitiesOfClass(VehicleEntity.class, box, vehicle -> !vehicle.isRemoved());
        VehicleEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (VehicleEntity vehicle : vehicles) {
            double distance = vehicle.getBoundingBox().distanceToSqr(center);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = vehicle;
            }
        }
        return best;
    }

    private static boolean hasReceivableEnergyCapability(VehicleEntity vehicle) {
        AtomicInteger result = new AtomicInteger(0);
        vehicle.getCapability(ForgeCapabilities.ENERGY).ifPresent(energy -> {
            if (energy.canReceive()) result.set(1);
        });
        return result.get() > 0;
    }

    private static void notifyPlayer(Level level, UUID playerUuid, String translationKey) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(playerUuid);
        if (player != null) {
            player.displayClientMessage(Component.translatable(translationKey), true);
        }
    }

    private record PumpKey(String dimension, BlockPos pos) {
    }

    private record PumpLink(UUID vehicleUuid, UUID playerUuid) {
    }
}
