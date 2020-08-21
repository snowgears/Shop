package com.snowgears.shop.handler;

import com.snowgears.shop.Shop;
import com.snowgears.shop.gui.ShopGuiWindow;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.util.List;

public class CommandHandler extends BukkitCommand {

    private Shop plugin;

    public CommandHandler(Shop instance, String permission, String name, String description, String usageMessage, List<String> aliases) {
        super(name, description, usageMessage, aliases);
        this.setPermission(permission);
        plugin = instance;
        try {
            register();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    //TODO replace all of these messages
    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player player = (Player) sender;

                if(plugin.useGUI()) {
                    ShopGuiWindow window = plugin.getGuiHandler().getWindow(player);
                    window.open();
                }
                else {

                    //these are commands all players have access to
                    player.sendMessage(ChatColor.AQUA + "/" + this.getName() + " list" + ChatColor.GRAY + " - list your shops on the server");
                    player.sendMessage(ChatColor.AQUA + "/" + this.getName() + " currency" + ChatColor.GRAY + " - info about the currency shops use");

                    //these are commands only operators have access to
                    if (player.hasPermission("shop.operator") || player.isOp()) {
                        player.sendMessage(ChatColor.RED + "/" + this.getName() + " setcurrency" + ChatColor.GRAY + " - set the currency item to item in hand");
                        player.sendMessage(ChatColor.RED + "/" + this.getName() + " setgamble" + ChatColor.GRAY + " - set the gamble item display to item in hand");
                        player.sendMessage(ChatColor.RED + "/" + this.getName() + " item refresh" + ChatColor.GRAY + " - refresh all display items on shops");
                        player.sendMessage(ChatColor.RED + "/" + this.getName() + " reload" + ChatColor.GRAY + " - reload Shop plugin");
                    }
                }
            }
            //these are commands that can be executed from the console
            else{
                sender.sendMessage("/"+this.getName()+" list - list all shops on server");
                sender.sendMessage("/"+this.getName()+" currency - information about currency being used on server");
                sender.sendMessage("/"+this.getName()+" item refresh - refresh display items on all shops");
                sender.sendMessage("/"+this.getName()+" reload - reload Shop plugin");
            }
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("list")) {
                if (sender instanceof Player) {
                    sender.sendMessage("There are " + ChatColor.GOLD + plugin.getShopHandler().getNumberOfShops() + ChatColor.WHITE + " shops registered on the server.");
                    if(plugin.usePerms())
                        sender.sendMessage(ChatColor.GRAY+"You have built "+plugin.getShopHandler().getNumberOfShops((Player)sender) + " out of your "+ plugin.getShopListener().getBuildLimit((Player)sender) +" allotted shops.");
                    else
                        sender.sendMessage(ChatColor.GRAY+"You own "+plugin.getShopHandler().getNumberOfShops((Player)sender) + " of these shops.");
                }
                else
                    sender.sendMessage("[Shop] There are " + plugin.getShopHandler().getNumberOfShops() + " shops registered on the server.");
            }
            else if (args[0].equalsIgnoreCase("reload")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if ((plugin.usePerms() && !player.hasPermission("shop.operator")) || (!plugin.usePerms() && !player.isOp())) {
                        player.sendMessage(ChatColor.RED + "You are not authorized to use that command.");
                        return true;
                    }
                    plugin.reload();
                    sender.sendMessage("[Shop] Reloaded plugin."); //TODO replace message
                } else {
                    plugin.reload();
                    sender.sendMessage("[Shop] Reloaded plugin."); //TODO replace message
                }

                for(Player p : Bukkit.getOnlinePlayers()){
                    if(p != null){
                        p.closeInventory();
                    }
                }

            }
            else if (args[0].equalsIgnoreCase("currency")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if ((plugin.usePerms() && player.hasPermission("shop.operator")) || player.isOp()) {
                        //TODO delete all of this and replace with ShopMessage messages
                        if(plugin.useVault())
                            player.sendMessage(ChatColor.GRAY + "The server is using virtual currency through Vault.");
                        else{
                            player.sendMessage(ChatColor.GRAY + "The server is using "+plugin.getItemNameUtil().getName(plugin.getItemCurrency())+" as currency.");
                            player.sendMessage(ChatColor.GRAY + "To change this run the command '/shop setcurrency' with the item you want in your hand.");
                        }
                        return true;
                    }
                } else {
                    sender.sendMessage("The server is using "+plugin.getItemNameUtil().getName(plugin.getItemCurrency())+" as currency.");
                }
            }
            else if (args[0].equalsIgnoreCase("setcurrency")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if ((plugin.usePerms() && player.hasPermission("shop.operator")) || player.isOp()) {
                        //TODO delete all of this and replace with ShopMessage messages
                        if(plugin.useVault()) {
                            player.sendMessage(ChatColor.RED + "The server is using virtual currency through Vault and so no item could be set.");
                            return true;
                        }
                        else{
                            ItemStack handItem = player.getItemInHand();
                            if(handItem == null || handItem.getType() == Material.AIR){
                                player.sendMessage(ChatColor.RED + "You must be holding a valid item to set the shop currency.");
                                return true;
                            }
                            handItem.setAmount(1);
                            plugin.setItemCurrency(handItem);
                            player.sendMessage(ChatColor.GRAY + "The server is now using "+plugin.getItemNameUtil().getName(plugin.getItemCurrency())+" as currency.");
                        }
                        return true;
                    }
                } else {
                    sender.sendMessage("The server is using "+plugin.getItemNameUtil().getName(plugin.getItemCurrency())+" as currency.");
                }
            }
            else if(args[0].equalsIgnoreCase("setgamble")){
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if ((plugin.usePerms() && !player.hasPermission("shop.operator")) || (!plugin.usePerms() && !player.isOp())) {
                        player.sendMessage(ChatColor.RED + "You are not authorized to use that command.");
                        return true;
                    }
                    if(player.getItemInHand() != null && player.getItemInHand().getType() != Material.AIR)
                        plugin.setGambleDisplayItem(player.getItemInHand());
                    else {
                        player.sendMessage(ChatColor.RED + "You must have an item in your hand to use that command.");
                        return true;
                    }
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("item") && args[1].equalsIgnoreCase("refresh")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if ((plugin.usePerms() && !player.hasPermission("shop.operator")) || (!plugin.usePerms() && !player.isOp())) {
                        player.sendMessage(ChatColor.RED + "You are not authorized to use that command.");
                        return true;
                    }
                    plugin.getShopHandler().refreshShopDisplays();
                    sender.sendMessage("[Shop] The display items on all of the shops have been refreshed.");
                } else {
                    plugin.getShopHandler().refreshShopDisplays();
                    sender.sendMessage("[Shop] The display items on all of the shops have been refreshed.");
                }
            }
        }
        return true;
    }

    private void register()
            throws ReflectiveOperationException {
        final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        bukkitCommandMap.setAccessible(true);

        CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
        commandMap.register(this.getName(), this);
    }
}
