/*
Copyright (c) 2012, Mushroom Hostage
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.exphc.QuickBench;

import java.util.*;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.Byte;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.*;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.Material.*;
import org.bukkit.material.*;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.command.*;
import org.bukkit.inventory.*;
import org.bukkit.configuration.*;
import org.bukkit.configuration.file.*;
import org.bukkit.scheduler.*;
import org.bukkit.enchantments.*;
import org.bukkit.*;

import net.minecraft.server.CraftingManager;

import org.bukkit.craftbukkit.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.inventory.CraftItemStack;

// A crafting recipe that lets you see into its ingredients!
class TransparentRecipe {
    static QuickBench plugin; 

    // Each ingredient which must be present; outer = all, inner = any, e.g. (foo OR bar) AND (baz) AND (quux)
    // The inner set is the alternatives; it may just have one ItemStack, or more
    // This is expressiveness (not provided by Bukkit wrappers) is necessary to support ore dictionary recipes
    ArrayList<HashSet<ItemStack>> ingredientsSet;

    ItemStack result;

    public TransparentRecipe(net.minecraft.server.CraftingRecipe/*MCP IRecipe*/ opaqueRecipe) {
        // Get recipe result
        result = new CraftItemStack(opaqueRecipe.b()); // MCP getResult()
        // TODO: need to call ItemStack b(InventoryCrafting)! for IC2, it performs electric item charge/discharge!


        // Get recipe ingredients
        ingredientsSet = new ArrayList<HashSet<ItemStack>>();

        String className = opaqueRecipe.getClass().getName();

        // For vanilla recipes, Bukkit's conversion wrappers are fine
        if (opaqueRecipe instanceof net.minecraft.server.ShapelessRecipes) {
            ShapelessRecipe shapelessRecipe = ((net.minecraft.server.ShapelessRecipes)opaqueRecipe).toBukkitRecipe();
            List<ItemStack> ingredientList = shapelessRecipe.getIngredientList();

            // Shapeless recipes are a simple list of everything we need, 1:1
            for (ItemStack ingredient: ingredientList) {
                if (ingredient != null) {
                    HashSet<ItemStack> innerSet = new HashSet<ItemStack>();
                    innerSet.add(ingredient);    // no alternatives, 1-element set
                    ingredientsSet.add(innerSet);
                }
            }
        } else if (opaqueRecipe instanceof net.minecraft.server.ShapedRecipes) {
            ShapedRecipe shapedRecipe = ((net.minecraft.server.ShapedRecipes)opaqueRecipe).toBukkitRecipe();
            Map<Character,ItemStack> ingredientMap = shapedRecipe.getIngredientMap();

            // Shaped recipes' order doesn't matter for us, but the count of each ingredient in the map does
            for (String shapeLine: shapedRecipe.getShape()) {
                for (int i = 0; i < shapeLine.length(); i += 1) {
                    char code = shapeLine.charAt(i);
                    if (code == ' ') {
                        // placeholder
                        continue;
                    }

                    ItemStack ingredient = ingredientMap.get(code);
                    if (ingredient == null) {
                        // placeholder
                        continue;
                    }

                    HashSet<ItemStack> innerSet = new HashSet<ItemStack>();
                    innerSet.add(ingredient);    // no alternatives, 1-element set
                    ingredientsSet.add(innerSet);
                }
            }
        // TODO else if (className.equals("ic2.common.AdvShapelessRecipe") || className.equals("ic2.common.AdvRecipe")) {
        } else {
            throw new IllegalArgumentException("Unsupported recipe class: " + className + " of " + opaqueRecipe);
        }


        // TODO: ic2.common.AdvShapelessRecipe
        // TODO: ic2.common.AdvRecipe

        // TODO: forge.oredict.ShapedOreRecipe
        // TODO: forge.oredict.ShapelessOreRecipe


        // TODO: eloraam.core.CoverRecipe (RedPower)
        // TODO: codechicken.enderstorage.EnderChestRecipe (EnderStorage)
        // TODO: nuclearcontrol.StorageArrayRecipe (IC2 Nuclear Control 1.1.10+)

    }

    public ItemStack getResult() {
        // TODO: need to call ItemStack b(InventoryCrafting)! for IC2, it performs electric item charge/discharge!
        return result;
    }

    /** Get whether array of item stacks has all of the recipe inputs. */
    public boolean canCraft(final ItemStack[] inputs) {
        plugin.log("- testing result="+getResult()+" canCraft inputs " + inputs + " vs ingredientsSet " + ingredientsSet);

        // Clone so don't modify original
        ItemStack[] accum = cloneItemStacks(inputs);

        // Remove items as we go, ensuring we can successfully remove everything
        for (HashSet<ItemStack> alternativeIngredients: ingredientsSet) {
            boolean have = false;

            if (alternativeIngredients == null) {
                continue;
            }

            // Do we have any of the ingredients?
            for (ItemStack ingredient: alternativeIngredients) {
                if (ingredient == null) {
                    continue;
                }

                int missing = takeItems(accum, ingredient);

                plugin.log("  ~ taking "+ingredient+" missing="+missing);

                if (missing == 0) {
                    have = true;
                    break;
                }
            }

            if (!have) {
                plugin.log(" - can't craft, missing any of " + alternativeIngredients);
                return false;
            }
        }
        plugin.log(" + craftable with "+inputs);
        return true;
    }

    /** Take an item from an inventory, returning the number of items that couldn't be taken, if any. */
    private static int takeItems(ItemStack[] inventory, ItemStack goners) {
        int remaining = goners.getAmount();
        if (remaining != 1) {
            // we only expect ingredients to be of one item (otherwise, canCraft alternative loop is broken)
            throw new IllegalArgumentException("unexpected quantity from takeItems: " + goners + ", remaining="+remaining+" != 1");
        }
    
        int i = 0;

        for (ItemStack slot: inventory) {
            // matching item? (ignores tags)
            if (slot != null && slot.getTypeId() == goners.getTypeId() &&
                (goners.getDurability() == -1 || (slot.getDurability() == goners.getDurability()))) { 
                if (remaining > slot.getAmount()) {
                    remaining -= slot.getAmount();
                    slot.setAmount(0);
                } else if (remaining > 0) {
                    slot.setAmount(slot.getAmount() - remaining);
                    remaining = 0;
                } else {
                    slot.setAmount(0);
                }

                // TODO
                /*
                // If removed whole slot, need to explicitly clear it
                // ItemStacks with amounts of 0 are interpreted as 1 (possible Bukkit bug?)
                if (slot.getAmount() == 0) {
                    player.getInventory().clear(i);
                }*/
            }

            i += 1;

            if (remaining == 0) {
                break;
            }
        }

        return remaining;
    }

    private static ItemStack[] cloneItemStacks(ItemStack[] original) {
        // TODO: better way to deep copy array?
        ItemStack[] copy = new ItemStack[original.length];
        for (int i = 0; i < original.length; i += 1) {
            if (original[i] != null) {
                copy[i] = original[i].clone();
            }
        }

        return copy;
    }

    /** Return all items which can be crafted using given inputs. */
    public static List<ItemStack> precraft(ItemStack[] inputs) {
        List<ItemStack> outputs = new ArrayList<ItemStack>();
        int recipeCount = 0;

        // TODO: have a pure Bukkit API fallback in case things go wrong (like in QuickBench 2.x series; uses iterator / Bukkit.getServer().getRecipesFor, etc.)
        List opaqueRecipes = net.minecraft.server.CraftingManager.getInstance().getRecipies();

        for (Object recipeObject: opaqueRecipes) {
            net.minecraft.server.CraftingRecipe opaqueRecipe = (net.minecraft.server.CraftingRecipe)recipeObject;

            try {
                TransparentRecipe recipe = new TransparentRecipe(opaqueRecipe);

                if (recipe.canCraft(inputs)) {
                    outputs.add(recipe.getResult());
                }
            } catch (Exception e) {
                plugin.log("precraft skipping recipe: "+opaqueRecipe);
                e.printStackTrace();
            }
        }

        plugin.log("Total recipes: " + recipeCount + ", craftable: " + outputs.size());

        return outputs;
    }


}

