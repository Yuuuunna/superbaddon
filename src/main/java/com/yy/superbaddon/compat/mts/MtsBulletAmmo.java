package com.yy.superbaddon.compat.mts;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MinecraftTransportSimulator / Immersive Vehicles bullet-item bridge.
 *
 * <p>MTS packs its round count into the {@code bulletQty} root NBT key (falling back to the
 * bullet definition's default quantity) and treats one item stack as many rounds.  SuperbWarfare's
 * {@code AmmoConsumer} instead assumes "one item count = one round" and matches by full NBT, so an
 * MTS bullet is neither recognised nor consumed correctly.  This helper reads, counts, consumes and
 * mints MTS bullet quantities so the consumer can speak rounds.</p>
 *
 * <p>MTS is not a build dependency.  Everything here is reflection + NBT so the addon keeps loading
 * when MTS is absent.</p>
 */
public final class MtsBulletAmmo {
    private static final String BULLET_QTY_KEY = "bulletQty";

    /** Item -> default per-item round count.  {@code -1} means "not an MTS bullet". */
    private static final Map<Item, Integer> DEFAULT_QTY_CACHE = new ConcurrentHashMap<>();

    /**
     * Lenient ammo-spec parser.  Unlike SuperbWarfare's {@code AMMO_PATTERN} (id group {@code \w+(:\w+)?})
     * this allows the dot in MTS registration names ({@code packID.systemName}), so {@code mts:pack.bullet}
     * survives parsing.  Trailing SNBT data and the {@code @}/{@code #} prefixes are tolerated.
     */
    private static final Pattern LENIENT_SPEC =
            Pattern.compile("^(?<count>\\d+)?\\s*(?<prefix>[@#]?)(?<id>[\\w.:/-]+)\\s*(?<data>\\{.*})?$");

    private MtsBulletAmmo() {
    }

    /**
     * Resolve the bullet {@link Item} a SuperbWarfare ammo spec points at, or {@code null} when the spec
     * is not a plain MTS bullet item reference.  The {@code @} prefix (player ammo) is rejected.
     *
     * <p>SuperbWarfare's own {@code AmmoConsumer.init()} cannot parse a dotted MTS id, so this is used to
     * set the consumer up out-of-band before that regex would bail.</p>
     */
    public static Item resolveBulletItem(String spec) {
        if (spec == null) return null;
        String trimmed = spec.trim();
        if (trimmed.isEmpty()) return null;

        Matcher matcher = LENIENT_SPEC.matcher(trimmed);
        if (!matcher.matches()) return null;
        if ("@".equals(matcher.group("prefix"))) return null;

        ResourceLocation location = ResourceLocation.tryParse(matcher.group("id"));
        if (location == null) return null;

        Item item = ForgeRegistries.ITEMS.getValue(location);
        if (item == null || item == Items.AIR) return null;
        return isBullet(new ItemStack(item)) ? item : null;
    }

    /**
     * True when the stack is an MTS bullet item.  Cheap after the first lookup per item type.
     */
    public static boolean isBullet(ItemStack stack) {
        return stack != null && !stack.isEmpty() && defaultQty(stack.getItem()) >= 0;
    }

    /**
     * Total rounds represented by a single inventory stack.  Uses the {@code bulletQty} NBT when
     * present (each tagged item carries its own remainder), otherwise the definition default; either
     * way multiplied by the stack count.
     */
    public static int qty(ItemStack stack) {
        int def = defaultQty(stack.getItem());
        if (def < 0 || stack.isEmpty()) return 0;

        CompoundTag tag = stack.getTag();
        int per = (tag != null && tag.contains(BULLET_QTY_KEY)) ? Math.max(0, tag.getInt(BULLET_QTY_KEY)) : def;
        return per * stack.getCount();
    }

    /**
     * Sum of rounds across every slot holding the given bullet item.
     */
    public static int countQty(IItemHandler handler, Item matchItem) {
        if (handler == null || matchItem == null) return 0;

        long total = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.getItem() == matchItem) total += qty(stack);
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    /**
     * Consume {@code rounds} rounds of the given bullet item from the handler.
     *
     * <p>A slot whose rounds are fully spent is extracted whole.  A slot only partially spent is
     * extracted whole and a single replacement item carrying the remainder {@code bulletQty} is
     * re-inserted, which sidesteps the ambiguity of a multi-count stack without a per-item tag.</p>
     *
     * @return the number of rounds actually consumed
     */
    public static int consumeQty(IItemHandler handler, Item matchItem, int rounds) {
        if (handler == null || matchItem == null || rounds <= 0) return 0;

        int remaining = rounds;
        for (int i = 0; i < handler.getSlots() && remaining > 0; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.getItem() != matchItem) continue;

            int avail = qty(stack);
            if (avail <= 0) continue;

            if (remaining >= avail) {
                handler.extractItem(i, stack.getCount(), false);
                remaining -= avail;
            } else {
                int left = avail - remaining;
                handler.extractItem(i, stack.getCount(), false);
                handler.insertItem(i, makeStack(matchItem, left), false);
                remaining = 0;
            }
        }
        return rounds - remaining;
    }

    /**
     * Return {@code rounds} rounds to a handler as a single bullet item carrying the {@code bulletQty}
     * NBT, so unloading or swapping ammo does not duplicate (or shred) the round count.
     */
    public static int insertQty(IItemHandler handler, Item matchItem, int rounds) {
        if (handler == null || matchItem == null || rounds <= 0) return 0;

        ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, makeStack(matchItem, rounds), false);
        return rounds - qty(remainder);
    }

    /**
     * Return {@code rounds} rounds to a player as a single bullet item carrying the {@code bulletQty}
     * NBT, dropping to the world when the inventory is full (vanilla give behaviour).
     */
    public static int insertQty(Player player, Item matchItem, int rounds) {
        if (player == null || matchItem == null || rounds <= 0) return 0;

        ItemHandlerHelper.giveItemToPlayer(player, makeStack(matchItem, rounds));
        return rounds;
    }

    private static ItemStack makeStack(Item item, int rounds) {
        ItemStack stack = new ItemStack(item, 1);
        stack.getOrCreateTag().putInt(BULLET_QTY_KEY, Math.max(0, rounds));
        return stack;
    }

    private static int defaultQty(Item item) {
        if (item == null) return -1;
        return DEFAULT_QTY_CACHE.computeIfAbsent(item, MtsBulletAmmo::resolveDefaultQty);
    }

    /**
     * Reflectively walks {@code BuilderItem.getWrappedItem().definition.bullet.quantity}.  Any item
     * that is not an MTS bullet (no wrapper, no {@code bullet} definition) resolves to {@code -1}.
     */
    private static int resolveDefaultQty(Item item) {
        try {
            Method getWrapped = findMethod(item.getClass(), "getWrappedItem");
            if (getWrapped == null) return -1;
            getWrapped.setAccessible(true);
            Object wrapped = getWrapped.invoke(item);
            if (wrapped == null) return -1;

            Field definitionField = findField(wrapped.getClass(), "definition");
            if (definitionField == null) return -1;
            definitionField.setAccessible(true);
            Object definition = definitionField.get(wrapped);
            if (definition == null) return -1;

            Field bulletField = findField(definition.getClass(), "bullet");
            if (bulletField == null) return -1;
            bulletField.setAccessible(true);
            Object bullet = bulletField.get(definition);
            if (bullet == null) return -1;

            Field quantityField = findField(bullet.getClass(), "quantity");
            if (quantityField == null) return -1;
            quantityField.setAccessible(true);
            return Math.max(1, quantityField.getInt(bullet));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return -1;
        }
    }

    private static Method findMethod(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getMethod(name);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        // getMethod already searches interfaces/superclasses for public methods; the loop is a
        // belt-and-braces fallback for unusual class hierarchies.
        try {
            return type.getMethod(name);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /** Convenience: resolve an entity's item handler, or {@code null}. */
    public static IItemHandler handlerOf(net.minecraft.world.entity.Entity entity) {
        if (entity == null) return null;
        return entity.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null);
    }
}
