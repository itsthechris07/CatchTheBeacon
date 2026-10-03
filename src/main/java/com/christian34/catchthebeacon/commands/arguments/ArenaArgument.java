package com.christian34.catchthebeacon.commands.arguments;

import com.christian34.catchthebeacon.CatchTheBeacon;
import com.christian34.catchthebeacon.game.map.Arena;
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
 * Command argument for configured arenas, suggests all arena names.
 *
 * @author Christian34
 */
public final class ArenaArgument {

    private ArenaArgument() {
    }

    public static final class ArenaParser<C> implements ArgumentParser<C, Arena>, BlockingSuggestionProvider.Strings<C> {

        @Override
        public @NotNull ArgumentParseResult<Arena> parse(@NotNull CommandContext<C> commandContext, @NotNull CommandInput commandInput) {
            final String input = commandInput.peekString();
            Arena arena = CatchTheBeacon.getInstance().getMapHandler().getArena(input);
            if (arena == null) {
                return ArgumentParseResult.failure(new MessageException(LangText.ARENA_NOT_FOUND, input));
            }
            commandInput.readString();
            return ArgumentParseResult.success(arena);
        }

        @Override
        public @NotNull Iterable<String> stringSuggestions(@NotNull CommandContext<C> commandContext, @NotNull CommandInput input) {
            List<String> names = new ArrayList<>();
            synchronized (CatchTheBeacon.getInstance().getMapHandler().getGameMaps()) {
                for (Arena arena : CatchTheBeacon.getInstance().getMapHandler().getGameMaps()) {
                    names.add(arena.getName());
                }
            }
            return names;
        }

    }

}
