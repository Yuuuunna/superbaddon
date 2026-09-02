package com.yy.superbaddon.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.yy.superbaddon.registry.SuperbAddonRecipeSerializers;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ShellReloadingRecipe implements CraftingRecipe {
    private final ResourceLocation id;
    private final String group;
    private final CraftingBookCategory category;
    private final CountedIngredient casing;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final boolean showNotification;

    public ShellReloadingRecipe(
            ResourceLocation id,
            String group,
            CraftingBookCategory category,
            CountedIngredient casing,
            NonNullList<Ingredient> ingredients,
            ItemStack result,
            boolean showNotification
    ) {
        this.id = id;
        this.group = group;
        this.category = category;
        this.casing = casing;
        this.ingredients = ingredients;
        this.result = result;
        this.showNotification = showNotification;
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        if (container.getWidth() * container.getHeight() < ingredients.size() + 1) return false;

        List<Ingredient> unmatched = new ArrayList<>(ingredients);
        boolean foundCasing = false;
        int occupiedSlots = 0;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;

            occupiedSlots++;
            if (!foundCasing && casing.test(stack)) {
                foundCasing = true;
                continue;
            }

            int match = firstMatchingIngredient(unmatched, stack);
            if (match < 0) return false;
            unmatched.remove(match);
        }

        return foundCasing && unmatched.isEmpty() && occupiedSlots == ingredients.size() + 1;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= ingredients.size() + 1;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> all = NonNullList.create();
        all.add(casing.ingredient());
        all.addAll(ingredients);
        return all;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return showNotification;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SuperbAddonRecipeSerializers.SHELL_RELOADING.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }

    public void consumeExtraCasing(CraftingContainer container) {
        int extra = casing.count() - 1;
        if (extra <= 0) return;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (casing.ingredient().test(stack)) {
                stack.shrink(extra);
                return;
            }
        }
    }

    private static int firstMatchingIngredient(List<Ingredient> ingredients, ItemStack stack) {
        for (int i = 0; i < ingredients.size(); i++) {
            if (ingredients.get(i).test(stack)) return i;
        }
        return -1;
    }

    public record CountedIngredient(Ingredient ingredient, int count) {
        public CountedIngredient {
            if (count < 1) throw new IllegalArgumentException("casing count must be positive");
        }

        public boolean test(ItemStack stack) {
            return stack.getCount() >= count && ingredient.test(stack);
        }

        private static CountedIngredient fromJson(JsonObject json) {
            Ingredient ingredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
            int count = GsonHelper.getAsInt(json, "count");
            return new CountedIngredient(ingredient, count);
        }

        private static CountedIngredient fromNetwork(FriendlyByteBuf buffer) {
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            int count = buffer.readVarInt();
            return new CountedIngredient(ingredient, count);
        }

        private void toNetwork(FriendlyByteBuf buffer) {
            ingredient.toNetwork(buffer);
            buffer.writeVarInt(count);
        }
    }

    public static final class Serializer implements RecipeSerializer<ShellReloadingRecipe> {
        @Override
        public ShellReloadingRecipe fromJson(ResourceLocation id, JsonObject json) {
            String group = GsonHelper.getAsString(json, "group", "");
            CraftingBookCategory category = CraftingBookCategory.CODEC.byName(
                    GsonHelper.getAsString(json, "category", null),
                    CraftingBookCategory.MISC
            );
            CountedIngredient casing = CountedIngredient.fromJson(GsonHelper.getAsJsonObject(json, "casing"));
            NonNullList<Ingredient> ingredients = readIngredients(GsonHelper.getAsJsonArray(json, "ingredients"));
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            boolean showNotification = GsonHelper.getAsBoolean(json, "show_notification", true);
            return new ShellReloadingRecipe(id, group, category, casing, ingredients, result, showNotification);
        }

        @Override
        public @Nullable ShellReloadingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
            CountedIngredient casing = CountedIngredient.fromNetwork(buffer);
            int ingredientCount = buffer.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.create();
            for (int i = 0; i < ingredientCount; i++) {
                ingredients.add(Ingredient.fromNetwork(buffer));
            }
            ItemStack result = buffer.readItem();
            boolean showNotification = buffer.readBoolean();
            return new ShellReloadingRecipe(id, group, category, casing, ingredients, result, showNotification);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ShellReloadingRecipe recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeEnum(recipe.category);
            recipe.casing.toNetwork(buffer);
            buffer.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                ingredient.toNetwork(buffer);
            }
            buffer.writeItem(recipe.result);
            buffer.writeBoolean(recipe.showNotification);
        }

        private static NonNullList<Ingredient> readIngredients(JsonArray json) {
            NonNullList<Ingredient> ingredients = NonNullList.create();
            for (int i = 0; i < json.size(); i++) {
                Ingredient ingredient = Ingredient.fromJson(json.get(i));
                if (!ingredient.isEmpty()) ingredients.add(ingredient);
            }
            if (ingredients.isEmpty()) {
                throw new IllegalArgumentException("shell reloading recipe requires at least one non-casing ingredient");
            }
            return ingredients;
        }
    }
}
