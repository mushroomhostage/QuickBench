QuickBench - crafting without recipes

Features:

* No clients mods required
* Adds a craftable and placeable QuickBench item
* Open the QuickBench to show all craftable items
* Click item to craft automatically
* Eliminates need to remember recipes

## Usage

Create a QuickBench by crafting a Crafting Table + Book.

Right-click to place the QuickBench in the world.

Click the QuickBench to open a list of craftable items.

Click the items you want to craft. The crafting inputs will be taken
from your inventory and the result 

## Configuration

* **verbose** (false): Enable for extra logging for diagnostic purposes.

* **quickBench**

 * **blockId** (22): Block ID for QuickBench placed in the world. Default is Lapis Lazuli Block to differentiate from 
   crafting tables. Note that the blockData below must also match; not all Lapis Lazuli Blocks will become QuickBenches.

 * **blockData** (1): Data value for block to be identified as a QuickBench, used in conjunction with blockId.

 * **itemId** (58): Item ID for QuickBench when held as an item. Default is Crafting Table.

 * **title** (QuickBench): Title for QuickBench inventory windows.

 * **enableCrafting** (true): Add crafting recipe, Crafting Table + Book = QuickBench. You can disable this
if you want to use a custom crafting recipe added by another plugin or provide some other way to acquire
QuickBenches (item is identified by itemId + Fire Aspect I enchantment).

 * **minSizeRows** (0): Minimum number of rows to show in QuickBench output inventory window. The default of 0
will cause a blank inventory to be shown if there is nothing to craft. Note that if during crafting additional items
become available beyond what can be shown in the window, the player must close and reopen the QuickBench to see
them - to mitigate this problem, you can set minSizeRows to a higher value to provide extra room. 6 is equivalent to a large chest.

## Limitations

If more than 6 inventory rows are shown, the client will display the dividing line between the inventories incorrectly (client bug?).
However, the items in the slots can still be picked up as you would expect.

If there are too many craftable items, not all can be shown. Close and reopen the QuickBench or temporarily drop items as a workaround.

Some unusual recipes or those added by mods may not behave as expected; please report any problems.

## See also

* [Crafting Table II](http://www.minecraftforum.net/topic/856538-11-crafting-table-ii-v162-310112/)

