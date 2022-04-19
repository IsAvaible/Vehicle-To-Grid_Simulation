package Services;

/**
 * Interface to output text in a traceable and unified format.
 * Classes should implement a overload that only requires out and a log_level.
 */
public interface Out{

    /** Wraps a output string with an identifier that also provides the log level of the call.
     * @param object_name The name of the object | e.g Simon's car or V2G Unit
     * @param id The unique id of the object | e.g. 53E56203
     * @param out The string that should be outputted.
     * @param log_level The log level of the object.
     * @param message_log_level The log level of the message.
     */
    static void print(String object_name, String id, String out, Log_Level log_level, Log_Level message_log_level) {
        String identifier_string = String.format("[%s (ID: %s) ~ %s]: ", object_name.toUpperCase(), id.toUpperCase(), message_log_level.toString());
        String copy_of_id_string = identifier_string; // Used to check if the identifier_string was changed

        switch (log_level) {
            // Using the drop-through behavior of a switch to cover all lower cases if a higher log level is set.
            case ALL:
                if (message_log_level == Log_Level.ALL) identifier_string += out;
            case INFO:
                if (message_log_level == Log_Level.INFO) identifier_string += out;
            case WARN:
                if (message_log_level == Log_Level.WARN) identifier_string += out;
            case ERROR:
                if (message_log_level == Log_Level.ERROR) identifier_string += out;

                // If the string was changed (log level was sufficient), output the string.
                if (!identifier_string.equals(copy_of_id_string)) System.out.println(identifier_string);
            case NONE:
                break;
        }
    }

    /**
     * Enum that stores different log levels that can be used to receive less or more notifications.
     */
    enum Log_Level{
        NONE, ERROR, WARN, INFO, ALL
    }

}
