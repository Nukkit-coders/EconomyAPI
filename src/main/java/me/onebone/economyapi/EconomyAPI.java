package me.onebone.economyapi;

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
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.lang.LangCode;
import cn.nukkit.lang.PluginI18n;
import cn.nukkit.lang.PluginI18nManager;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.utils.Utils;
import me.onebone.economyapi.command.*;
import me.onebone.economyapi.event.account.CreateAccountEvent;
import me.onebone.economyapi.event.money.AddMoneyEvent;
import me.onebone.economyapi.event.money.ReduceMoneyEvent;
import me.onebone.economyapi.event.money.SetMoneyEvent;
import me.onebone.economyapi.provider.Provider;
import me.onebone.economyapi.provider.YamlProvider;
import me.onebone.economyapi.task.AutoSaveTask;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;

public class EconomyAPI extends PluginBase implements Listener {
    public static final int RET_NO_ACCOUNT = -3;
    public static final int RET_CANCELLED = -2;
    public static final int RET_NOT_FOUND = -1;
    public static final int RET_INVALID = 0;
    public static final int RET_SUCCESS = 1;
    public static final DecimalFormat MONEY_FORMAT = new DecimalFormat();
    private static EconomyAPI instance;
    private static PluginI18n i18n;
    public static LangCode serverLangCode;
    private Provider provider;
    private final HashMap<String, Class<?>> providerClass = new HashMap<>();

    static {
        MONEY_FORMAT.setMaximumFractionDigits(2);
    }

    private final String[] langList = new String[]{
            "ch", "cs", "def", "fr", "id", "it", "jp", "ko", "nl", "ru", "zh"
    };

    public static EconomyAPI getInstance() {
        return instance;
    }

    public static PluginI18n getI18n() {
        return i18n;
    }

    public boolean createAccount(Player player) {
        return this.createAccount(player, -1);
    }

    public boolean createAccount(Player player, double defaultMoney) {
        return this.createAccount(player, defaultMoney, false);
    }

    public boolean createAccount(Player player, double defaultMoney, boolean force) {
        return this.createAccount(player.getUniqueId(), defaultMoney, force);
    }

    public boolean createAccount(IPlayer player) {
        return this.createAccount(player, -1);
    }

    public boolean createAccount(IPlayer player, double defaultMoney) {
        return this.createAccount(player, defaultMoney, false);
    }

    public boolean createAccount(IPlayer player, double defaultMoney, boolean force) {
        return this.createAccount(player.getUniqueId(), defaultMoney, force);
    }

    public boolean createAccount(UUID id, double defaultMoney) {
        return this.createAccount(id, defaultMoney, false);
    }

    public boolean createAccount(UUID id, double defaultMoney, boolean force) {
        checkAndConvertLegacy(id);
        return createAccountInternal(id.toString(), defaultMoney, force);
    }

    public boolean createAccount(String id, double defaultMoney, boolean force) {
        Optional<UUID> uuid = checkAndConvertLegacy(id);
        return uuid.map(uuid1 -> createAccount(uuid1, defaultMoney, force))
                .orElse(createAccountInternal(id, defaultMoney, force));
    }

