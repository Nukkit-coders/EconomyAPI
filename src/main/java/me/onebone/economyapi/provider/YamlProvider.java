package me.onebone.economyapi.provider;

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

import cn.nukkit.utils.Config;

import java.io.File;
import java.util.LinkedHashMap;

public class YamlProvider implements Provider {
    private Config file;
    private LinkedHashMap<String, Double> data;
    private boolean needSave;

    @Override
    public void init(File path) {
        file = new Config(new File(path, "Money.yml"), Config.YAML);
        file.set("version", 2);

        //noinspection unchecked
        LinkedHashMap<String, Object> temp = (LinkedHashMap) file.getRootSection()
                .computeIfAbsent("money", s -> new LinkedHashMap<>());

        data = new LinkedHashMap<>();
        temp.forEach((username, money) -> {
            if (money instanceof Integer) {
                data.put(username, ((Integer) money).doubleValue());
            } else if (money instanceof Double) {
                data.put(username, (Double) money);
            } else if (money instanceof String) {
                data.put(username, Double.parseDouble(money.toString()));
            }
        });
    }

    @Override
    public void open() {
        // nothing to do
    }

    @Override
    public void save() {
        if (needSave) {
            needSave = false;
            file.set("money", data);
            file.save();
        }
    }

    @Override
    public void close() {
        this.save();

        file = null;
        data = null;
    }

    @Override
    public boolean accountExists(String id) {
        return data.containsKey(id);
    }

    @Override
    public boolean removeAccount(String id) {
        if (accountExists(id)) {
            needSave = true;
            data.remove(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean createAccount(String id, double defaultMoney) {
        if (!this.accountExists(id)) {
            needSave = true;
            data.put(id, defaultMoney);
        }
        return false;
    }

    @Override
    public boolean setMoney(String id, double amount) {
        if (data.containsKey(id)) {
            needSave = true;
            data.put(id, amount);
            return true;
        }
        return false;
    }


    @Override
    public boolean addMoney(String id, double amount) {
        if (data.containsKey(id)) {
            needSave = true;
            data.put(id, data.get(id) + amount);
            return true;
        }
        return false;
    }

    @Override
    public boolean reduceMoney(String id, double amount) {
        if (data.containsKey(id)) {
            needSave = true;
            data.put(id, data.get(id) - amount);
            return true;
        }
        return false;
    }

    @Override
    public double getMoney(String id) {
        if (data.containsKey(id)) {
            return data.get(id);
        }
        return -1;
    }

    public LinkedHashMap<String, Double> getAll() {
        return data;
    }

    public String getName() {
        return "Yaml";
    }
}
