package com.yy.superbaddon.shell;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public record ShellContext(
        ServerLevel level,
        Entity shooter,
        Entity storageTarget,
        String gunId,
        String ammoId,
        String ammoSpec,
        String projectileId,
        String vehicleId,
        String weaponId,
        String weaponName,
        int seatIndex,
        Vec3 shootPosition,
        Vec3 shootDirection,
        int projectileAmount
) {
}
