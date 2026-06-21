package com.yy.superbaddon.command;

import com.yy.superbaddon.SuperbAddonMod;
import com.yy.superbaddon.content.ContentControlManager;
import com.yy.superbaddon.shell.AmmoOption;
import com.yy.superbaddon.shell.ShellRule;
import com.atsuishio.superbwarfare.data.gun.AmmoConsumer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.registries.ForgeRegistries;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.data.vehicle.subdata.SeatInfo;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ShellEjectionScanner {
    private static final Set<String> IGNORED_NAMESPACES = Set.of("minecraft", "forge", SuperbAddonMod.MODID);
    private static final int MAX_SCANNED_SEATS = 32;
    private static final String[] RECIPE_ADVANCEMENT_CATEGORIES = {
            "building_blocks", "decorations", "redstone", "transportation", "tools",
            "combat", "food", "brewing", "misc"
    };

    private ShellEjectionScanner() {
    }

    public static ScanResult scan(MinecraftServer server, boolean enabled) {
        String generatedAt = Instant.now().toString();
        LinkedHashSet<String> superbWarfareNamespaces = superbWarfareNamespaces();
        LinkedHashMap<ResourceLocation, LinkedHashSet<String>> guns = scanRegisteredGuns(superbWarfareNamespaces);
        VehicleScan vehicleScan = scanRegisteredVehicles(server, superbWarfareNamespaces);
        LinkedHashMap<ResourceLocation, LinkedHashMap<String, VehicleWeaponRef>> loadedVehicleWeapons = scanLoadedVehicleWeapons(server);

        LinkedHashSet<ResourceLocation> registeredVehicles = vehicleScan.vehicles();
        LinkedHashMap<ResourceLocation, LinkedHashMap<String, VehicleWeaponRef>> vehicleWeapons = new LinkedHashMap<>(vehicleScan.createdVehicleWeapons());
        mergeVehicleWeapons(vehicleWeapons, loadedVehicleWeapons);

        JsonArray rules = new JsonArray();
        LinkedHashMap<String, JsonObject> files = new LinkedHashMap<>();
        int ammoVariantRules = 0;

        for (Map.Entry<ResourceLocation, LinkedHashSet<String>> entry : guns.entrySet()) {
            JsonArray fileRules = new JsonArray();
            ammoVariantRules += addGunRules(fileRules, entry.getKey(), entry.getValue(), enabled);
            addAllRules(rules, fileRules);
            files.put(scanFileKey(entry.getKey(), "items"), generatedFileRoot(generatedAt, enabled, "item", entry.getKey(), fileRules));
        }

        for (Map.Entry<ResourceLocation, LinkedHashMap<String, VehicleWeaponRef>> entry : vehicleWeapons.entrySet()) {
            ResourceLocation vehicleId = entry.getKey();
            registeredVehicles.add(vehicleId);
            JsonArray fileRules = new JsonArray();
            int index = 0;
            for (VehicleWeaponRef ref : entry.getValue().values()) {
                ammoVariantRules += addVehicleWeaponRules(fileRules, vehicleId, ref, index++, enabled);
            }
            if (fileRules.size() > 0) {
                addAllRules(rules, fileRules);
                files.put(scanFileKey(vehicleId, "vehicles"), generatedFileRoot(generatedAt, enabled, "vehicle", vehicleId, fileRules));
            }
        }

        for (ResourceLocation vehicleId : registeredVehicles) {
            if (!vehicleWeapons.containsKey(vehicleId)) {
                JsonObject rule = vehicleTemplateRule(vehicleId, enabled);
                rules.add(rule);

                JsonArray fileRules = new JsonArray();
                fileRules.add(rule);
                files.put(scanFileKey(vehicleId, "vehicles"), generatedFileRoot(generatedAt, enabled, "vehicle", vehicleId, fileRules));
            }
        }

        JsonArray scannedNamespaces = new JsonArray();
        for (String namespace : superbWarfareNamespaces) scannedNamespaces.add(namespace);

        JsonObject root = new JsonObject();
        root.addProperty("format", 5);
        root.addProperty("generated_by", SuperbAddonMod.MODID);
        root.addProperty("generated_at", generatedAt);
        root.addProperty("enabled_by_default", enabled);
        root.addProperty("layout", "generated_scan/<namespace>/<items|vehicles>/<registry_path>.json");
        root.addProperty("note", "Generated candidates for /superbaddon shell_ejection scan. Output is split by namespace, type and registry name. target selects the gun or vehicle weapon; ammo_override controls which ammo it may use; ejection.by_ammo controls the casing for each selected ammo. Existing known ammo consumers are exported as one rule per ammo variant; unknown ammo is left as an empty placeholder. content_control.mode can be keep, recipes_only or block_load.");
        root.add("scanned_namespaces", scannedNamespaces);
        root.addProperty("registered_guns", guns.size());
        root.addProperty("registered_vehicle_types", registeredVehicles.size());
        root.addProperty("registered_vehicle_instances_created_for_scan", vehicleScan.createdVehicleCount());
        root.addProperty("loaded_vehicle_instances_with_weapons", loadedVehicleWeapons.size());
        root.addProperty("ammo_variant_rules", ammoVariantRules);
        root.add("rules", rules);
        return new ScanResult(root, files, rules.size(), files.size(), guns.size(), registeredVehicles.size(), vehicleScan.createdVehicleCount(), loadedVehicleWeapons.size(), superbWarfareNamespaces.size());
    }

    private static void addAllRules(JsonArray target, JsonArray source) {
        for (int i = 0; i < source.size(); i++) target.add(source.get(i));
    }

    private static LinkedHashSet<String> superbWarfareNamespaces() {
        LinkedHashSet<String> namespaces = new LinkedHashSet<>();
        namespaces.add("superbwarfare");

        try {
            for (IModInfo mod : ModList.get().getMods()) {
                String modId = mod.getModId();
                if (modId == null || modId.isBlank() || IGNORED_NAMESPACES.contains(modId)) continue;

                for (IModInfo.ModVersion dependency : mod.getDependencies()) {
                    if ("superbwarfare".equals(dependency.getModId())) {
                        namespaces.add(modId);
                        break;
                    }
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
        }

        namespaces.remove(SuperbAddonMod.MODID);
        return namespaces;
    }

    private static LinkedHashMap<ResourceLocation, LinkedHashSet<String>> scanRegisteredGuns(Set<String> superbWarfareNamespaces) {
        ArrayList<ResourceLocation> ids = new ArrayList<>();
        LinkedHashMap<ResourceLocation, LinkedHashSet<String>> ammoByGun = new LinkedHashMap<>();
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id != null && !ContentControlManager.isProtectedInfrastructureId(id) && isLikelyGunItem(id, item, superbWarfareNamespaces)) {
                ids.add(id);
                ammoByGun.put(id, ammoSpecs(item));
            }
        }
        ids.sort(Comparator.comparing(ResourceLocation::toString));

        LinkedHashMap<ResourceLocation, LinkedHashSet<String>> sorted = new LinkedHashMap<>();
        for (ResourceLocation id : ids) sorted.put(id, ammoByGun.getOrDefault(id, new LinkedHashSet<>()));
        return sorted;
    }

    private static VehicleScan scanRegisteredVehicles(MinecraftServer server, Set<String> superbWarfareNamespaces) {
        ArrayList<ResourceLocation> ids = new ArrayList<>();
        LinkedHashMap<ResourceLocation, LinkedHashMap<String, VehicleWeaponRef>> weapons = new LinkedHashMap<>();
        ServerLevel level = firstLevel(server);
        int createdVehicleCount = 0;

        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES.getValues()) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
            if (id == null || IGNORED_NAMESPACES.contains(id.getNamespace()) || ContentControlManager.isProtectedInfrastructureId(id)) continue;

            Entity created = null;
            boolean isVehicle = false;
            try {
                if (level != null) {
                    created = type.create(level);
                    if (created instanceof VehicleEntity vehicle) {
                        isVehicle = true;
                        createdVehicleCount++;
                        LinkedHashMap<String, VehicleWeaponRef> refs = weaponRefs(vehicle);
                        if (!refs.isEmpty()) weapons.put(id, refs);
                    }
                }
            } catch (RuntimeException | LinkageError ignored) {
            } finally {
                if (created != null) {
                    try {
                        created.discard();
                    } catch (RuntimeException ignored) {
                    }
                }
            }

            if (!isVehicle && isLikelyVehicleTypeByName(id, superbWarfareNamespaces)) {
                isVehicle = true;
            }

            if (isVehicle) ids.add(id);
        }

        ids.sort(Comparator.comparing(ResourceLocation::toString));
        LinkedHashSet<ResourceLocation> sortedIds = new LinkedHashSet<>(ids);
        return new VehicleScan(sortedIds, sortWeaponMap(weapons), createdVehicleCount);
    }

    private static ServerLevel firstLevel(MinecraftServer server) {
        try {
            for (ServerLevel level : server.getAllLevels()) return level;
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    private static LinkedHashMap<ResourceLocation, LinkedHashMap<String, VehicleWeaponRef>> scanLoadedVehicleWeapons(MinecraftServer server) {
        LinkedHashMap<ResourceLocation, LinkedHashMap<String, VehicleWeaponRef>> result = new LinkedHashMap<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : loadedEntities(level)) {
                if (!(entity instanceof VehicleEntity vehicle)) continue;
                ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(vehicle.getType());
                if (id == null || ContentControlManager.isProtectedInfrastructureId(id)) continue;
                mergeWeaponRefs(result.computeIfAbsent(id, ignored -> new LinkedHashMap<>()), weaponRefs(vehicle));
            }
        }
        result.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        return sortWeaponMap(result);
    }

    private static void mergeVehicleWeapons(LinkedHashMap<ResourceLocation, LinkedHashMap<String, VehicleWeaponRef>> target,
                                            LinkedHashMap<ResourceLocation, LinkedHashMap<String, VehicleWeaponRef>> source) {
        for (Map.Entry<ResourceLocation, LinkedHashMap<String, VehicleWeaponRef>> entry : source.entrySet()) {
            mergeWeaponRefs(target.computeIfAbsent(entry.getKey(), ignored -> new LinkedHashMap<>()), entry.getValue());
        }
    }

    private static void mergeWeaponRefs(LinkedHashMap<String, VehicleWeaponRef> target,
                                        LinkedHashMap<String, VehicleWeaponRef> source) {
        for (VehicleWeaponRef ref : source.values()) {
            VehicleWeaponRef targetRef = target.computeIfAbsent(ref.name(), name -> new VehicleWeaponRef(name, new LinkedHashSet<>(), new LinkedHashSet<>()));
            targetRef.seats().addAll(ref.seats());
            targetRef.allowedAmmo().addAll(ref.allowedAmmo());
        }
    }

    private static LinkedHashMap<ResourceLocation, LinkedHashMap<String, VehicleWeaponRef>> sortWeaponMap(LinkedHashMap<ResourceLocation, LinkedHashMap<String, VehicleWeaponRef>> input) {
        ArrayList<Map.Entry<ResourceLocation, LinkedHashMap<String, VehicleWeaponRef>>> entries = new ArrayList<>(input.entrySet());
        entries.sort(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)));

        LinkedHashMap<ResourceLocation, LinkedHashMap<String, VehicleWeaponRef>> sorted = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, LinkedHashMap<String, VehicleWeaponRef>> entry : entries) {
            ArrayList<VehicleWeaponRef> refs = new ArrayList<>(entry.getValue().values());
            refs.sort(Comparator.comparing(VehicleWeaponRef::name));

            LinkedHashMap<String, VehicleWeaponRef> sortedRefs = new LinkedHashMap<>();
            for (VehicleWeaponRef ref : refs) {
                sortedRefs.put(ref.name(), new VehicleWeaponRef(ref.name(), sortedInts(ref.seats()), sortedStrings(ref.allowedAmmo())));
            }
            sorted.put(entry.getKey(), sortedRefs);
        }
        return sorted;
    }

    private static LinkedHashSet<Integer> sortedInts(Collection<Integer> values) {
        ArrayList<Integer> list = new ArrayList<>(values);
        list.sort(Integer::compareTo);
        return new LinkedHashSet<>(list);
    }

    private static LinkedHashSet<String> sortedStrings(Collection<String> values) {
        ArrayList<String> list = new ArrayList<>(values);
        list.sort(String::compareTo);
        return new LinkedHashSet<>(list);
    }

    private static Iterable<Entity> loadedEntities(ServerLevel level) {
        Iterable<Entity> direct = invokeEntityIterable(level, "getAllEntities");
        if (direct != null) return direct;

        try {
            Method method = level.getClass().getMethod("getEntities");
            Object storage = method.invoke(level);
            if (storage != null) {
                Iterable<Entity> all = invokeEntityIterable(storage, "getAll");
                if (all != null) return all;
                Iterable<Entity> allEntities = invokeEntityIterable(storage, "getAllEntities");
                if (allEntities != null) return allEntities;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return List.of();
    }

    private static Iterable<Entity> invokeEntityIterable(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            if (value instanceof Iterable<?> iterable) {
                ArrayList<Entity> entities = new ArrayList<>();
                for (Object item : iterable) {
                    if (item instanceof Entity entity) entities.add(entity);
                }
                return entities;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return null;
    }

    private static LinkedHashMap<String, VehicleWeaponRef> weaponRefs(VehicleEntity vehicle) {
        LinkedHashMap<String, VehicleWeaponRef> refs = new LinkedHashMap<>();
        try {
            Map<String, GunData> map = vehicle.getGunDataMap();
            if (map != null) {
                for (Map.Entry<String, GunData> entry : map.entrySet()) {
                    addWeaponRef(refs, entry.getKey(), -1, ammoSpecs(entry.getValue()));
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
        }

        for (int seat = 0; seat < MAX_SCANNED_SEATS; seat++) {
            try {
                SeatInfo seatInfo = vehicle.getSeat(seat);
                if (seatInfo != null) {
                    for (String weapon : seatInfo.weapons()) {
                        addWeaponRef(refs, weapon, seat, Set.of());
                    }
                }
            } catch (RuntimeException | LinkageError ignored) {
            }

            try {
                addWeaponRef(refs, vehicle.getGunName(seat), seat, Set.of());
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
        return refs;
    }

    private static void addWeaponRef(LinkedHashMap<String, VehicleWeaponRef> refs, String raw, int seat, Collection<String> ammo) {
        String value = ShellRule.normalizeId(raw);
        if (value.isBlank()) return;

        VehicleWeaponRef ref = refs.computeIfAbsent(value, name -> new VehicleWeaponRef(name, new LinkedHashSet<>(), new LinkedHashSet<>()));
        if (seat >= 0) ref.seats().add(seat);
        ref.allowedAmmo().addAll(ammo);
    }

    private static LinkedHashSet<String> ammoSpecs(Item item) {
        try {
            return ammoSpecs(GunData.from(new ItemStack(item)));
        } catch (RuntimeException | LinkageError ignored) {
            return new LinkedHashSet<>();
        }
    }

    private static LinkedHashSet<String> ammoSpecs(GunData data) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (data == null) return result;
        try {
            List<AmmoConsumer> consumers = data.get(GunProp.AMMO_CONSUMER);
            for (AmmoConsumer consumer : consumers) {
                result.addAll(ammoSpecs(consumer));
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
        return sortedStrings(result);
    }

    private static LinkedHashSet<String> ammoSpecs(AmmoConsumer consumer) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String spec = ammoSpec(consumer);
        if (!spec.isBlank()) result.add(spec);
        return result;
    }

    private static String ammoSpec(AmmoConsumer consumer) {
        if (consumer == null) return "";
        try {
            if (!consumer.initialized()) consumer.init();
        } catch (RuntimeException ignored) {
        }
        try {
            String spec = consumer.getAmmo();
            if (spec != null && !spec.isBlank()) return AmmoOption.normalizeAmmoSpec(spec);
        } catch (RuntimeException ignored) {
        }
        try {
            ItemStack stack = consumer.stack();
            ResourceLocation id = stack == null || stack.isEmpty() || stack.getItem() == Items.AIR
                    ? null
                    : ForgeRegistries.ITEMS.getKey(stack.getItem());
            return id == null ? "" : id.toString();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static boolean isLikelyGunItem(ResourceLocation id, Item item, Set<String> superbWarfareNamespaces) {
        if (IGNORED_NAMESPACES.contains(id.getNamespace())) return false;
        String className = item.getClass().getName().toLowerCase(Locale.ROOT);
        String path = id.getPath().toLowerCase(Locale.ROOT);

        if (isClearlyNotGunPath(path)) return false;
        if (isGunClass(className)) return true;
        if (!superbWarfareNamespaces.contains(id.getNamespace())) return false;
        return isWeaponishItemPath(path);
    }

    private static boolean isGunClass(String className) {
        return className.contains(".gun.")
                || className.contains(".guns.")
                || className.endsWith("gunitem")
                || className.contains("gunitem")
                || className.endsWith("firearmitem")
                || className.contains("firearm")
                || className.contains("weaponitem")
                || className.contains("shootable");
    }

    private static boolean isClearlyNotGunPath(String path) {
        return path.contains("ammo")
                || path.contains("bullet")
                || path.contains("casing")
                || path.contains("shell")
                || path.contains("cartridge")
                || path.contains("magazine")
                || path.contains("clip")
                || path.contains("projectile")
                || path.contains("missile")
                || path.contains("rocket")
                || path.contains("grenade")
                || path.contains("bomb")
                || path.contains("mine")
                || path.contains("spawn_egg")
                || path.contains("helmet")
                || path.contains("chestplate")
                || path.contains("leggings")
                || path.contains("boots")
                || path.contains("armor")
                || path.contains("material")
                || path.contains("ingot")
                || path.contains("nugget");
    }

    private static boolean isWeaponishItemPath(String path) {
        return path.contains("gun")
                || path.contains("rifle")
                || path.contains("pistol")
                || path.contains("handgun")
                || path.contains("revolver")
                || path.contains("shotgun")
                || path.contains("smg")
                || path.contains("sniper")
                || path.contains("carbine")
                || path.contains("machine_gun")
                || path.contains("machinegun")
                || path.contains("minigun")
                || path.contains("launcher")
                || path.contains("hmg")
                || path.contains("lmg")
                || path.contains("mg_")
                || path.endsWith("_mg")
                || path.contains("ak_")
                || path.contains("ar_")
                || path.contains("m4")
                || path.contains("m16")
                || path.contains("mk_")
                || path.contains("glock")
                || path.contains("deagle")
                || path.contains("uzi")
                || path.contains("vector")
                || path.contains("scar")
                || path.contains("mp5")
                || path.contains("p90")
                || path.contains("rpg")
                || path.contains("javelin");
    }

    private static boolean isLikelyVehicleTypeByName(ResourceLocation id, Set<String> superbWarfareNamespaces) {
        if (IGNORED_NAMESPACES.contains(id.getNamespace())) return false;
        if (!superbWarfareNamespaces.contains(id.getNamespace())) return false;
        String path = id.getPath().toLowerCase(Locale.ROOT);
        return path.contains("vehicle")
                || path.contains("tank")
                || path.contains("ifv")
                || path.contains("apc")
                || path.contains("lav")
                || path.contains("bmp")
                || path.contains("bradley")
                || path.contains("ztz")
                || path.contains("t_90")
                || path.contains("t90")
                || path.contains("m_1a_2")
                || path.contains("m1a2")
                || path.contains("ah_6")
                || path.contains("ah6")
                || path.contains("mi_28")
                || path.contains("mi28")
                || path.contains("a_10")
                || path.contains("a10")
                || path.contains("helicopter")
                || path.contains("heli")
                || path.contains("plane")
                || path.contains("fighter")
                || path.contains("aircraft")
                || path.contains("truck")
                || path.contains("pickup")
                || path.contains("turret")
                || path.contains("cannon")
                || path.contains("artillery")
                || path.contains("plz")
                || path.contains("hpj");
    }

    private static JsonObject generatedFileRoot(String generatedAt, boolean enabled, String ownerType, ResourceLocation ownerId, JsonArray rules) {
        JsonObject root = new JsonObject();
        root.addProperty("format", 5);
        root.addProperty("generated_by", SuperbAddonMod.MODID);
        root.addProperty("generated_at", generatedAt);
        root.addProperty("enabled_by_default", enabled);
        root.addProperty("owner_type", ownerType);
        root.addProperty("owner", ownerId.toString());
        root.addProperty("note", "Generated by /superbaddon shell_ejection scan. Edit target, ammo_override, ejection.by_ammo and enabled as needed. content_control.mode can be keep, recipes_only or block_load.");
        root.add("content_control", contentControl(ownerType, ownerId));
        root.add("rules", rules);
        return root;
    }


    private static JsonObject contentControl(String ownerType, ResourceLocation ownerId) {
        JsonObject control = new JsonObject();
        control.addProperty("mode", "keep");
        control.addProperty("target_type", ownerType);
        control.addProperty("target", ownerId.toString());
        control.addProperty("comment", "keep = normal load; recipes_only = remove recipes for this target; block_load = remove recipes, block configured resources and cancel blocked vehicle spawns. Existing Forge registries cannot be unregistered after SuperbWarfare has registered them.");
        control.add("recipe_ids", new JsonArray());

        JsonObject paths = new JsonObject();
        paths.add("assets", defaultAssetPaths(ownerType, ownerId));
        paths.add("data", defaultDataPaths(ownerType, ownerId));
        control.add("resource_paths", paths);

        JsonObject prefixes = new JsonObject();
        prefixes.add("assets", defaultAssetPrefixes(ownerType, ownerId));
        prefixes.add("data", defaultDataPrefixes(ownerId));
        control.add("resource_prefixes", prefixes);
        return control;
    }

    private static JsonArray defaultAssetPaths(String ownerType, ResourceLocation ownerId) {
        JsonArray array = new JsonArray();
        String namespace = ownerId.getNamespace();
        String path = ownerId.getPath();
        if ("vehicle".equals(ownerType)) {
            array.add(namespace + ":sbw/vehicles/" + path + ".json");
            return array;
        }

        array.add(namespace + ":sbw/guns/" + path + ".json");
        array.add(namespace + ":models/item/" + path + ".json");
        array.add(namespace + ":textures/item/" + path + ".png");
        array.add(namespace + ":textures/gun_icon/" + path + "_icon.png");
        return array;
    }

    private static JsonArray defaultAssetPrefixes(String ownerType, ResourceLocation ownerId) {
        JsonArray array = new JsonArray();
        String namespace = ownerId.getNamespace();
        String path = ownerId.getPath();
        if ("vehicle".equals(ownerType)) {
            array.add(namespace + ":textures/vehicle_icon/" + path);
            array.add(namespace + ":textures/bedrock/vehicle/" + path);
            array.add(namespace + ":textures/bedrock/vehicle_lod/" + path);
            array.add(namespace + ":models/bedrock/vehicle/" + path);
            array.add(namespace + ":models/bedrock/vehicle_lod/" + path);
            array.add(namespace + ":animations/bedrock/vehicle/" + path);
            array.add(namespace + ":sounds/vehicle/" + path);
            return array;
        }

        array.add(namespace + ":models/item/" + path);
        array.add(namespace + ":models/displaysettings/" + path);
        array.add(namespace + ":textures/item/" + path);
        array.add(namespace + ":textures/gun_icon/" + path);
        array.add(namespace + ":geo/" + path);
        array.add(namespace + ":geo/lod/" + path);
        array.add(namespace + ":animations/" + path);
        return array;
    }

    private static JsonArray defaultDataPaths(String ownerType, ResourceLocation ownerId) {
        JsonArray array = new JsonArray();
        String namespace = ownerId.getNamespace();
        String path = ownerId.getPath();
        array.add(namespace + ":" + ("vehicle".equals(ownerType) ? "sbw/vehicles/" : "sbw/guns/") + path + ".json");
        array.add(namespace + ":recipes/" + path + ".json");
        return array;
    }

    private static JsonArray defaultDataPrefixes(ResourceLocation ownerId) {
        JsonArray array = new JsonArray();
        String namespace = ownerId.getNamespace();
        String path = ownerId.getPath();
        array.add(namespace + ":recipes/" + path);
        for (String category : RECIPE_ADVANCEMENT_CATEGORIES) {
            array.add(namespace + ":advancements/recipes/" + category + "/" + path);
        }
        return array;
    }

    private static String scanFileKey(ResourceLocation id, String type) {
        return safePathPart(id.getNamespace()) + "/" + safePathPart(type) + "/" + safeResourcePath(id.getPath()) + ".json";
    }

    private static String safeResourcePath(String raw) {
        String value = raw == null ? "" : raw.toLowerCase(Locale.ROOT).trim();
        StringBuilder out = new StringBuilder();
        boolean slash = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '/') {
                if (!slash && out.length() > 0) out.append('/');
                slash = true;
            } else if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.') {
                out.append(c);
                slash = false;
            } else {
                out.append('_');
                slash = false;
            }
        }
        while (out.length() > 0 && out.charAt(out.length() - 1) == '/') out.deleteCharAt(out.length() - 1);
        return out.length() == 0 ? "unnamed" : out.toString();
    }

    private static String safePathPart(String raw) {
        String value = raw == null ? "" : raw.toLowerCase(Locale.ROOT).trim();
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.') {
                out.append(c);
            } else {
                out.append('_');
            }
        }
        return out.length() == 0 ? "unknown" : out.toString();
    }

    private static int addGunRules(JsonArray rules, ResourceLocation gunId, LinkedHashSet<String> ammo, boolean enabled) {
        if (ammo == null || ammo.isEmpty()) {
            rules.add(gunRule(gunId, "", enabled));
            return 0;
        }

        int count = 0;
        for (String ammoSpec : ammo) {
            rules.add(gunRule(gunId, ammoSpec, enabled));
            count++;
        }
        return count;
    }

    private static JsonObject gunRule(ResourceLocation gunId, String ammoSpec, boolean enabled) {
        JsonObject rule = baseRule(enabled, ammoSpec.isBlank() ? 1100 : 1150);
        rule.addProperty("comment", ammoSpec.isBlank()
                ? "Handheld gun candidate generated by scan. Fill ammo_override.allowed and ejection.by_ammo before enabling."
                : "Handheld gun ammo variant generated by scan. ammo_override.allowed defines the usable ammo; ejection.by_ammo defines its casing.");

        JsonObject target = new JsonObject();
        target.add("guns", ids(gunId.toString()));
        rule.add("target", target);

        rule.add("ammo_override", ammoOverride(ammoSpec, !ammoSpec.isBlank(), "replace"));

        JsonObject ejection = new JsonObject();
        JsonObject spec = ejection("drop", guessCasing(ammoSpec, gunId.getPath()), 24);
        spec.add("ports", ports(port("right", 0.20, -0.04, -0.12, 0.10, 0.07, -0.03, 0.03)));
        if (ammoSpec.isBlank()) {
            ejection.add("default", spec);
        } else {
            ejection.add("by_ammo", byAmmo(ammoSpec, spec));
        }
        rule.add("ejection", ejection);
        return rule;
    }

    private static int addVehicleWeaponRules(JsonArray rules, ResourceLocation vehicleId, VehicleWeaponRef ref, int index, boolean enabled) {
        if (ref.allowedAmmo().isEmpty()) {
            rules.add(vehicleWeaponRule(vehicleId, ref, index, "", enabled));
            return 0;
        }

        int count = 0;
        for (String ammoSpec : ref.allowedAmmo()) {
            rules.add(vehicleWeaponRule(vehicleId, ref, index, ammoSpec, enabled));
            count++;
        }
        return count;
    }

    private static JsonObject vehicleWeaponRule(ResourceLocation vehicleId, VehicleWeaponRef ref, int index, String ammoSpec, boolean enabled) {
        JsonObject rule = baseRule(enabled, ammoSpec.isBlank() ? 1200 : 1250);
        rule.addProperty("comment", ammoSpec.isBlank()
                ? "Vehicle weapon candidate generated by scan. target.seats is filled when discoverable; fill ammo_override.allowed and ejection.by_ammo before enabling."
                : "Vehicle weapon ammo variant generated by scan. Useful for switchable AP/HE/AA-style ammo; give each ammo its own casing under ejection.by_ammo.");

        JsonObject target = new JsonObject();
        target.add("vehicles", ids(vehicleId.toString()));
        target.add("weapons", ids(ref.name(), vehicleId + "/" + ref.name()));
        if (!ref.seats().isEmpty()) target.add("seats", ints(ref.seats()));
        target.addProperty("requires_vehicle", true);
        rule.add("target", target);

        rule.add("ammo_override", ammoOverride(ammoSpec, !ammoSpec.isBlank(), "replace"));

        boolean left = (index & 1) == 1;
        double x = left ? -0.45 : 0.45;
        double vx = left ? -0.18 : 0.18;
        String portId = ref.name() + (left ? "_left" : "_right");
        JsonObject spec = ejection("drop", guessVehicleCasing(ref.name(), ammoSpec), 60);
        spec.add("ports", ports(port(portId, x, -0.10, -0.20, vx, 0.10, -0.04, 0.05)));

        JsonObject ejection = new JsonObject();
        if (ammoSpec.isBlank()) {
            ejection.add("default", spec);
        } else {
            ejection.add("by_ammo", byAmmo(ammoSpec, spec));
        }
        rule.add("ejection", ejection);
        return rule;
    }

    private static JsonObject vehicleTemplateRule(ResourceLocation vehicleId, boolean enabled) {
        JsonObject rule = baseRule(enabled, 1000);
        rule.addProperty("comment", "Vehicle fallback candidate generated by scan. Add target.weapons, target.seats, ammo_override.allowed and ejection.by_ammo before enabling unless this should apply to every weapon on this vehicle.");

        JsonObject target = new JsonObject();
        target.add("vehicles", ids(vehicleId.toString()));
        target.add("weapons", new JsonArray());
        target.add("seats", new JsonArray());
        target.addProperty("requires_vehicle", true);
        rule.add("target", target);

        rule.add("ammo_override", ammoOverride("", false, "replace"));

        JsonObject ejection = new JsonObject();
        JsonObject spec = ejection("drop", "superbaddon:autocannon_casing", 60);
        spec.add("ports", ports(port("right_default", 0.45, -0.10, -0.20, 0.18, 0.10, -0.04, 0.05)));
        ejection.add("default", spec);
        rule.add("ejection", ejection);
        return rule;
    }

    private static JsonObject baseRule(boolean enabled, int priority) {
        JsonObject rule = new JsonObject();
        rule.addProperty("enabled", enabled);
        rule.addProperty("priority", priority);
        return rule;
    }

    private static JsonObject ammoOverride(String ammoSpec, boolean enabled, String policy) {
        JsonObject override = new JsonObject();
        override.addProperty("enabled", enabled);
        override.addProperty("policy", policy);
        override.add("allowed", ammoArray(ammoSpec));
        return override;
    }

    private static JsonObject byAmmo(String ammoSpec, JsonObject spec) {
        JsonObject table = new JsonObject();
        String key = AmmoOption.normalizeAmmoSpec(ammoSpec);
        if (!key.isBlank()) table.add(key, spec);
        return table;
    }

    private static JsonObject ejection(String mode, String casing, int pickupDelay) {
        JsonObject ejection = new JsonObject();
        ejection.addProperty("mode", mode);
        ejection.addProperty("casing", casing);
        ejection.addProperty("count", 1);
        ejection.addProperty("chance", 1.0);
        ejection.addProperty("pickup_delay", pickupDelay);
        ejection.addProperty("fallback_to_drop", false);
        return ejection;
    }

    private static String guessCasing(String ammoSpec, String itemPath) {
        String value = (ammoSpec + " " + itemPath).toLowerCase(Locale.ROOT);
        if (value.contains("shotgun")) return "superbaddon:shotgun_hull";
        if (value.contains("large_shell")) return "superbaddon:large_shell_stub";
        if (value.contains("small_shell") || value.contains("autocannon")) return "superbaddon:autocannon_casing";
        if (value.contains("grenade") || value.contains("launcher") || value.contains("hmg") || value.contains("heavy")) return "superbaddon:heavy_casing";
        return "superbaddon:brass_casing";
    }

    private static String guessVehicleCasing(String weapon, String ammoSpec) {
        String value = (weapon + " " + ammoSpec).toLowerCase(Locale.ROOT);
        if (value.contains("large_shell")) return "superbaddon:large_shell_stub";
        if (value.contains("small_shell") || value.contains("main") || value.contains("cannon")) return "superbaddon:autocannon_casing";
        if (value.contains("machinegun") || value.contains("mg") || value.contains("gun") || value.contains("rifle") || value.contains("handgun")) return "superbaddon:brass_casing";
        return "superbaddon:heavy_casing";
    }

    private static JsonArray ids(String... ids) {
        JsonArray array = new JsonArray();
        for (String raw : ids) {
            String value = ShellRule.normalizeId(raw);
            if (!value.isBlank()) array.add(value);
        }
        return array;
    }

    private static JsonArray ammoArray(String ammoSpec) {
        JsonArray array = new JsonArray();
        String value = AmmoOption.normalizeAmmoSpec(ammoSpec);
        if (!value.isBlank()) array.add(value);
        return array;
    }

    private static JsonArray ints(Collection<Integer> values) {
        JsonArray array = new JsonArray();
        for (Integer value : values) array.add(value);
        return array;
    }

    private static JsonArray ports(JsonObject... points) {
        JsonArray array = new JsonArray();
        for (JsonObject point : points) array.add(point);
        return array;
    }

    private static JsonObject port(String id, double ox, double oy, double oz, double vx, double vy, double vz, double randomVelocity) {
        JsonObject point = new JsonObject();
        point.addProperty("id", sanitizeName(id));
        point.add("offset", vec(ox, oy, oz));
        point.add("velocity", vec(vx, vy, vz));
        point.addProperty("random_velocity", randomVelocity);
        return point;
    }

    private static JsonArray vec(double x, double y, double z) {
        JsonArray array = new JsonArray();
        array.add(x);
        array.add(y);
        array.add(z);
        return array;
    }

    private static String sanitizeName(String raw) {
        String value = ShellRule.normalizeId(raw);
        return value.replace(':', '_').replace('/', '_').replace(' ', '_');
    }

    private record VehicleWeaponRef(String name, LinkedHashSet<Integer> seats, LinkedHashSet<String> allowedAmmo) {
    }

    private record VehicleScan(LinkedHashSet<ResourceLocation> vehicles,
                               LinkedHashMap<ResourceLocation, LinkedHashMap<String, VehicleWeaponRef>> createdVehicleWeapons,
                               int createdVehicleCount) {
    }

    public record ScanResult(JsonObject root,
                             LinkedHashMap<String, JsonObject> files,
                             int ruleCount,
                             int generatedFileCount,
                             int gunCount,
                             int vehicleCount,
                             int createdVehicleCount,
                             int loadedVehicleWeaponOwnerCount,
                             int scannedNamespaceCount) {
    }
}
