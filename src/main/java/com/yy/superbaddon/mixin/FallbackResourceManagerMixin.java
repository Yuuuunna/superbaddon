package com.yy.superbaddon.mixin;

import com.yy.superbaddon.SuperbAddonMod;
import com.yy.superbaddon.content.ContentControlManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.FallbackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@Mixin(FallbackResourceManager.class)
public abstract class FallbackResourceManagerMixin {
    @org.spongepowered.asm.mixin.Unique
    private static final java.util.Set<ResourceLocation> superbaddon$loggedBlocks = java.util.concurrent.ConcurrentHashMap.newKeySet();

    @Inject(method = "getResource", at = @At("HEAD"), cancellable = true)
    private void superbaddon$blockResource(ResourceLocation location, CallbackInfoReturnable<Optional<Resource>> cir) {
        if (ContentControlManager.blocksResource(location)) {
            if (superbaddon$loggedBlocks.add(location)) {
                SuperbAddonMod.LOGGER.debug("[content-control] blocked getResource {}", location);
            }
            cir.setReturnValue(Optional.empty());
        }
    }

    @Inject(method = "getResourceStack", at = @At("HEAD"), cancellable = true)
    private void superbaddon$blockResourceStack(ResourceLocation location, CallbackInfoReturnable<List<Resource>> cir) {
        if (ContentControlManager.blocksResource(location)) cir.setReturnValue(List.of());
    }

    @Inject(method = "listResources", at = @At("RETURN"), cancellable = true)
    private void superbaddon$filterListedResources(String path, Predicate<ResourceLocation> filter, CallbackInfoReturnable<Map<ResourceLocation, Resource>> cir) {
        Map<ResourceLocation, Resource> original = cir.getReturnValue();
        if (original.isEmpty()) return;

        LinkedHashMap<ResourceLocation, Resource> filtered = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry : original.entrySet()) {
            if (!ContentControlManager.blocksResource(entry.getKey())) {
                filtered.put(entry.getKey(), entry.getValue());
            } else if (superbaddon$loggedBlocks.add(entry.getKey())) {
                SuperbAddonMod.LOGGER.debug("[content-control] blocked listResources entry under '{}': {}", path, entry.getKey());
            }
        }
        if (filtered.size() != original.size()) cir.setReturnValue(Map.copyOf(filtered));
    }

    @Inject(method = "listResourceStacks", at = @At("RETURN"), cancellable = true)
    private void superbaddon$filterListedResourceStacks(String path, Predicate<ResourceLocation> filter, CallbackInfoReturnable<Map<ResourceLocation, List<Resource>>> cir) {
        Map<ResourceLocation, List<Resource>> original = cir.getReturnValue();
        if (original.isEmpty()) return;

        LinkedHashMap<ResourceLocation, List<Resource>> filtered = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, List<Resource>> entry : original.entrySet()) {
            if (!ContentControlManager.blocksResource(entry.getKey())) filtered.put(entry.getKey(), entry.getValue());
        }
        if (filtered.size() != original.size()) cir.setReturnValue(Map.copyOf(filtered));
    }
}
