package com.christian34.catchthebeacon.commands.arguments;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.game.Game;
import com.christian34.catchthebeacon.lib.lang.LangText;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Command argument for running games (by their id, as in /ctb game list), suggests all ids.
 *
 * @author Christian34
 */
public final class GameArgument {

    private GameArgument() {
    }

    public static final class GameParser<C> implements ArgumentParser<C, Game>, BlockingSuggestionProvider.Strings<C> {

        @Override
        public @NotNull ArgumentParseResult<Game> parse(@NotNull CommandContext<C> commandContext, @NotNull CommandInput commandInput) {
            final String input = commandInput.peekString();
            Game game = CatchTheBeacon.getInstance().getGameManager().getGame(input);
            if (game == null) {
                return ArgumentParseResult.failure(new MessageException(LangText.GAME_NOT_FOUND, input));
            }
            commandInput.readString();
            return ArgumentParseResult.success(game);
        }

        @Override
        public @NotNull Iterable<String> stringSuggestions(@NotNull CommandContext<C> commandContext, @NotNull CommandInput input) {
            List<String> ids = new ArrayList<>();
            for (Game game : new ArrayList<>(CatchTheBeacon.getInstance().getGameManager().getGames())) {
                ids.add(game.getUniqueId());
            }
            return ids;
        }

    }

}
