package com.logicgate.ui;

import com.logicgate.Circuit;
import com.logicgate.editor.interaction.KeyboardInteractionHandler;
import com.logicgate.editor.interaction.MouseInteractionHandler;
import com.logicgate.editor.interaction.WiringManager;
import com.logicgate.editor.io.ProjectManager;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.rendering.CanvasRenderer;
import com.logicgate.editor.state.EditorContext;
import com.logicgate.gates.*;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;

public class MainController {

    @FXML
    private Pane canvasPane;
    @FXML
    private Canvas simulationCanvas;

    private Circuit circuit;
    private EditorContext context;
    private ProjectManager projectManager;
    private WiringManager wiringManager;
    private CanvasRenderer renderer;
    private MouseInteractionHandler mouseHandler;
    private KeyboardInteractionHandler keyboardHandler;
    private AnimationTimer timer;

    @FXML
    public void initialize() {
        circuit = new Circuit();
        circuit.setTickDelayMs(16);

        context = new EditorContext(circuit);
        wiringManager = new WiringManager(context);
        projectManager = new ProjectManager(context);
        mouseHandler = new MouseInteractionHandler(context, wiringManager);
        keyboardHandler = new KeyboardInteractionHandler(context);
        renderer = new CanvasRenderer(simulationCanvas, context, wiringManager);

        simulationCanvas.widthProperty().bind(canvasPane.widthProperty());
        simulationCanvas.heightProperty().bind(canvasPane.heightProperty());

        simulationCanvas.setFocusTraversable(true);
        simulationCanvas.setOnMouseEntered(e -> simulationCanvas.requestFocus());

        simulationCanvas.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) context.activeKeys.clear();
        });
        simulationCanvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obs2, oldWin, newWin) -> {
                    if (newWin != null) {
                        newWin.focusedProperty().addListener((obs3, oldF, newF) -> {
                            if (!newF) context.activeKeys.clear();
                        });
                    }
                });
            }
        });

        simulationCanvas.setOnMousePressed(mouseHandler::handleMousePressed);
        simulationCanvas.setOnMouseDragged(mouseHandler::handleMouseDragged);
        simulationCanvas.setOnMouseReleased(mouseHandler::handleMouseReleased);
        simulationCanvas.setOnScroll(mouseHandler::handleMouseScrolled);
        simulationCanvas.setOnMouseMoved(mouseHandler::handleMouseMoved);

        simulationCanvas.setOnKeyPressed(keyboardHandler::handleKeyPressed);
        simulationCanvas.setOnKeyReleased(keyboardHandler::handleKeyReleased);
        
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                keyboardHandler.updateCamera();
                renderer.draw();
            }
        };
        timer.start();

        circuit.startSimulation();
    }

    @FXML
    public void addSwitch() { spawnNode(new InputPin(), "SW"); }
    @FXML
    public void addLed() { spawnNode(new OutputPin(), "LED"); }
    @FXML
    public void addJoint() { spawnNode(new Joint(), "JNT"); }
    @FXML
    public void addAndGate() { spawnNode(new And(), "AND"); }
    @FXML
    public void addOrGate() { spawnNode(new Or(), "OR"); }
    @FXML
    public void addNotGate() { spawnNode(new Not(), "NOT"); }
    @FXML
    public void addXorGate() { spawnNode(new Xor(), "XOR"); }
    @FXML
    public void addNorGate() { spawnNode(new Nor(), "NOR"); }
    @FXML
    public void addNandGate() { spawnNode(new Nand(), "NAND"); }
    @FXML
    public void addXnorGate() { spawnNode(new Xnor(), "XNOR"); }

    @FXML
    public void saveProject() {
        projectManager.saveProject(simulationCanvas.getScene().getWindow());
    }

    @FXML
    public void loadProject() {
        projectManager.loadProject(simulationCanvas.getScene().getWindow());
    }

    private void spawnNode(Node logicNode, String label) {
        circuit.addNode(logicNode);
        
        double spawnX = ((simulationCanvas.getWidth() / 2) - context.cameraX) / context.zoom;
        double spawnY = ((simulationCanvas.getHeight() / 2) - context.cameraY) / context.zoom;
        
        double nodeWidth = 80;
        double nodeHeight = 50;
        if (logicNode instanceof InputPin || logicNode instanceof OutputPin) {
            nodeWidth = 50;
        } else if (logicNode instanceof Joint) {
            nodeWidth = 30;
            nodeHeight = 30;
        }
        
        VisualNode newNode = new VisualNode(logicNode, spawnX - (nodeWidth / 2), spawnY - (nodeHeight / 2), label);
        context.visualNodes.add(newNode);
        
        context.selectedNode = newNode;
        context.selectedWire = null;
    }

    public void shutdown() {
        if (timer != null) timer.stop();
        if (circuit != null) circuit.stopSimulation();
    }
}