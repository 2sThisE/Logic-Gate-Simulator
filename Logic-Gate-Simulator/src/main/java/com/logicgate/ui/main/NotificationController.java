package com.logicgate.ui.main;

import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

public class NotificationController {
    public enum Type {
        INFO,
        WARNING,
        ERROR
    }

    private static final int MAX_VISIBLE_NOTIFICATIONS = 5;
    private static final Duration AUTO_DISMISS_DELAY = Duration.seconds(3);
    private static final Duration FADE_DURATION = Duration.millis(140);

    private final VBox notificationPane;
    private final Map<VBox, PauseTransition> dismissTimers = new IdentityHashMap<>();
    private final Set<VBox> expandedCards = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    public NotificationController(VBox notificationPane) {
        this.notificationPane = notificationPane;
        if (notificationPane != null) {
            notificationPane.setPickOnBounds(false);
            notificationPane.setFillWidth(false);
        }
    }

    public void show(Type type, String title, String body) {
        if (notificationPane == null) return;

        Platform.runLater(() -> {
            VBox card = createCard(type, title, body);
            card.setOpacity(0);
            notificationPane.getChildren().add(0, card);
            startAutoDismiss(card);
            playFadeIn(card);

            while (notificationPane.getChildren().size() > MAX_VISIBLE_NOTIFICATIONS) {
                removeCard((VBox) notificationPane.getChildren().get(notificationPane.getChildren().size() - 1));
            }

            notificationPane.setVisible(true);
            notificationPane.setManaged(true);
            refreshTimerStates();
        });
    }

    private VBox createCard(Type type, String title, String body) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("notification-card", "notification-" + type.name().toLowerCase(Locale.ROOT));
        boolean hasTitle = title != null && !title.isBlank();
        boolean hasBody = body != null && !body.isBlank();
        boolean expandable = hasTitle && hasBody;

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon typeIcon = new FontIcon(getIconLiteral(type));
        typeIcon.setIconSize(16);
        typeIcon.getStyleClass().add("notification-icon");

        Label typeLabel = new Label(null, typeIcon);
        typeLabel.getStyleClass().add("notification-type");

        Label titleLabel = new Label(hasTitle ? title : (hasBody ? body : getTypeLabel(type)));
        titleLabel.getStyleClass().add("notification-title");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button closeButton = new Button("x");
        closeButton.getStyleClass().add("notification-close");
        closeButton.setFocusTraversable(false);
        closeButton.setOnMouseClicked(e -> e.consume());
        closeButton.setOnAction(e -> removeCard(card));

        header.getChildren().addAll(typeLabel, titleLabel, closeButton);

        Label bodyLabel = new Label(hasBody ? body : "");
        bodyLabel.getStyleClass().add("notification-body");
        bodyLabel.setWrapText(true);
        bodyLabel.setVisible(false);
        bodyLabel.setManaged(false);

        card.getChildren().addAll(header, bodyLabel);
        card.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                removeCard(card);
                event.consume();
                return;
            }
            if (event.getTarget() == closeButton) return;
            if (!expandable) return;
            boolean expanded = bodyLabel.isVisible();
            bodyLabel.setVisible(!expanded);
            bodyLabel.setManaged(!expanded);
            setExpanded(card, !expanded);
            refreshTimerStates();
        });

        return card;
    }

    private void startAutoDismiss(VBox card) {
        PauseTransition timer = new PauseTransition(AUTO_DISMISS_DELAY);
        timer.setOnFinished(e -> removeCard(card));
        dismissTimers.put(card, timer);
        timer.play();
    }

    private void setExpanded(VBox card, boolean expanded) {
        if (expanded) {
            expandedCards.add(card);
            if (!card.getStyleClass().contains("notification-expanded")) {
                card.getStyleClass().add("notification-expanded");
            }
        } else {
            expandedCards.remove(card);
            card.getStyleClass().remove("notification-expanded");
        }
    }

    private void removeCard(VBox card) {
        PauseTransition timer = dismissTimers.remove(card);
        if (timer != null) {
            timer.stop();
        }
        expandedCards.remove(card);
        playFadeOutAndRemove(card);
    }

    private void playFadeIn(VBox card) {
        FadeTransition fade = new FadeTransition(FADE_DURATION, card);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void playFadeOutAndRemove(VBox card) {
        if (!notificationPane.getChildren().contains(card)) {
            return;
        }
        FadeTransition fade = new FadeTransition(FADE_DURATION, card);
        fade.setFromValue(card.getOpacity());
        fade.setToValue(0);
        fade.setOnFinished(e -> removeCardImmediately(card));
        fade.play();
    }

    private void removeCardImmediately(VBox card) {
        notificationPane.getChildren().remove(card);
        if (notificationPane.getChildren().isEmpty()) {
            notificationPane.setVisible(false);
            notificationPane.setManaged(false);
        }
        refreshTimerStates();
    }

    private void refreshTimerStates() {
        int firstExpandedIndex = -1;
        for (int i = 0; i < notificationPane.getChildren().size(); i++) {
            if (expandedCards.contains(notificationPane.getChildren().get(i))) {
                firstExpandedIndex = i;
                break;
            }
        }

        for (int i = 0; i < notificationPane.getChildren().size(); i++) {
            VBox card = (VBox) notificationPane.getChildren().get(i);
            PauseTransition timer = dismissTimers.get(card);
            if (timer == null) continue;

            if (firstExpandedIndex >= 0 && i >= firstExpandedIndex) {
                timer.pause();
            } else {
                timer.play();
            }
        }
    }

    private String getTypeLabel(Type type) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("com.logicgate.ui.strings", Locale.getDefault());
            return switch (type) {
                case INFO -> bundle.getString("notification.type.info");
                case WARNING -> bundle.getString("notification.type.warning");
                case ERROR -> bundle.getString("notification.type.error");
            };
        } catch (Exception e) {
            return switch (type) {
                case INFO -> "Info";
                case WARNING -> "Warning";
                case ERROR -> "Error";
            };
        }
    }

    private String getIconLiteral(Type type) {
        return switch (type) {
            case INFO -> "mdi2i-information-outline";
            case WARNING -> "mdi2a-alert-outline";
            case ERROR -> "mdi2a-alert-circle-outline";
        };
    }
}
