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

import cn.nukkit.IPlayer;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.lang.LangCode;
import cn.nukkit.lang.TranslationContainer;
import cn.nukkit.utils.TextFormat;
import me.onebone.economyapi.EconomyAPI;

import java.util.*;

import static me.onebone.economyapi.EconomyAPI.serverLangCode;

public class TopMoneyCommand extends Command {
    private final EconomyAPI plugin;

    public TopMoneyCommand(EconomyAPI plugin) {
        super("topmoney", "Shows top money of this server", "/topmoney [page]", new String[]{"baltop", "balancetop"});

        this.plugin = plugin;
    }

    private static String getName(String possibleUuid) {
        UUID uuid;
        try {
            uuid = UUID.fromString(possibleUuid);
        } catch (Exception e) {
            return possibleUuid;
        }

        IPlayer player = Server.getInstance().getOfflinePlayer(uuid);
        if (player != null && player.getName() != null) {
            return player.getName();
        }
        return possibleUuid;
    }

	@Override
    public boolean execute(final CommandSender sender, String label, final String[] args) {
        if (!this.plugin.isEnabled()) return false;
        LangCode langCode = sender instanceof Player ? ((Player) sender).getLanguageCode() : serverLangCode;
        if (!sender.hasPermission("economyapi.command.topmoney")) {
            sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));
            return false;
        }

        try {
            int arg = args.length > 0 ? Integer.parseInt(args[0]) : 1;
            final LinkedHashMap<String, Double> money = new LinkedHashMap<>(plugin.getAllMoney());
            sender.getServer().getScheduler().scheduleTask(EconomyAPI.getInstance(), () -> {
                int page = args.length > 0 ? Math.max(1, Math.min(arg, money.size())) : 1;
                List<String> list = new LinkedList<>(money.keySet());
                list.sort((s1, s2) -> Double.compare(money.get(s2), money.get(s1)));
                StringBuilder output = new StringBuilder();
                output.append(EconomyAPI.getI18n().tr(langCode, "topmoney-tag", Integer.toString(page), Integer.toString(((money.size() + 6) / 5)))).append("\n");
                if (page == 1) {
                    double total = 0;
                    for (double val : money.values()) {
                        total += val;
                    }
                    output.append(EconomyAPI.getI18n().tr(langCode, "topmoney-total", EconomyAPI.MONEY_FORMAT.format(total))).append("\n\n");
                }
                int duplicate = 0;
                double prev = -1D;
                for (int n = 0; n < list.size(); n++) {
                    int current = (int) Math.ceil((double) (n + 1) / 5);
                    if (page == current) {
                    	double m = money.get(list.get(n));
                    	if (m == prev) duplicate++;
                    	else duplicate = 0;
                    	prev = m;
                        output.append(EconomyAPI.getI18n().tr(langCode, "topmoney-format", Integer.toString(n + 1 - duplicate), getName(list.get(n)), EconomyAPI.MONEY_FORMAT.format(m))).append("\n");
                    } else if (page < current) {
                        break;
                    }
                }

                sender.sendMessage(output.substring(0, output.length() - 1));
            }, true);
        } catch (NumberFormatException e) {

            sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "topmoney-invalid-page-number"));
        }
        return true;
    }
}
