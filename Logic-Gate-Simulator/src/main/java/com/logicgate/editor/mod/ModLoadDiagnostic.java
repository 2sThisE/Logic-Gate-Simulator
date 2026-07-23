package com.logicgate.editor.mod;

public final class ModLoadDiagnostic {
    public enum Severity {
        WARNING,
        ERROR
    }

    public enum Stage {
        FILE_ACCESS,
        CLASS_LOADING,
        NODE_REGISTRATION,
        SYMBOL_REGISTRATION
    }

    public final Severity severity;
    public final Stage stage;
    public final String jarName;
    public final String className;
    public final String detail;

    public ModLoadDiagnostic(
        Severity severity,
        Stage stage,
        String jarName,
        String className,
        String detail
    ) {
        this.severity = severity;
        this.stage = stage;
        this.jarName = jarName;
        this.className = className;
        this.detail = detail;
    }

    public String toDisplayString() {
        StringBuilder text = new StringBuilder();
        text.append(jarName != null ? jarName : "<unknown jar>");
        text.append(" · ").append(stage);
        if (className != null && !className.isBlank()) {
            text.append(" · ").append(className);
        }
        if (detail != null && !detail.isBlank()) {
            text.append("\n").append(detail);
        }
        return text.toString();
    }

    public static String describe(Throwable error) {
        if (error == null) return "Unknown error";

        Throwable root = error;
        int depth = 0;
        while (root.getCause() != null && root.getCause() != root && depth++ < 10) {
            root = root.getCause();
        }

        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            return root.getClass().getSimpleName();
        }
        String normalized = message.replace('\r', ' ').replace('\n', ' ').trim();
        if (normalized.length() > 300) {
            normalized = normalized.substring(0, 297) + "...";
        }
        return root.getClass().getSimpleName() + ": " + normalized;
    }
}
