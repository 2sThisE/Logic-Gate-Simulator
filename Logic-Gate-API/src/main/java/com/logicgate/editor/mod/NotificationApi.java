package com.logicgate.editor.mod;

/**
 * Mods can use this API to show editor notifications without depending on UI classes.
 */
public final class NotificationApi {
    public enum Type {
        INFO,
        WARNING,
        ERROR
    }

    @FunctionalInterface
    public interface Handler {
        void show(Type type, String title, String body);
    }

    private static Handler handler;

    private NotificationApi() {
    }

    public static void setHandler(Handler newHandler) {
        handler = newHandler;
    }

    public static void show(Type type, String title, String body) {
        Handler currentHandler = handler;
        if (currentHandler != null) {
            currentHandler.show(type, title, body);
        }
    }

    public static void info(String title, String body) {
        show(Type.INFO, title, body);
    }

    public static void warning(String title, String body) {
        show(Type.WARNING, title, body);
    }

    public static void error(String title, String body) {
        show(Type.ERROR, title, body);
    }
}
