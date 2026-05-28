package me.onebone.economyapi.command;

/*
 * EconomyAPI: Core of economy system for Nukkit
 * Copyright (C) 2016  onebone <jyc00410@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.lang.TranslationContainer;
import cn.nukkit.utils.TextFormat;
import me.onebone.economyapi.EconomyAPI;

public class PayCommand extends Command {
    private final EconomyAPI plugin;

    public PayCommand(EconomyAPI plugin) {
        super("pay", "Pays to other player", "/pay <player> <amount>");

        this.plugin = plugin;

        // command parameters
        commandParameters.clear();
        commandParameters.put("default", new CommandParameter[]{
                new CommandParameter("player", CommandParamType.TARGET, false),
                new CommandParameter("amount", CommandParamType.FLOAT, false)
        });
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!this.plugin.isEnabled()) return false;
        if (!sender.hasPermission("economyapi.command.pay")) {
            sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(new TranslationContainer("%commands.generic.ingame"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(new TranslationContainer("commands.generic.usage", this.getUsage()));
            return true;
        }
        String player = args[0];

        Player p = this.plugin.getServer().getPlayer(player);
        if (p != null) {
            if (sender == p) {
                sender.sendMessage(this.plugin.getMessage("pay-failed-self", sender));
                return true;
            }

            player = p.getName();
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(this.plugin.getMessage("takemoney-must-be-number", sender));
            return true;
        }

        if (amount < 0.01 || !Double.isFinite(amount)) {
            sender.sendMessage(this.plugin.getMessage("pay-too-low", sender));
            return true;
        }

        if (!this.plugin.hasAccount(player)) {
            sender.sendMessage(this.plugin.getMessage("player-never-connected", new String[]{player}, sender));
            return true;
        }

        int result = this.plugin.reduceMoney((Player) sender, amount);
        switch (result) {
            case EconomyAPI.RET_NO_ACCOUNT:
                sender.sendMessage(this.plugin.getMessage("player-never-connected", new String[]{player}, sender));
                break;
            case EconomyAPI.RET_CANCELLED:
            case EconomyAPI.RET_INVALID:
                sender.sendMessage(this.plugin.getMessage("pay-failed", sender));
                break;
            case EconomyAPI.RET_SUCCESS:
                this.plugin.addMoney(player, amount, true);

                sender.sendMessage(this.plugin.getMessage("pay-success", new String[]{EconomyAPI.MONEY_FORMAT.format(amount), player}, sender));
                if (p != null) {
                    p.sendMessage(this.plugin.getMessage("money-paid", new String[]{sender.getName(), EconomyAPI.MONEY_FORMAT.format(amount)}, sender));
                }
                break;
        }
        return true;
    }

}