class QuickBenchListener implements Listener {
    QuickBench plugin;

    final int QUICKBENCH_BLOCK_ID;
    final byte QUICKBENCH_BLOCK_DATA;
    final int QUICKBENCH_ITEM_ID;
    final static Enchantment QUICKBENCH_ITEM_TAG = Enchantment.FIRE_ASPECT;
    final String QUICKBENCH_TITLE;

    public QuickBenchListener(QuickBench plugin) {
        this.plugin = plugin;

        QUICKBENCH_BLOCK_ID = plugin.getConfig().getInt("quickBench.blockId", Material.LAPIS_BLOCK.getId());
        QUICKBENCH_BLOCK_DATA = (byte)plugin.getConfig().getInt("quickBench.blockData", 1);
        QUICKBENCH_ITEM_ID = plugin.getConfig().getInt("quickBench.itemId", Material.WORKBENCH.getId());
        QUICKBENCH_TITLE = plugin.getConfig().getString("quickBench.title", "QuickBench");

        loadRecipe();

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

   
    public boolean isQuickBench(Block block) {
        return block.getTypeId() == QUICKBENCH_BLOCK_ID && block.getData() == QUICKBENCH_BLOCK_DATA;
    }

    public boolean isQuickBench(ItemStack item) {
        return item.getTypeId() == QUICKBENCH_ITEM_ID && item.containsEnchantment(QUICKBENCH_ITEM_TAG);
    }

    public ItemStack getQuickBenchItem() {
        ItemStack item = new ItemStack(QUICKBENCH_ITEM_ID, 1);
        item.addUnsafeEnchantment(QUICKBENCH_ITEM_TAG, 1);

        return item;
    }

    private void loadRecipe() {
        if (plugin.getConfig().getBoolean("quickBench.enableCrafting", true)) {
            ShapelessRecipe recipe = new ShapelessRecipe(getQuickBenchItem());

            recipe.addIngredient(1, Material.WORKBENCH);
            recipe.addIngredient(1, Material.BOOK);

            Bukkit.addRecipe(recipe);
        }
    }

    // Open clicked QuickBench
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true) 
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block != null && event.getAction() == Action.RIGHT_CLICK_BLOCK && isQuickBench(block)) {
            if (!player.hasPermission("quickbench.use")) {
                String message = plugin.getConfig().getString("quickbench.useDeniedMessage", "You do not have permission to use this QuickBench.");
                if (message != null) {
                    player.sendMessage(message);
                }
                return;
            }

            List<ItemStack> outputs = TransparentRecipe.precraft(player.getInventory().getContents());

            final int ROW_SIZE = 9;
            int rows = (int)Math.max(plugin.getConfig().getInt("quickBench.minSizeRows", 0), Math.ceil(outputs.size() * 1.0 / ROW_SIZE));

            // Note: >54 still shows dividing line on client, but can interact
            Inventory inventory = Bukkit.createInventory(player, ROW_SIZE * rows, QUICKBENCH_TITLE);

            for (int i = 0; i < Math.min(outputs.size(), inventory.getSize()); i += 1) {
                inventory.setItem(i, outputs.get(i));
            }

            player.openInventory(inventory);

            // don't let, for example, place a block AND open the QuickBench..
            event.setCancelled(true);
        }
    }

    public List<Recipe> getRecipesForX(ItemStack item) {
        // XXX: either implement, or replace with click-location-based tracking (more reliable? for charging)
        return null;
    }


    /** Get whether the item stack is contained within an array of item stacks. */
    public boolean haveItems(ItemStack[] inputs, ItemStack check) {
        if (check == null) {    
            // everyone has nothing
            return true;
        }

        int type = check.getTypeId();
        short damage = check.getDurability();
        int count = check.getAmount();

        for (ItemStack input: inputs) {
            if (input == null) {
                continue;
            }

            // match types and damage
            if (input.getTypeId() != type) {
                continue;
            }

            if (damage != -1 && damage != input.getDurability()) {
                continue;
            }

            // ignore enchantments

            // consume what we need from what they have
            if (input.getAmount() >= count) {
                count -= input.getAmount();
                if (count <= 0) {
                    break;
                }
            }
        }

        // if matched everything, and then some
        return count <= 0;
    }

    // XXX: replace by TransparentRecipe
    Collection<ItemStack> getRecipeInputs(Recipe recipe) {
        return (recipe instanceof ShapedRecipe) ?  ((ShapedRecipe)recipe).getIngredientMap().values() : ((ShapelessRecipe)recipe).getIngredientList();
    }

    // XXX: replace
    public ItemStack[] cloneItemStacks(ItemStack[] original) {
        // TODO: better way to deep copy array?
        ItemStack[] copy = new ItemStack[original.length];
        for (int i = 0; i < original.length; i += 1) {
            if (original[i] != null) {
                copy[i] = original[i].clone();
            }
        }

        return copy;
    }


    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onInventoryClickWrapper(InventoryClickEvent event) {
        // If something goes wrong, deny the event to try to avoid duping items
        try {
            onInventoryClick(event);
        } catch (Exception e) {
            plugin.logger.warning("onInventoryClick exception: " + e);
            e.printStackTrace();

            event.setResult(Event.Result.DENY);
        }
    }

    // Craft on inventory click
    // do NOT add EventHandler here
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();

        if (view == null || view.getTitle() == null || !view.getTitle().equals(QUICKBENCH_TITLE)) {
            return;
        }

        // Click to craft

        HumanEntity player = event.getWhoClicked();
        ItemStack item = event.getCurrentItem();

        plugin.log("click "+event);
        plugin.log("cur item = "+item);
        plugin.log("shift = "+event.isShiftClick());
        // TODO: shift-click to craft all?
        plugin.log("raw slot = "+event.getRawSlot());

        if (event.getRawSlot() >= view.getTopInventory().getSize()) {
            // clicked player inventory (bottom)

            if (event.isShiftClick()) {
                // shift-click would player inventory -> quickbench, deny
                event.setResult(Event.Result.DENY);
            }

            // otherwise, let manipulate their own player inventory
            return;
        }

        if (item == null || item.getType() == Material.AIR) {
            // dropped item (raw slot -999) or empty slot
            event.setResult(Event.Result.DENY);
            return;
        }

        Inventory playerInventory = view.getBottomInventory();
        ItemStack[] playerContents = playerInventory.getContents();

        // Remove crafting inputs
        plugin.logger.warning("TODO: remove inputs for "+item);
        event.setResult(Event.Result.DENY);
        boolean crafted = false;

        /* XXX TODO - should we backtrack recipe from clicked input? or better yet, track position of click in window?!
        List<Recipe> recipes = getRecipesForX(item);
        if (recipes == null) {
            plugin.logger.warning("No recipes for "+item);
            event.setResult(Event.Result.DENY);
            return;
        }

        for (Recipe recipe: recipes) {
            if (canCraft(playerContents, recipe)) {

                Collection<ItemStack> inputs = getRecipeInputs(recipe);

                plugin.log(" craft "+recipe+" inputs="+inputs);

                // Remove items from recipe from player inventory
                for (ItemStack input: inputs) {
                    if (input == null) {
                        continue;
                    }

                    int missing = takeItems(playerContents, input);

                    if (missing != 0) {
                        plugin.logger.warning("Failed to remove crafting inputs "+inputs+" for player "+player.getName()+" crafting "+item+", missing "+missing);
                        event.setResult(Event.Result.DENY);
                        return;
                    }
                }

                playerInventory.setContents(playerContents);
                crafted = true;
                break;
            }
        }*/

        if (!crafted) {
            plugin.logger.warning("Failed to find matching recipe from player "+player.getName()+" for crafting "+item);
            // don't let pick up
            event.setResult(Event.Result.DENY);
            return;
        }

        // add to player inventory when clicked
        HashMap<Integer,ItemStack> overflow = view.getBottomInventory().addItem(item);

        // drop excess items on the floor (easier than denying the event.. maybe better?)
        for (ItemStack excessItem: overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), excessItem);
        }


        // Populate with new items, either adding (if have new crafting inputs) or removing (if took up all)
        List<ItemStack> newItems = TransparentRecipe.precraft(playerContents);

        if (newItems.size() > view.getTopInventory().getSize()) {
            // TODO: improve.. but can't resize window? close and reopen
            ((Player)player).sendMessage("More crafting outputs available than shown here - reopen to see full list!");
            newItems = newItems.subList(0, view.getTopInventory().getSize());
        }

        view.getTopInventory().setContents(itemStackArray(newItems));

        // don't let pick up
        event.setResult(Event.Result.DENY);
    }

    public ItemStack[] itemStackArray(List<ItemStack> list) {
        ItemStack[] array = new ItemStack[list.size()];

        // TODO: list.toArray()? returns Object[]
        int i = 0;
        for (ItemStack item: list) {
            array[i] = list.get(i);
            i += 1;
        }

        return array;
    }


    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true) 
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryView view = event.getView();

        if (view == null || view.getTitle() == null || !view.getTitle().equals(QUICKBENCH_TITLE)) {
            // not for us
            return;
        }

        Inventory playerInventory = view.getTopInventory();

        Inventory benchInventory = view.getBottomInventory();
    }

    // QuickBench block <-> item

    // Place item -> block
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();

        if (isQuickBench(item)) {
            if (event.getPlayer().hasPermission("quickbench.place")) {
                // place quickbench item as lapis block
                event.getBlockPlaced().setTypeIdAndData(QUICKBENCH_BLOCK_ID, QUICKBENCH_BLOCK_DATA, true);
            } else {
                String message = plugin.getConfig().getString("quickbench.placeDeniedMessage", "You do not have permission to place this QuickBench.");
                if (message != null) {
                    event.getPlayer().sendMessage(message);
                }

                event.setCancelled(true);
            }
        }
    }

    // Break block -> item
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block != null && isQuickBench(block)) {
            if (event.getPlayer().hasPermission("quickbench.destroy")) {
                // break tagged lapis block as quickbench item
                ItemStack item = getQuickBenchItem();

                block.setType(Material.AIR);
                block.getWorld().dropItemNaturally(block.getLocation(), item);

                event.setCancelled(true);
            } else {
                String message = plugin.getConfig().getString("quickbench.destroyDeniedMessage", "You do not have permission to destroy this QuickBench.");
                if (message != null) {
                    event.getPlayer().sendMessage(message);
                }

                event.setCancelled(true);
            }
        }
    }
}

public class QuickBench extends JavaPlugin {
    Logger logger = Logger.getLogger("Minecraft");

    public void onEnable() {
        TransparentRecipe.plugin = this;

        getConfig().options().copyDefaults(true);
        saveConfig();
        reloadConfig();

        new QuickBenchListener(this);
    }

    public void onDisable() {
    }

    public void log(String message) {
        if (getConfig().getBoolean("verbose", false)) {
            logger.info(message);
        }
    }

}
