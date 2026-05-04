package com.logicgate.ui.main;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;

public class ConsoleLogController {
    private final ListView<String> consoleListView;
    private final Button errorButton;
    private final ObservableList<String> consoleMessages = FXCollections.observableArrayList();
    private int errorCount = 0;

    public ConsoleLogController(ListView<String> consoleListView, Button errorButton) {
        this.consoleListView = consoleListView;
        this.errorButton = errorButton;
    }

    public void setup() {
        if (consoleListView == null) return;
        consoleListView.setItems(consoleMessages);
        redirectSystemErr();
        setupContextMenu();
    }

    public void toggleConsole() {
        if (consoleListView == null) return;
        boolean isVisible = consoleListView.isVisible();
        consoleListView.setVisible(!isVisible);
        consoleListView.setManaged(!isVisible);
    }

    private void setupContextMenu() {
        if (errorButton == null) return;

        javafx.scene.control.ContextMenu logMenu = new javafx.scene.control.ContextMenu();
        javafx.scene.control.MenuItem clearItem = new javafx.scene.control.MenuItem("로그 지우기");
        clearItem.setOnAction(e -> clearLogs());
        logMenu.getItems().add(clearItem);

        errorButton.setContextMenu(logMenu);
    }

    private void clearLogs() {
        consoleMessages.clear();
        errorCount = 0;
        if (errorButton != null) {
            errorButton.setText("0 로그");
            errorButton.getStyleClass().remove("status-btn-error");
        }
    }

    private void redirectSystemErr() {
        PrintStream originalErr = System.err;

        OutputStream capturingErrStream = new OutputStream() {
            private final java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();

            @Override
            public void write(int b) {
                originalErr.write(b);
                if (b == '\n') {
                    String msg = buffer.toString(StandardCharsets.UTF_8);
                    buffer.reset();
                    Platform.runLater(() -> addLog("[ERROR] " + msg, true));
                } else if (b != '\r') {
                    buffer.write(b);
                }
            }
        };

        System.setErr(new PrintStream(capturingErrStream, true, StandardCharsets.UTF_8));

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Platform.runLater(() -> addLog("[FATAL] Uncaught Exception: " + e.toString(), true));
            e.printStackTrace(originalErr);
        });
    }

    private void addLog(String message, boolean isError) {
        consoleMessages.add(message);
        if (isError) {
            errorCount++;
            if (errorButton != null) {
                errorButton.setText(errorCount + " 오류/경고");
                if (!errorButton.getStyleClass().contains("status-btn-error")) {
                    errorButton.getStyleClass().add("status-btn-error");
                }
            }
        } else if (errorCount == 0 && errorButton != null) {
            errorButton.setText(consoleMessages.size() + " 로그");
        }

        if (consoleListView != null) {
            consoleListView.scrollTo(consoleMessages.size() - 1);
        }
    }
}
