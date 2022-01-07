package com.snowgears.shop.handler;

import com.snowgears.shop.Shop;
import com.snowgears.shop.gui.ShopGuiWindow;
import com.snowgears.shop.util.CurrencyType;
import com.snowgears.shop.util.ItemListType;
import com.snowgears.shop.util.PlayerSettings;
import com.snowgears.shop.util.ShopMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
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

    private void sendCommandMessage(String subType, Player player) {
        String message = ShopMessage.getMessage("command", subType, null, player);
        if(message != null && !message.isEmpty())
            player.sendMessage(message);
    }

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

                    //used for getting some large item eulerstand angles when new items are added in
                    //for(Entity e : player.getNearbyEntities(1,1,1)){
                    //    if(e instanceof ArmorStand){
                    //        System.out.println(UtilMethods.getEulerAngleString(((ArmorStand)e).getRightArmPose()));
                    //        System.out.println(UtilMethods.getEulerAngleString(((ArmorStand)e).getBodyPose()));
                    //    }
                    //}

                    //these are commands all players have access to
                    sendCommandMessage("list", player);
                    sendCommandMessage("currency", player);

                    //these are commands only operators have access to
                    if (player.hasPermission("shop.operator") || player.isOp()) {
                        sendCommandMessage("setcurrency", player);
                        sendCommandMessage("setgamble", player);
                        sendCommandMessage("itemrefresh", player);
                        if(plugin.getItemListType() != ItemListType.NONE){
                            sendCommandMessage("itemlist", player);
                        }
                        sendCommandMessage("reload", player);
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
                    Player player = (Player)sender;
                    sendCommandMessage("list_output_total", player);
                    if(plugin.usePerms())
                        sendCommandMessage("list_output_perms", player);
                    else
                        sendCommandMessage("list_output_noperms", player);
                }
                else
                    sender.sendMessage("[Shop] There are " + plugin.getShopHandler().getNumberOfShops() + " shops registered on the server.");
            }
            else if (args[0].equalsIgnoreCase("reload")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if ((plugin.usePerms() && !player.hasPermission("shop.operator")) || (!plugin.usePerms() && !player.isOp())) {
                        sendCommandMessage("not_authorized", player);
                        return true;
                    }
                    plugin.reload();
                    sendCommandMessage("reload_output", player);
                } else {
                    plugin.reload();
                    sender.sendMessage("[Shop] Reloaded plugin.");
                }

                for(Player p : Bukkit.getOnlinePlayers()){
                    if(p != null){
                        p.closeInventory();
                    }
                }
                //plugin.getShopHandler().refreshShopDisplays(null);
                plugin.getShopHandler().removeLegacyDisplays();

            }
            else if (args[0].equalsIgnoreCase("currency")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if ((plugin.usePerms() && player.hasPermission("shop.operator")) || player.isOp()) {

                        sendCommandMessage("currency_output", player);
                        sendCommandMessage("currency_output_tip", player);
                        return true;
                    }
                } else {
                    sender.sendMessage("The server is using "+plugin.getCurrencyName()+" as currency.");
                }
            }
            else if (args[0].equalsIgnoreCase("setcurrency")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if ((plugin.usePerms() && player.hasPermission("shop.operator")) || player.isOp()) {
                        if(plugin.getCurrencyType() != CurrencyType.ITEM){
                            sendCommandMessage("error_novault", player);
                            return true;
                        }
                        else{
                            ItemStack handItem = player.getInventory().getItemInHand();
                            if(handItem == null || handItem.getType() == Material.AIR){
                                sendCommandMessage("error_nohand", player);
                                return true;
                            }
                            handItem.setAmount(1);
                            plugin.setItemCurrency(handItem);
                            sendCommandMessage("setcurrency_output", player);
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
                        sendCommandMessage("not_authorized", player);
                        return true;
                    }
                    if(player.getInventory().getItemInHand() != null && player.getInventory().getItemInHand().getType() != Material.AIR)
                        plugin.setGambleDisplayItem(player.getInventory().getItemInHand());
                    else {
                        sendCommandMessage("error_nohand", player);
                        return true;
                    }
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("item") && args[1].equalsIgnoreCase("refresh")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if ((plugin.usePerms() && !player.hasPermission("shop.operator")) || (!plugin.usePerms() && !player.isOp())) {
                        sendCommandMessage("not_authorized", player);
                        return true;
                    }
                    //plugin.getShopHandler().refreshShopDisplays(null);
                    plugin.getShopHandler().removeLegacyDisplays();
                    sendCommandMessage("itemrefresh_output", player);
                } else {
                    //plugin.getShopHandler().refreshShopDisplays(null);
                    plugin.getShopHandler().removeLegacyDisplays();
                    sender.sendMessage("[Shop] The display items on all of the shops have been refreshed.");
                }
            }
            else if (args[0].equalsIgnoreCase("itemlist")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if ((plugin.usePerms() && !player.hasPermission("shop.operator")) || (!plugin.usePerms() && !player.isOp())) {
                        sendCommandMessage("not_authorized", player);
                        return true;
                    }
                    if(args[1].equalsIgnoreCase("add")){
                        plugin.getShopHandler().addInventoryToItemList(player.getInventory());
                        sendCommandMessage("itemlist_add", player);
                    }
                    else if(args[1].equalsIgnoreCase("remove")){
                        plugin.getShopHandler().removeInventoryFromItemList(player.getInventory());
                        sendCommandMessage("itemlist_remove", player);
                    }
                } else {
                    sender.sendMessage("[Shop] This command can only be run as a player.");
                }
            }
            else if (args[0].equalsIgnoreCase("notify")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if(args[1].equalsIgnoreCase("user")) {
                        toggleOptionAndNotifyPlayer(player, PlayerSettings.Option.SALE_USER_NOTIFICATIONS);
                    }
                    else if(args[1].equalsIgnoreCase("owner")) {
                        toggleOptionAndNotifyPlayer(player, PlayerSettings.Option.SALE_OWNER_NOTIFICATIONS);
                    }
                    else if(args[1].equalsIgnoreCase("stock")) {
                        toggleOptionAndNotifyPlayer(player, PlayerSettings.Option.STOCK_NOTIFICATIONS);
                    }
                } else {
                    sender.sendMessage("[Shop] This command can only be run as a player.");
                }
            }
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        List<String> results = new ArrayList<>();
        if (args.length == 0) {
            results.add(this.getName());
        }
        else if (args.length == 1) {
            results.add("list");
            results.add("currency");

            boolean showOperatorCommands = false;
            if(sender instanceof Player){
                Player player = (Player)sender;

                if(player.hasPermission("shop.operator") || player.isOp()) {
                    showOperatorCommands = true;
                }
            }
            else{
                showOperatorCommands = true;
            }

            if(showOperatorCommands){
                results.add("setcurrency");
                results.add("setgamble");
                results.add("item");
                results.add("notify");
                if(Shop.getPlugin().getItemListType() != ItemListType.NONE){
                    results.add("itemlist");
                }
                results.add("reload");
            }
            return sortedResults(args[0], results);
        }
        else if (args.length == 2) {
            if(args[0].equalsIgnoreCase("notify")){
                results.add("user");
                results.add("owner");
                results.add("stock");
            }

            boolean showOperatorCommands = false;
            if(sender instanceof Player){
                Player player = (Player)sender;

                if(player.hasPermission("shop.operator") || player.isOp()) {
                    showOperatorCommands = true;
                }
            }
            else{
                showOperatorCommands = true;
            }

            if(showOperatorCommands){
                if(Shop.getPlugin().getItemListType() != ItemListType.NONE){
                    if(args[0].equalsIgnoreCase("itemlist")) {
                        results.add("add");
                        results.add("remove");
                    }
                }
                if(args[0].equalsIgnoreCase("item")) {
                    results.add("refresh");
                }
            }
            return sortedResults(args[1], results);
        }
        return results;
    }

    private void toggleOptionAndNotifyPlayer(Player player, PlayerSettings.Option option) {
        Shop.getPlugin().getGuiHandler().toggleSettingsOption(player, option);

        switch (option) {
            case SALE_USER_NOTIFICATIONS:
                sendCommandMessage("notify_user", player);
                break;
            case SALE_OWNER_NOTIFICATIONS:
                sendCommandMessage("notify_owner", player);
                break;
            case STOCK_NOTIFICATIONS:
                sendCommandMessage("notify_stock", player);
                break;
        }
    }

    private void register()
            throws ReflectiveOperationException {
        final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        bukkitCommandMap.setAccessible(true);

        CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
        commandMap.register(this.getName(), this);
    }

    // Sorts possible results to provide true tab auto complete based off of what is already typed.
    public List <String> sortedResults(String arg, List<String> results) {
        final List < String > completions = new ArrayList < > ();
        StringUtil.copyPartialMatches(arg, results, completions);
        Collections.sort(completions);
        results.clear();
        for (String s: completions) {
            results.add(s);
        }
        return results;
    }
}
