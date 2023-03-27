package org.jetlinks.core.command;

public class CommandUtils {

    @SuppressWarnings("all")
    public static String getCommandIdByType(Class<? extends Command> commandType) {
        String id = commandType.getSimpleName();
        if (id.endsWith("Command")) {
            id = id.substring(0, id.length() - 7);
        }
        return id;
    }

}
