package com.christian34.catchthebeacon.listeners;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.game.Team;
import com.christian34.catchthebeacon.game.Variant;
import com.christian34.catchthebeacon.lib.InteractionItems;
import com.christian34.catchthebeacon.lib.Menu;
import com.christian34.catchthebeacon.lib.lang.LangText;
import com.christian34.catchthebeacon.user.GamePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.christian34.catchthebeacon.lib.lang.I.i18n;

public class LobbyInteractionListener implements Listener {
    private final CatchTheBeacon instance;

    public LobbyInteractionListener(CatchTheBeacon instance) {
        this.instance = instance;
    }

    @EventHandler
    public void interactionInLobby(PlayerInteractEvent e) {
        if (!e.getPlayer().getWorld().getName().startsWith("ctb_")) {
            return;
        }
        GamePlayer gamePlayer = instance.getUser(e.getPlayer());
        Game game = gamePlayer.getGame();
        if (game == null /*|| game.getGameState() != GameState.LOBBY */ ||
                !(e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK)))
            return;

        ItemStack item = e.getItem();
        if (item == null) return;
        if (item.equals(InteractionItems.getSelectTeamItem())) {
            openTeamMenu(gamePlayer, game);
        } else if (item.isSimilar(InteractionItems.getVoteItem())) {
            e.setCancelled(true);
            openVoteMenu(gamePlayer, game);
        }
    }

    /**
     * red, random and blue with their players - clicking one joins the team
     */
    private void openTeamMenu(GamePlayer gamePlayer, Game game) {
        Menu menu = new Menu(i18n(LangText.GUI_TITEL_SELECT_TEAM));
        for (Team team : List.of(Team.RED, Team.RANDOM, Team.BLUE)) {
            Component size = teamSize(game, team);
            menu.button(getTeamItem(gamePlayer, team), team.getDisplayName(), size, getLore(game, team, size), player -> {
                if (gamePlayer.setTeam(team)) {
                    gamePlayer.sendMessage(i18n(LangText.JOINED_TEAM, team.getDisplayName()));
                } else {
                    gamePlayer.sendMessage(i18n(LangText.TEAM_IS_FULL));
                }
            });
        }
        menu.show(gamePlayer.getPlayer());
    }

    /**
     * the variants of variants.list with their votes - clicking one votes for it
     */
    private void openVoteMenu(GamePlayer gamePlayer, Game game) {
        List<Variant> variants = Variant.getVotable();
        if (variants.isEmpty()) return;
        Menu menu = new Menu(i18n(LangText.GUI_VOTE));
        for (Variant variant : variants) {
            ItemStack icon = new ItemStack(variant.getIcon());
            if (variant == game.getVote(gamePlayer)) {
                ItemMeta meta = icon.getItemMeta();
                meta.setEnchantmentGlintOverride(true);
                icon.setItemMeta(meta);
            }
            Component votes = i18n(LangText.VARIANT_VOTES, game.getVotes(variant));
            menu.button(icon, variant.getName().colorIfAbsent(NamedTextColor.YELLOW), votes,
                    List.of(variant.getDescription().colorIfAbsent(NamedTextColor.GRAY), votes), player -> {
                        game.vote(gamePlayer, variant);
                        gamePlayer.sendMessage(i18n(LangText.VOTED, variant.getName()));
                    });
        }
        menu.show(gamePlayer.getPlayer());
    }

    /**
     * @return "players/max", null for random
     */
    @Nullable
    private Component teamSize(Game game, Team team) {
        if (team.equals(Team.RANDOM)) return null;
        return Component.text(game.getGamePlayers(team).size() + "/" + game.getArena().getMaxPlayers() / 2, NamedTextColor.GRAY);
    }

    private List<Component> getLore(Game game, Team team, @Nullable Component size) {
        List<Component> lore = new ArrayList<>();
        if (size != null) lore.add(size);
        lore.add(Component.text("---------------------", NamedTextColor.WHITE));
        if (!team.equals(Team.RANDOM)) {
            List<GamePlayer> players = new ArrayList<>(game.getGamePlayers(team));
            for (int i = 0; i < players.size(); i++) {
                lore.add(Component.text("#" + (i + 1) + " ", team.getColor())
                        .append(players.get(i).getPlayer().displayName().colorIfAbsent(team.getColor())));
            }
        }
        return lore;
    }

    private ItemStack getTeamItem(GamePlayer gamePlayer, Team team) {
        ItemStack item = team.getItem();
        if (gamePlayer.getTeam() != null && gamePlayer.getTeam().equals(team)) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setEnchantmentGlintOverride(true);
                item.setItemMeta(meta);
            }
        }
        return item;
    }

}
