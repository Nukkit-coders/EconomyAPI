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

public class MyMoneyCommand extends Command {
    private final EconomyAPI plugin;

    public MyMoneyCommand(EconomyAPI plugin) {
        super("mymoney", "Shows your money", "/money", new String[]{"money", "bal", "seemoney", "balance"});

        this.plugin = plugin;

        commandParameters.clear();
        commandParameters.put("default", new CommandParameter[]{
                new CommandParameter("player", CommandParamType.TARGET, false)
        });
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!this.plugin.isEnabled()) return false;
        LangCode langCode = sender instanceof Player ? ((Player) sender).getLanguageCode() : serverLangCode;
        if (!sender.hasPermission("economyapi.command.mymoney")) {
            sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));
            return false;
        }

        String target;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(new TranslationContainer("%commands.generic.ingame"));
                return true;
            }
            target = sender.getName();
        } else {
            target = args[0];
        }

        double money = this.plugin.myMoney(target);
        if (money == -1) {
            sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "player-never-connected", args[0]));
            return true;
        }

        String moneyString = EconomyAPI.MONEY_FORMAT.format(money);
        if (sender.getName().equals(target)) {
            sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "mymoney-mymoney", moneyString, plugin.getMonetaryUnit()));
        } else {
            sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "seemoney-seemoney", target, moneyString, plugin.getMonetaryUnit()));
        }
        return true;
    }
}
