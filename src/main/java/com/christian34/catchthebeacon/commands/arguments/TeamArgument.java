package com.christian34.catchthebeacon.commands.arguments;

import com.christian34.catchthebeacon.game.Team;
import com.christian34.catchthebeacon.lib.lang.LangText;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Command argument for the playable teams (red, blue - not random).
 *
 * @author Christian34
 */
public final class TeamArgument {

    private TeamArgument() {
    }

    public static final class TeamParser<C> implements ArgumentParser<C, Team>, BlockingSuggestionProvider.Strings<C> {

        @Override
        public @NotNull ArgumentParseResult<Team> parse(@NotNull CommandContext<C> commandContext, @NotNull CommandInput commandInput) {
            final String input = commandInput.peekString();
            Team team = Team.get(input);
            if (team == null) {
                return ArgumentParseResult.failure(new MessageException(LangText.INVALID_TEAM));
            }
            commandInput.readString();
            return ArgumentParseResult.success(team);
        }

        @Override
        public @NotNull Iterable<String> stringSuggestions(@NotNull CommandContext<C> commandContext, @NotNull CommandInput input) {
            return List.of("red", "blue");
        }

    }

}
