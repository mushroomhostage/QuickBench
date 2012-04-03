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

package me.exphc.QuickBench;

import java.util.*;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Formatter;
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

class QuickBenchListener implements Listener {
    QuickBench plugin;

    final int QUICKBENCH_BLOCK_ID;
    final byte QUICKBENCH_BLOCK_DATA;
    final int QUICKBENCH_ITEM_ID;
    final static Enchantment QUICKBENCH_ITEM_TAG = Enchantment.FIRE_ASPECT;

    public QuickBenchListener(QuickBench plugin) {
        this.plugin = plugin;

        QUICKBENCH_BLOCK_ID = plugin.getConfig().getInt("quickBench.blockId", Material.LAPIS_BLOCK.getId());
        QUICKBENCH_BLOCK_DATA = (byte)plugin.getConfig().getInt("quickBench.blockData", 1);
        QUICKBENCH_ITEM_ID = plugin.getConfig().getInt("quickBench.itemId", Material.WORKBENCH.getId());

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
        if (!plugin.getConfig().getBoolean("enableCrafting", true)) {
            return;
        }

        ShapelessRecipe recipe = new ShapelessRecipe(getQuickBenchItem());

        recipe.addIngredient(1, Material.WORKBENCH);
        recipe.addIngredient(1, Material.BOOK);

        Bukkit.addRecipe(recipe);
    }
 
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true) 
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block != null && isQuickBench(block)) {
            plugin.log.info("clicked qb");

            List<ItemStack> outputs = precraft(player.getInventory().getContents());

            final int ROW_SIZE = 9;
            int rows = (int)Math.max(1, Math.ceil(outputs.size() * 1.0 / ROW_SIZE));

            // Note: >54 still shows dividing line on client, but can interact
            Inventory inventory = Bukkit.createInventory(player, ROW_SIZE * rows, "QuickBench");

            for (ItemStack output: outputs) {
                inventory.addItem(output);
            }

            player.openInventory(inventory);
        }
    }

    public List<ItemStack> precraft(ItemStack[] inputs) {
        List<ItemStack> outputs = new ArrayList<ItemStack>();

        // Set of all available input items
        Set<ItemStack> inputsSet = new HashSet<ItemStack>();

        for (ItemStack input: inputs) {
            inputsSet.add(input);
        }


        Iterator<Recipe> recipes = Bukkit.getServer().recipeIterator();

        RECIPE: while(recipes.hasNext()) {
            Recipe recipe = recipes.next();

            //plugin.log.info("recipe "+recipe);

            if (recipe instanceof ShapedRecipe) {
                Collection<ItemStack> recipeInputs = ((ShapedRecipe)recipe).getIngredientMap().values();

                for (ItemStack recipeInput: recipeInputs) {
                    if (inputsSet.contains(recipeInput)) {
                        //plugin.log.info("have "+recipeInput);
                    } else {
                        //plugin.log.info("missing "+recipeInput);
                        continue RECIPE;
                    }
                }
                // we can craft it!

                outputs.add(recipe.getResult());
            }
            // TODO: shapeless
        }

        return outputs;
    }

    // not in 1.2.4-R1.0 :(
    /*
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onInventoryClick(InventoryClickEvent event) {
        plugin.log.info("click "+event);
        plugin.log.info("cur item = "+event.getCurrentItem());
        plugin.log.info("shift = "+event.isShiftClick());
    }
    */


    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();

        if (isQuickBench(item)) {
            plugin.log.info("placed qb");
            // place quickbench item as lapis block
            event.getBlockPlaced().setTypeIdAndData(QUICKBENCH_BLOCK_ID, QUICKBENCH_BLOCK_DATA, true);
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block != null && isQuickBench(block)) {
            // break tagged lapis block as quickbench item
            ItemStack item = getQuickBenchItem();

            block.setType(Material.AIR);
            block.getWorld().dropItemNaturally(block.getLocation(), item);

            event.setCancelled(true);
        }
    }
}

public class QuickBench extends JavaPlugin {
    Logger log = Logger.getLogger("Minecraft");

    public void onEnable() {
        new QuickBenchListener(this);
    }

    public void onDisable() {
    }

}
