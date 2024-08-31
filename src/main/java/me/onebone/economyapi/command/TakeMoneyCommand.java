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
import cn.nukkit.lang.LangCode;
import cn.nukkit.lang.TranslationContainer;
import cn.nukkit.utils.TextFormat;
import me.onebone.economyapi.EconomyAPI;

import static me.onebone.economyapi.EconomyAPI.serverLangCode;

public class TakeMoneyCommand extends Command {
    private final EconomyAPI plugin;

    public TakeMoneyCommand(EconomyAPI plugin) {
        super("takemoney", "Takes money from player", "/takemoney <player> <amount>", new String[]{"withdraw"});

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
        LangCode langCode = sender instanceof Player ? ((Player) sender).getLanguageCode() : serverLangCode;
        if (!sender.hasPermission("economyapi.command.takemoney")) {
            sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage(new TranslationContainer("commands.generic.usage", this.getUsage()));
            return true;
        }
        String player = args[0];

        Player p = this.plugin.getServer().getPlayer(player);
        if (p != null) {
            player = p.getName();
        }
        try {
            double amount = Double.parseDouble(args[1]);
            if (amount < 0) {
                sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "takemoney-invalid-number"));
                return true;
            }

            int result = this.plugin.reduceMoney(player, amount);
            switch (result) {
                case EconomyAPI.RET_INVALID:
                    sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "takemoney-player-lack-of-money", player, EconomyAPI.MONEY_FORMAT.format(amount), EconomyAPI.MONEY_FORMAT.format(this.plugin.myMoney(player)), plugin.getMonetaryUnit()));
                    return true;
                case EconomyAPI.RET_NO_ACCOUNT:
                    sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "player-never-connected", player));
                    return true;
                case EconomyAPI.RET_CANCELLED:
                    sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "takemoney-failed", player));
                    return true;
                case EconomyAPI.RET_SUCCESS:
                    sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "takemoney-took-money", player, EconomyAPI.MONEY_FORMAT.format(amount), plugin.getMonetaryUnit()));
                    if (p != null) {
                        p.sendMessage(EconomyAPI.getI18n().tr(p.getLanguageCode(), "takemoney-money-taken", EconomyAPI.MONEY_FORMAT.format(amount), plugin.getMonetaryUnit()));
                    }
                    return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "takemoney-must-be-number"));
        }
        return true;
    }

}
