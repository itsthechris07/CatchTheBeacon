package com.christian34.catchthebeacon.lib;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.files.ConfigFile;
import com.christian34.catchthebeacon.integrations.TabSupport;
import com.christian34.catchthebeacon.lib.lang.I;
import com.christian34.catchthebeacon.user.GamePlayer;
import net.kyori.adventure.text.Component;

public class TabListManager {
    private final Component header;
    private final Component footer;
    private TabSupport tabSupport;

    public TabListManager(CatchTheBeacon instance) {
        ConfigFile config = instance.getFileManager().getConfigFile();
        this.header = I.lines(config.getList("tablist.header"));
        this.footer = I.lines(config.getList("tablist.footer"));
        this.tabSupport = TabSupport.create(instance);
    }

    /**
     * replaces the hook into TAB (the tests use a fake one)
     */
    public void setTabSupport(TabSupport tabSupport) {
        this.tabSupport = tabSupport;
    }

    public void update(GamePlayer gamePlayer) {
        boolean inGame = gamePlayer.getGame() != null;
        gamePlayer.getPlayer().sendPlayerListHeaderAndFooter(inGame ? header : Component.empty(),
                inGame ? footer : Component.empty());
        // TAB would overwrite the tab list and name tags of the game
        if (inGame) {
            tabSupport.takeOver(gamePlayer.getPlayer(), header, footer);
        } else {
            tabSupport.release(gamePlayer.getPlayer());
        }
    }

}
