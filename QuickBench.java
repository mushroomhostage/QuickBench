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

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.Iterator;
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

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    public boolean isQuickBench(Block block) {
        return block.getTypeId() == QUICKBENCH_BLOCK_ID && block.getData() == QUICKBENCH_BLOCK_DATA;
    }

    public boolean isQuickBench(ItemStack item) {
        return item.getTypeId() == QUICKBENCH_ITEM_ID && item.containsEnchantment(QUICKBENCH_ITEM_TAG);
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true) 
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block != null && isQuickBench(block)) {
            plugin.log.info("clicked qb");

        }
    }


    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();

        if (isQuickBench(item)) {
            plugin.log.info("placed qb");
            event.getBlockPlaced().setTypeIdAndData(QUICKBENCH_BLOCK_ID, QUICKBENCH_BLOCK_DATA, true);
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