    private boolean createAccountInternal(String id, double defaultMoney, boolean force) {
        CreateAccountEvent event = new CreateAccountEvent(id, defaultMoney);
        this.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled() || force) {
            defaultMoney = event.getDefaultMoney() == -1D ? this.getDefaultMoney() : event.getDefaultMoney();
            return this.provider.createAccount(id, defaultMoney);
        }
        return false;
    }

    public LinkedHashMap<String, Double> getAllMoney() {
        return this.provider.getAll();
    }

    /**
     * Returns money of player
     *
     * @param player
     * @return Money of player. -1 if player does not exist.
     */
    public double myMoney(Player player) {
        return this.myMoney(player.getUniqueId());
    }

    public double myMoney(IPlayer player) {
        return this.myMoney(player.getUniqueId());
    }

    public double myMoney(UUID id) {
        checkAndConvertLegacy(id);
        return myMoneyInternal(id.toString());
    }

    public double myMoney(String id) {
        Optional<UUID> uuid = checkAndConvertLegacy(id);
        return uuid.map(this::myMoney).orElse(myMoneyInternal(id));
    }

    private double myMoneyInternal(String id) {
        return this.provider.getMoney(id.toLowerCase());
    }

    public int setMoney(Player player, double amount) {
        return this.setMoney(player, amount, false);
    }

    public int setMoney(Player player, double amount, boolean force) {
        return this.setMoney(player.getUniqueId(), amount, force);
    }

    public int setMoney(IPlayer player, double amount) {
        return this.setMoney(player, amount, false);
    }

    public int setMoney(IPlayer player, double amount, boolean force) {
        return this.setMoney(player.getUniqueId(), amount, force);
    }

    public int setMoney(UUID id, double amount) {
        return setMoney(id, amount, false);
    }

    public int setMoney(UUID id, double amount, boolean force) {
        checkAndConvertLegacy(id);
        return setMoneyInternal(id.toString(), amount, force);
    }

    public int setMoney(String id, double amount) {
        return this.setMoney(id, amount, false);
    }

    public int setMoney(String id, double amount, boolean force) {
        Optional<UUID> uuid = checkAndConvertLegacy(id);
        return uuid.map(uuid1 -> setMoney(uuid1, amount, force))
                .orElse(setMoneyInternal(id, amount, force));
    }

    private int setMoneyInternal(String id, double amount, boolean force) {
        id = id.toLowerCase();
        if (amount < 0) {
            return RET_INVALID;
        }

        SetMoneyEvent event = new SetMoneyEvent(id, amount);
        this.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled() || force) {
            if (this.provider.accountExists(id)) {
                amount = event.getAmount();

                if (amount <= this.getMaxMoney()) {
                    this.provider.setMoney(id, amount);
                    return RET_SUCCESS;
                } else {
                    return RET_INVALID;
                }
            } else {
                return RET_NO_ACCOUNT;
            }
        }
        return RET_CANCELLED;
    }

    public int addMoney(Player player, double amount) {
        return this.addMoney(player, amount, false);
    }

    public int addMoney(Player player, double amount, boolean force) {
        return this.addMoney(player.getUniqueId(), amount, force);
    }

    public int addMoney(IPlayer player, double amount) {
        return this.addMoney(player, amount, false);
    }

    public int addMoney(IPlayer player, double amount, boolean force) {
        return this.addMoney(player.getUniqueId(), amount, force);
    }

    public int addMoney(UUID id, double amount) {
        return addMoney(id, amount, false);
    }

    public int addMoney(UUID id, double amount, boolean force) {
        checkAndConvertLegacy(id);
        return addMoneyInternal(id.toString(), amount, force);
    }

    public int addMoney(String id, double amount) {
        return this.addMoney(id, amount, false);
    }

    public int addMoney(String id, double amount, boolean force) {
        Optional<UUID> uuid = checkAndConvertLegacy(id);
        return uuid.map(uuid1 -> addMoney(uuid1, amount, force))
                .orElse(addMoneyInternal(id, amount, force));
    }

    private int addMoneyInternal(String id, double amount, boolean force) {
        id = id.toLowerCase();
        if (amount < 0) {
            return RET_INVALID;
        }
        AddMoneyEvent event = new AddMoneyEvent(id, amount);
        this.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled() || force) {
            double money;
            if ((money = this.provider.getMoney(id)) != -1) {
                if (money + amount > this.getMaxMoney()) {
                    return RET_INVALID;
                } else {
                    this.provider.addMoney(id, amount);
                    return RET_SUCCESS;
                }
            } else {
                return RET_NO_ACCOUNT;
            }
        }
        return RET_CANCELLED;
    }

    public int reduceMoney(Player player, double amount) {
        return this.reduceMoney(player, amount, false);
    }

    public int reduceMoney(Player player, double amount, boolean force) {
        return this.reduceMoney(player.getUniqueId(), amount, force);
    }

    public int reduceMoney(IPlayer player, double amount) {
        return this.reduceMoney(player, amount, false);
    }

    public int reduceMoney(IPlayer player, double amount, boolean force) {
        return this.reduceMoney(player.getUniqueId(), amount, force);
    }

    public int reduceMoney(UUID id, double amount) {
        return this.reduceMoney(id, amount, false);
    }

    public int reduceMoney(UUID id, double amount, boolean force) {
        checkAndConvertLegacy(id);
        return reduceMoneyInternal(id.toString(), amount, force);
    }

    public int reduceMoney(String id, double amount) {
        return this.reduceMoney(id, amount, false);
    }

    public int reduceMoney(String id, double amount, boolean force) {
        Optional<UUID> uuid = checkAndConvertLegacy(id);
        return uuid.map(uuid1 -> reduceMoney(uuid1, amount, force))
                .orElse(reduceMoneyInternal(id, amount, force));
    }

    private int reduceMoneyInternal(String id, double amount, boolean force) {
        id = id.toLowerCase();
        if (amount < 0) {
            return RET_INVALID;
        }

        ReduceMoneyEvent event = new ReduceMoneyEvent(id, amount);
        this.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled() || force) {
            amount = event.getAmount();

            double money;
            if ((money = this.provider.getMoney(id)) != -1) {
                if (money - amount < 0) {
                    return RET_INVALID;
                } else {
                    this.provider.reduceMoney(id, amount);
                    return RET_SUCCESS;
                }
            } else {
                return RET_NO_ACCOUNT;
            }
        }
        return RET_CANCELLED;
    }

    public boolean hasAccount(IPlayer player) {
        return hasAccount(player.getUniqueId());
    }

    public boolean hasAccount(UUID id) {
        return hasAccount(id.toString());
    }

    public boolean hasAccount(String id) {
        return provider.accountExists(checkAndConvertLegacy(id).map(UUID::toString).map(String::toLowerCase).orElse(id));
    }

    public String getMonetaryUnit() {
        return this.getConfig().get("money.monetary-unit", "$");
    }

    public double getDefaultMoney() {
        if (this.getConfig().isDouble("money.default")) {
            return this.getConfig().get("money.default", 1000D);
        } else if (this.getConfig().isLong("money.default")) {
            return this.getConfig().getLong("money.default", 1000);
        }
        return 1000;
    }

    public double getMaxMoney() {
        if (this.getConfig().isDouble("money.max")) {
            return this.getConfig().get("money.max", 9999999999D);
        } else if (this.getConfig().isLong("money.max")) {
            return this.getConfig().getLong("money.max", 9999999999L);
        }
        return 9999999999D;
    }

    public void saveAll() {
        if (this.provider != null) {
            this.provider.save();
        }
    }

    public void onLoad() {
        instance = this;
        // 注册插件的 i18n
        i18n = PluginI18nManager.register(this);
        initServerLangCode();

        this.addProvider("yaml", YamlProvider.class);
    }

    public void onEnable() {
        this.saveDefaultConfig();

        boolean success = this.initialize();

        if (success) {
            this.getServer().getPluginManager().registerEvents(this, this);
            this.getServer().getScheduler().scheduleDelayedRepeatingTask(new AutoSaveTask(this), this.getConfig().get("data.auto-save-interval", 10) * 1200, this.getConfig().get("data.auto-save-interval", 10) * 1200);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.createAccount(event.getPlayer());
    }

    public void onDisable() {
        this.saveAll();
    }

    private boolean initialize() {
        this.registerCommands();
        return this.selectProvider();
    }

    private void registerCommands() {
        this.getServer().getCommandMap().register("economy", new MyMoneyCommand(this));
        this.getServer().getCommandMap().register("economy", new TopMoneyCommand(this));
        this.getServer().getCommandMap().register("economy", new GiveMoneyCommand(this));
        this.getServer().getCommandMap().register("economy", new TakeMoneyCommand(this));
        this.getServer().getCommandMap().register("economy", new PayCommand(this));
        this.getServer().getCommandMap().register("economy", new SetMoneyCommand(this));
    }

    private boolean selectProvider() {
        Class<?> providerClass = this.providerClass.get((this.getConfig().get("data.provider", "yaml")).toLowerCase());

        if (providerClass == null) {
            this.getLogger().critical("Invalid data provider was given.");
            return false;
        }

        try {
            this.provider = (Provider) providerClass.newInstance();
            this.provider.init(this.getDataFolder());
        } catch (InstantiationException | IllegalAccessException e) {
            this.getLogger().critical("Invalid data provider was given.");
            return false;
        }

        this.provider.open();

        this.getLogger().info("Data provider was set to: " + provider.getName());
        return true;
    }

    public boolean addProvider(String name, Class<? extends Provider> providerClass) {
        this.providerClass.put(name, providerClass);
        return true;
    }


    private void checkAndConvertLegacy(UUID uuid) {
        IPlayer player = getServer().getOfflinePlayer(uuid);
        if (player != null && player.getName() != null) {
            checkAndConvertLegacy(uuid, player.getName());
        }
    }

    private Optional<UUID> checkAndConvertLegacy(String id) {
        Optional<UUID> uuid = getServer().lookupName(id);
        uuid.ifPresent(uuid1 -> checkAndConvertLegacy(uuid1, id));
        return uuid;
    }

    private void checkAndConvertLegacy(UUID uuid, String name) {
        name = name.toLowerCase();
        if (!provider.accountExists(name)) {
            return;
        }

        if (provider.accountExists(uuid.toString())) {
            provider.removeAccount(name);
            return;
        }

        double money = provider.getMoney(name);
        provider.createAccount(uuid.toString(), money);
        provider.removeAccount(name);
    }

    private void initServerLangCode() {
        switch (Server.getInstance().getLanguage().getLang()) {
            case "eng" -> {
                serverLangCode = LangCode.en_US;
            }
            case "chs" -> {
                serverLangCode = LangCode.zh_CN;
            }
            case "deu" -> {
                serverLangCode = LangCode.de_DE;
            }
            case "rus" -> {
                serverLangCode = LangCode.ru_RU;
            }
            default -> {
                try {
                    serverLangCode = LangCode.valueOf(Server.getInstance().getLanguage().getLang());
                } catch (IllegalArgumentException e) {
                    serverLangCode = LangCode.en_US;
                }
            }
        }
    }
}
