package io.github.railgun19457.astrbotadapter.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AstrbotTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("reload", "status", "help");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Respect permission: if no permission, no suggestions
        if (!sender.hasPermission("astrbot.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            StringUtil.copyPartialMatches(args[0], SUBCOMMANDS, completions);
            Collections.sort(completions);
            return completions;
        }

        // No further arguments currently
        return Collections.emptyList();
    }
}
