package com.logicgate.ui;

import com.logicgate.Circuit;
import com.logicgate.gates.*;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MainController {

    @FXML
    private Pane canvasPane;
    @FXML
    private Canvas simulationCanvas;

    private Circuit circuit;
    private List<VisualNode> visualNodes = new ArrayList<>();
    private List<VisualWire> visualWires = new ArrayList<>();
    private AnimationTimer timer;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // 무한 캔버스 카메라 좌표 (팬 이동) 및 줌
    private double cameraX = 0;
    private double cameraY = 0;
    private double zoom = 1.0;
    
    // 화면상의 마우스 좌표 및 논리(월드) 좌표
    private double screenMouseX, screenMouseY;
    private double worldMouseX, worldMouseY;

    // 호버 상태 변수
    private VisualNode hoveredNode = null;
    private int hoveredInPin = -1;
    private int hoveredOutPin = -1;
    
    // 선택(Selection) 상태 변수
    private VisualNode selectedNode = null;
    private VisualWire selectedWire = null;

    // 캔버스 팬(Pan) 상태 변수
    private boolean isPanning = false;
    private double panStartX, panStartY;

    // 드래그 이동 상태 변수
    private VisualNode draggingNode = null;
    private double dragOffsetX, dragOffsetY;

    // 선 긋기 상태 변수
    private boolean isWiring = false;
    private boolean isWiringFromOut = true;
    private VisualNode wiringNode = null;
    private int wiringPin = -1;

    // [추가] 붙여넣기(불러오기) 배치 상태 변수
    private boolean isPlacingImport = false;
    private ProjectData pendingProjectData = null;

    private final Set<KeyCode> activeKeys = new HashSet<>();

    @FXML
    public void initialize() {
        circuit = new Circuit();
        circuit.setTickDelayMs(16);

        simulationCanvas.widthProperty().bind(canvasPane.widthProperty());
        simulationCanvas.heightProperty().bind(canvasPane.heightProperty());

        // 키보드 이벤트를 받기 위한 포커스 설정
        simulationCanvas.setFocusTraversable(true);
        simulationCanvas.setOnMouseEntered(e -> simulationCanvas.requestFocus());

        // [수정] 캔버스 및 윈도우 전체의 포커스 유실을 감지하여 키 상태 초기화 (키 고임 현상 방지)
        simulationCanvas.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) activeKeys.clear();
        });
        simulationCanvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obs2, oldWin, newWin) -> {
                    if (newWin != null) {
                        newWin.focusedProperty().addListener((obs3, oldF, newF) -> {
                            if (!newF) activeKeys.clear();
                        });
                    }
                });
            }
        });

        simulationCanvas.setOnMousePressed(this::handleMousePressed);
        simulationCanvas.setOnMouseDragged(this::handleMouseDragged);
        simulationCanvas.setOnMouseReleased(this::handleMouseReleased);
        simulationCanvas.setOnScroll(this::handleMouseScrolled);
        simulationCanvas.setOnKeyPressed(this::handleKeyPressed);
        simulationCanvas.setOnKeyReleased(this::handleKeyReleased);
        
        simulationCanvas.setOnMouseMoved(e -> { 
            screenMouseX = e.getX(); 
            screenMouseY = e.getY();
            updateWorldCoordinates();
            updateHoverState();
        });

        GraphicsContext gc = simulationCanvas.getGraphicsContext2D();
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateCamera();
                draw(gc);
            }
        };
        timer.start();

        circuit.startSimulation();
    }

    private void updateCamera() {
        if (activeKeys.isEmpty()) return;

        double moveX = 0;
        double moveY = 0;

        // 방향키 및 WASD 지원
        if (activeKeys.contains(KeyCode.UP)    || activeKeys.contains(KeyCode.W)) moveY += 1;
        if (activeKeys.contains(KeyCode.DOWN)  || activeKeys.contains(KeyCode.S)) moveY -= 1;
        if (activeKeys.contains(KeyCode.LEFT)  || activeKeys.contains(KeyCode.A)) moveX += 1;
        if (activeKeys.contains(KeyCode.RIGHT) || activeKeys.contains(KeyCode.D)) moveX -= 1;

        if (moveX != 0 || moveY != 0) {
            // 대각선 이동 시 속도가 빨라지지 않도록 정규화
            double length = Math.hypot(moveX, moveY);
            double speed = 10.0 / zoom;
            
            cameraX += (moveX / length) * speed;
            cameraY += (moveY / length) * speed;
            
            updateWorldCoordinates();
            updateHoverState();
        }
    }

    private void updateWorldCoordinates() {
        worldMouseX = (screenMouseX - cameraX) / zoom;
        worldMouseY = (screenMouseY - cameraY) / zoom;
    }

    private void updateHoverState() {
        // 배치 모드 중에는 기존 노드 호버를 막음
        if (isPlacingImport) {
            hoveredNode = null;
            hoveredInPin = -1;
            hoveredOutPin = -1;
            return;
        }

        hoveredNode = null;
        hoveredInPin = -1;
        hoveredOutPin = -1;

        // 핀의 기본 반지름(4) + 여유값(4) = 8로 고정.
        final double pinDetectionRadius = 8.0;

        for (int i = visualNodes.size() - 1; i >= 0; i--) {
            VisualNode vn = visualNodes.get(i);
            double minDistance = Double.MAX_VALUE;
            boolean foundPin = false;

            // 출력 핀 검사
            for (int outIdx = 0; outIdx < vn.node.getOutputSize(); outIdx++) {
                double px = vn.getOutPinX(outIdx);
                double py = vn.getOutPinY(outIdx);
                double dist = Math.hypot(px - worldMouseX, py - worldMouseY);
                if (dist < pinDetectionRadius && dist <= minDistance) {
                    minDistance = dist;
                    hoveredNode = vn;
                    hoveredOutPin = outIdx;
                    foundPin = true;
                }
            }

            // 입력 핀 검사 (이미 찾은 출력 핀과 거리가 같아도(-1로 초기화하지 않고) 둘 다 저장)
            for (int inIdx = 0; inIdx < vn.node.getInputSize(); inIdx++) {
                double px = vn.getInPinX(inIdx);
                double py = vn.getInPinY(inIdx);
                double dist = Math.hypot(px - worldMouseX, py - worldMouseY);
                if (dist < pinDetectionRadius && dist <= minDistance) {
                    // 출력 핀보다 더 가까운 입력을 찾은 경우에만 출력 핀 선택을 취소
                    if (dist < minDistance) hoveredOutPin = -1;
                    
                    minDistance = dist;
                    hoveredNode = vn;
                    hoveredInPin = inIdx;
                    foundPin = true;
                }
            }

            if (foundPin) return;

            if (vn.contains(worldMouseX, worldMouseY)) {
                hoveredNode = vn;
                return;
            }
        }
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
    public void saveProject() {
        if (visualNodes.isEmpty()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("회로 저장 (상대 좌표)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("LogicGate Files", "*.json"));
        File file = fileChooser.showSaveDialog(simulationCanvas.getScene().getWindow());

        if (file != null) {
            try {
                // 기준점(가장 좌상단 좌표) 찾기
                double minX = Double.MAX_VALUE;
                double minY = Double.MAX_VALUE;
                for (VisualNode vn : visualNodes) {
                    minX = Math.min(minX, vn.x);
                    minY = Math.min(minY, vn.y);
                }

                ProjectData data = new ProjectData();
                for (VisualNode vn : visualNodes) {
                    // 상대 좌표로 저장 (x - minX, y - minY)
                    data.nodes.add(new NodeData(
                        vn.node.getClass().getSimpleName(),
                        vn.x - minX, vn.y - minY, vn.label
                    ));
                }
                for (VisualWire vw : visualWires) {
                    data.wires.add(new WireData(
                        visualNodes.indexOf(vw.from),
                        vw.outPin,
                        visualNodes.indexOf(vw.to),
                        vw.inPin
                    ));
                }
                String json = gson.toJson(data);
                Files.writeString(file.toPath(), json);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void loadProject() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("붙여넣을 회로 선택");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("LogicGate Files", "*.json"));
        File file = fileChooser.showOpenDialog(simulationCanvas.getScene().getWindow());

        if (file != null) {
            try {
                String json = Files.readString(file.toPath());
                pendingProjectData = gson.fromJson(json, ProjectData.class);
                isPlacingImport = true; // 배치 모드 활성화
                updateHoverState();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void finalizePlacement() {
        if (pendingProjectData == null) return;

        List<VisualNode> newNodes = new ArrayList<>();
        // 1. 노드 생성 및 배치
        for (NodeData nd : pendingProjectData.nodes) {
            Node logicNode = createNodeByType(nd.type);
            if (logicNode != null) {
                circuit.addNode(logicNode);
                // 현재 마우스 월드 좌표 + 저장된 상대 좌표
                VisualNode vn = new VisualNode(logicNode, worldMouseX + nd.x, worldMouseY + nd.y, nd.label);
                visualNodes.add(vn);
                newNodes.add(vn);
            }
        }

        // 2. 전선 연결
        for (WireData wd : pendingProjectData.wires) {
            if (wd.fromIdx >= 0 && wd.fromIdx < newNodes.size() &&
                wd.toIdx >= 0 && wd.toIdx < newNodes.size()) {
                connectWires(newNodes.get(wd.fromIdx), wd.outPin, newNodes.get(wd.toIdx), wd.inPin);
            }
        }

        // 배치 완료 후 모드 해제
        isPlacingImport = false;
        pendingProjectData = null;
        updateHoverState();
    }

    private Node createNodeByType(String type) {
        return switch (type) {
            case "InputPin" -> new InputPin();
            case "OutputPin" -> new OutputPin();
            case "Joint" -> new Joint();
            case "And" -> new And();
            case "Or" -> new Or();
            case "Not" -> new Not();
            case "Xor" -> new Xor();
            case "Nor" -> new Nor();
            case "Nand" -> new Nand();
            case "Xnor" -> new Xnor();
            default -> null;
        };
    }

    private void spawnNode(Node logicNode, String label) {
        circuit.addNode(logicNode);
        
        // 캔버스 크기, 카메라 위치, 줌 비율에 상관없이 항상 화면 정중앙에 스폰되도록 보정
        double spawnX = ((simulationCanvas.getWidth() / 2) - cameraX) / zoom;
        double spawnY = ((simulationCanvas.getHeight() / 2) - cameraY) / zoom;
        
        // 부품 크기(80x50, 50x50, 20x20 등)의 절반을 빼주어 중심점이 맞게 함
        double nodeWidth = 80;
        double nodeHeight = 50;
        if (logicNode instanceof InputPin || logicNode instanceof OutputPin) {
            nodeWidth = 50;
        } else if (logicNode instanceof Joint) {
            nodeWidth = 30;
            nodeHeight = 30;
        }
        
        VisualNode newNode = new VisualNode(logicNode, spawnX - (nodeWidth / 2), spawnY - (nodeHeight / 2), label);
        visualNodes.add(newNode);
        
        // 스폰된 노드를 즉시 선택 상태로 만듦
        selectedNode = newNode;
        selectedWire = null;
    }

    private void handleMouseScrolled(ScrollEvent event) {
        double zoomFactor = 1.1;
        double oldZoom = zoom;
        
        if (event.getDeltaY() > 0) {
            zoom *= zoomFactor; // 휠 위로 (확대)
        } else if (event.getDeltaY() < 0) {
            zoom /= zoomFactor; // 휠 아래로 (축소)
        }
        
        zoom = Math.max(0.1, Math.min(zoom, 5.0));

        // 마우스 커서가 가리키는 곳을 중심으로 확대/축소되도록 카메라 보정
        double f = (zoom / oldZoom) - 1;
        cameraX -= (event.getX() - cameraX) * f;
        cameraY -= (event.getY() - cameraY) * f;

        updateWorldCoordinates();
        updateHoverState();
    }

    private void handleKeyPressed(KeyEvent event) {
        activeKeys.add(event.getCode());

        switch (event.getCode()) {
            case ESCAPE:
                // 모든 드래그 및 선 긋기 상태 강제 취소
                isWiring = false;
                wiringNode = null;
                wiringPin = -1;
                draggingNode = null;
                isPanning = false;
                selectedNode = null;
                selectedWire = null;
                // 배치 모드 취소
                isPlacingImport = false;
                pendingProjectData = null;
                updateHoverState();
                break;
            case DELETE:
            case BACK_SPACE:
                if (selectedNode != null) {
                    removeNode(selectedNode);
                    selectedNode = null;
                } else if (selectedWire != null) {
                    circuit.disconnect(selectedWire.from.node, selectedWire.outPin);
                    visualWires.remove(selectedWire);
                    selectedWire = null;
                }
                updateHoverState();
                break;
            default: break;
        }
    }

    private void handleKeyReleased(KeyEvent event) {
        activeKeys.remove(event.getCode());
    }

    private void handleMousePressed(MouseEvent event) {
        screenMouseX = event.getX();
        screenMouseY = event.getY();
        updateWorldCoordinates();
        updateHoverState();

        if (event.getButton() == MouseButton.PRIMARY) {
            // [추가] 배치 모드일 때 클릭 시 최종 배치
            if (isPlacingImport) {
                finalizePlacement();
                return;
            }

            if (hoveredNode != null) {
                // 선 긋기 시작 (핀 클릭 시)
                if (hoveredOutPin != -1) {
                    startWiring(hoveredNode, hoveredOutPin, true);
                    return;
                } else if (hoveredInPin != -1) {
                    startWiring(hoveredNode, hoveredInPin, false);
                    return;
                } else {
                    // 본체 클릭 시
                    selectedNode = hoveredNode;
                    selectedWire = null; // 다른거 선택시 선 선택 해제
                    
                    if (hoveredNode.node instanceof InputPin) {
                        InputPin pin = (InputPin) hoveredNode.node;
                        pin.setState(pin.getOut() == 0);
                    }
                    draggingNode = hoveredNode;
                    dragOffsetX = worldMouseX - hoveredNode.x;
                    dragOffsetY = worldMouseY - hoveredNode.y;
                    return; 
                }
            }

            // 허공 클릭 처리: 전선 선택 확인
            for (VisualWire wire : visualWires) {
                double p1x = wire.from.getOutPinX(wire.outPin);
                double p1y = wire.from.getOutPinY(wire.outPin);
                double p2x = wire.to.getInPinX(wire.inPin);
                double p2y = wire.to.getInPinY(wire.inPin);
                
                if (distanceToSegment(worldMouseX, worldMouseY, p1x, p1y, p2x, p2y) < 10 / zoom) {
                    selectedWire = wire;
                    selectedNode = null; // 선 선택시 노드 선택 해제
                    return;
                }
            }

            // 아무것도 클릭되지 않았을 때 (허공 클릭 시) -> 선택 해제 및 패닝 시작
            selectedNode = null;
            selectedWire = null;
            
            isPanning = true;
            panStartX = screenMouseX - cameraX;
            panStartY = screenMouseY - cameraY;
        }
    }

    private void startWiring(VisualNode node, int pinIndex, boolean isFromOut) {
        isWiring = true;
        isWiringFromOut = isFromOut;
        wiringNode = node;
        wiringPin = pinIndex;

        Iterator<VisualWire> it = visualWires.iterator();
        while (it.hasNext()) {
            VisualWire w = it.next();
            if (isFromOut && w.from == node && w.outPin == pinIndex) {
                circuit.disconnect(w.from.node, w.outPin);
                if (w == selectedWire) selectedWire = null;
                it.remove();
                break; 
            } else if (!isFromOut && w.to == node && w.inPin == pinIndex) {
                circuit.disconnect(w.from.node, w.outPin);
                if (w == selectedWire) selectedWire = null;
                it.remove();
            }
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        screenMouseX = event.getX();
        screenMouseY = event.getY();
        updateWorldCoordinates();
        updateHoverState();

        if (draggingNode != null) {
            draggingNode.x = worldMouseX - dragOffsetX;
            draggingNode.y = worldMouseY - dragOffsetY;
        } else if (isPanning) {
            cameraX = screenMouseX - panStartX;
            cameraY = screenMouseY - panStartY;
            updateWorldCoordinates(); 
        }
    }

    private void handleMouseReleased(MouseEvent event) {
        screenMouseX = event.getX();
        screenMouseY = event.getY();
        updateWorldCoordinates();
        updateHoverState();

        if (isWiring && wiringNode != null) {
            boolean connectionMade = false;
            if (hoveredNode != null && hoveredNode != wiringNode) {
                if (isWiringFromOut && hoveredInPin != -1) {
                    if (isValidConnection(wiringNode, wiringPin, hoveredNode, hoveredInPin)) {
                        connectWires(wiringNode, wiringPin, hoveredNode, hoveredInPin);
                        connectionMade = true;
                    }
                } else if (!isWiringFromOut && hoveredOutPin != -1) {
                    if (isValidConnection(hoveredNode, hoveredOutPin, wiringNode, wiringPin)) {
                        connectWires(hoveredNode, hoveredOutPin, wiringNode, wiringPin);
                        connectionMade = true;
                    }
                }
            }

            // [추가] 허공에 선을 놓았을 때 자동으로 Joint 생성 및 연결
            // 마우스가 노드 본체 위(`hoveredNode != null`)에 있을 때는 조인트 생성을 방지
            if (!connectionMade && hoveredNode == null) {
                // 시작점으로부터 일정 거리 이상 드래그했을 때만 생성 (실수 방지)
                double startX = isWiringFromOut ? wiringNode.getOutPinX(wiringPin) : wiringNode.getInPinX(wiringPin);
                double startY = isWiringFromOut ? wiringNode.getOutPinY(wiringPin) : wiringNode.getInPinY(wiringPin);
                
                if (Math.hypot(startX - worldMouseX, startY - worldMouseY) > 20 / zoom) {
                    Joint joint = new Joint();
                    VisualNode jointVn = new VisualNode(joint, worldMouseX - 15, worldMouseY - 15, "JNT");
                    
                    // 새 Joint의 4개 핀 중 마우스와 가장 가까운 핀 선택
                    int bestPin = 0;
                    double minDist = Double.MAX_VALUE;
                    for (int i = 0; i < 4; i++) {
                        double d = Math.hypot(jointVn.getInPinX(i) - worldMouseX, jointVn.getInPinY(i) - worldMouseY);
                        if (d < minDist) {
                            minDist = d;
                            bestPin = i;
                        }
                    }

                    boolean autoConnectValid = false;
                    if (isWiringFromOut) {
                        autoConnectValid = isValidConnection(wiringNode, wiringPin, jointVn, bestPin);
                    } else {
                        autoConnectValid = isValidConnection(jointVn, bestPin, wiringNode, wiringPin);
                    }

                    if (autoConnectValid) {
                        circuit.addNode(joint);
                        visualNodes.add(jointVn);
                        if (isWiringFromOut) {
                            connectWires(wiringNode, wiringPin, jointVn, bestPin);
                        } else {
                            connectWires(jointVn, bestPin, wiringNode, wiringPin);
                        }
                    }
                }
            }
        }

        isWiring = false;
        wiringNode = null;
        wiringPin = -1;
        draggingNode = null;
        isPanning = false;
        updateHoverState();
    }

    private void connectWires(VisualNode fromNode, int outPin, VisualNode toNode, int inPin) {
        visualWires.removeIf(w -> {
            boolean removed = false;
            if (w.to == toNode && w.inPin == inPin) {
                circuit.disconnect(w.from.node, w.outPin);
                removed = true;
            }
            if (w.from == fromNode && w.outPin == outPin) {
                circuit.disconnect(w.from.node, w.outPin);
                removed = true;
            }
            if (removed && w == selectedWire) {
                selectedWire = null;
            }
            return removed;
        });

        circuit.connect(fromNode.node, outPin, toNode.node, inPin);
        VisualWire newWire = new VisualWire(fromNode, outPin, toNode, inPin);
        visualWires.add(newWire);
        
        selectedWire = newWire;
        selectedNode = null;
    }

    private boolean isValidConnection(VisualNode fromNode, int outPin, VisualNode toNode, int inPin) {
        if (fromNode == toNode) return false;

        // 조인트 제약 사항: 3개가 출력으로 나가면 남은 단자는 입력이 되어야 함 (최대 출력 3개)
        if (fromNode.node instanceof Joint) {
            int currentOuts = 0;
            boolean isReplacingSelf = false;
            for (VisualWire w : visualWires) {
                if (w.from == fromNode) {
                    if (w.outPin == outPin) isReplacingSelf = true;
                    currentOuts++;
                }
            }
            // 현재 핀을 교체하는 것이 아니라면, 추가 후 총 출력이 3개를 초과하면 안 됨
            if (!isReplacingSelf && currentOuts >= 3) return false;
        }

        return true;
    }

    private void removeNode(VisualNode vn) {
        circuit.removeNode(vn.node);
        visualNodes.remove(vn);
        visualWires.removeIf(w -> {
            boolean related = w.from == vn || w.to == vn;
            if (related && w == selectedWire) selectedWire = null;
            return related;
        });
    }

    private void draw(GraphicsContext gc) {
        // 1. 화면 배경 초기화 (줌/팬의 영향을 받지 않는 화면 고정 레이어)
        gc.setFill(Color.web("#2B2B2B"));
        gc.fillRect(0, 0, simulationCanvas.getWidth(), simulationCanvas.getHeight());

        gc.save();
        // 2. 카메라(팬, 줌) 적용
        gc.translate(cameraX, cameraY);
        gc.scale(zoom, zoom);

        // 3. 영구 전선 그리기
        for (VisualWire wire : visualWires) {
            boolean isHigh = (wire.from.node.getOut() & (1 << wire.outPin)) != 0;
            boolean isSelected = (wire == selectedWire);
            
            if (isSelected) {
                gc.setStroke(Color.web("#00FFFF"));
                gc.setLineWidth(5);
            } else {
                gc.setStroke(isHigh ? Color.web("#FF3366") : Color.web("#555555"));
                gc.setLineWidth(3);
            }
            
            double x1 = wire.from.getOutPinX(wire.outPin);
            double y1 = wire.from.getOutPinY(wire.outPin);
            double x2 = wire.to.getInPinX(wire.inPin);
            double y2 = wire.to.getInPinY(wire.inPin);
            
            gc.beginPath();
            gc.moveTo(x1, y1);
            gc.bezierCurveTo(x1 + 50, y1, x2 - 50, y2, x2, y2);
            gc.stroke();
        }

        // 4. 임시 전선 그리기 (선 긋는 중일 때 마우스를 따라가는 선)
        if (isWiring && wiringNode != null) {
            boolean isHigh = false;
            double startX, startY;

            if (isWiringFromOut) {
                isHigh = (wiringNode.node.getOut() & (1 << wiringPin)) != 0;
                startX = wiringNode.getOutPinX(wiringPin);
                startY = wiringNode.getOutPinY(wiringPin);
            } else {
                startX = wiringNode.getInPinX(wiringPin);
                startY = wiringNode.getInPinY(wiringPin);
            }

            gc.setStroke(isHigh ? Color.web("#FF3366") : Color.web("#888888"));
            gc.setLineWidth(3);
            gc.setLineDashes(5); 
            
            gc.beginPath();
            gc.moveTo(startX, startY);
            
            if (isWiringFromOut) {
                gc.bezierCurveTo(startX + 50, startY, worldMouseX - 50, worldMouseY, worldMouseX, worldMouseY);
            } else {
                gc.bezierCurveTo(startX - 50, startY, worldMouseX + 50, worldMouseY, worldMouseX, worldMouseY);
            }
            gc.stroke();
            gc.setLineDashes(null);
        }

        // 5. 노드(부품) 그리기
        for (VisualNode vn : visualNodes) {
            boolean isHovered = (vn == hoveredNode);
            boolean isSelected = (vn == selectedNode);
            
            boolean isConnectionInvalid = false;
            if (isWiring && isHovered) {
                if (isWiringFromOut && hoveredInPin != -1) {
                    isConnectionInvalid = !isValidConnection(wiringNode, wiringPin, vn, hoveredInPin);
                } else if (!isWiringFromOut && hoveredOutPin != -1) {
                    isConnectionInvalid = !isValidConnection(vn, hoveredOutPin, wiringNode, wiringPin);
                }
            }
            
            vn.draw(gc, isHovered, isSelected, isHovered ? hoveredInPin : -1, isHovered ? hoveredOutPin : -1, selectedWire, isConnectionInvalid);
        }

        // [추가] 6. 붙여넣기 미리보기(Ghost Preview)
        if (isPlacingImport && pendingProjectData != null) {
            gc.setGlobalAlpha(0.5); // 반투명
            
            // 미리보기 노드들
            for (NodeData nd : pendingProjectData.nodes) {
                double previewX = worldMouseX + nd.x;
                double previewY = worldMouseY + nd.y;
                Node dummyNode = createNodeByType(nd.type);
                VisualNode dummyVn = new VisualNode(dummyNode, previewX, previewY, nd.label);
                dummyVn.draw(gc, false, false, -1, -1, null, false);
            }

            // 미리보기 전선들
            gc.setStroke(Color.web("#AAAAAA"));
            gc.setLineWidth(2);
            for (WireData wd : pendingProjectData.wires) {
                NodeData fromNd = pendingProjectData.nodes.get(wd.fromIdx);
                NodeData toNd = pendingProjectData.nodes.get(wd.toIdx);
                
                // 좌표 계산용 임시 노드
                VisualNode fromVn = new VisualNode(createNodeByType(fromNd.type), worldMouseX + fromNd.x, worldMouseY + fromNd.y, "");
                VisualNode toVn = new VisualNode(createNodeByType(toNd.type), worldMouseX + toNd.x, worldMouseY + toNd.y, "");
                
                double x1 = fromVn.getOutPinX(wd.outPin);
                double y1 = fromVn.getOutPinY(wd.outPin);
                double x2 = toVn.getInPinX(wd.inPin);
                double y2 = toVn.getInPinY(wd.inPin);
                
                gc.beginPath();
                gc.moveTo(x1, y1);
                gc.bezierCurveTo(x1 + 50, y1, x2 - 50, y2, x2, y2);
                gc.stroke();
            }
            gc.setGlobalAlpha(1.0);
        }

        gc.restore();
    }

    public void shutdown() {
        if (timer != null) timer.stop();
        if (circuit != null) circuit.stopSimulation();
    }

    private double distanceToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double l2 = Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2);
        if (l2 == 0) return Math.hypot(px - x1, py - y1);
        double t = Math.max(0, Math.min(1, ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2));
        double projX = x1 + t * (x2 - x1);
        double projY = y1 + t * (y2 - y1);
        return Math.hypot(px - projX, py - projY);
    }

    // --- DTO Classes for JSON ---
    private static class ProjectData {
        List<NodeData> nodes = new ArrayList<>();
        List<WireData> wires = new ArrayList<>();
    }

    private static class NodeData {
        String type;
        double x, y;
        String label;

        NodeData(String type, double x, double y, String label) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.label = label;
        }
    }

    private static class WireData {
        int fromIdx, outPin;
        int toIdx, inPin;

        WireData(int fromIdx, int outPin, int toIdx, int inPin) {
            this.fromIdx = fromIdx;
            this.outPin = outPin;
            this.toIdx = toIdx;
            this.inPin = inPin;
        }
    }

    static class VisualWire {
        VisualNode from;
        int outPin;
        VisualNode to;
        int inPin;

        public VisualWire(VisualNode from, int outPin, VisualNode to, int inPin) {
            this.from = from;
            this.outPin = outPin;
            this.to = to;
            this.inPin = inPin;
        }
    }

    static class VisualNode {
        Node node;
        double x, y;
        double width = 80, height = 50;
        String label;

        public VisualNode(Node node, double x, double y, String label) {
            this.node = node;
            this.x = x;
            this.y = y;
            this.label = label;
            if (node instanceof InputPin || node instanceof OutputPin) {
                width = 50;
            } else if (node instanceof Joint) {
                width = 30;
                height = 30;
            }
        }

        public boolean contains(double px, double py) {
            return px >= x && px <= x + width && py >= y && py <= y + height;
        }

        public double getInPinX(int index) {
            if (node instanceof Joint) {
                if (index == 0) return x; // Left
                if (index == 1) return x + width; // Right
                if (index == 2) return x + width / 2; // Top
                if (index == 3) return x + width / 2; // Bottom
            }
            return x;
        }

        public double getInPinY(int index) {
            if (node instanceof Joint) {
                if (index == 0) return y + height / 2;
                if (index == 1) return y + height / 2;
                if (index == 2) return y;
                if (index == 3) return y + height;
            }
            int max = Math.max(1, node.getInputSize());
            return y + (height / (max + 1)) * (index + 1);
        }

        public double getOutPinX(int index) {
            if (node instanceof Joint) return getInPinX(index);
            return x + width;
        }

        public double getOutPinY(int index) {
            if (node instanceof Joint) return getInPinY(index);
            int max = Math.max(1, node.getOutputSize());
            return y + (height / (max + 1)) * (index + 1);
        }

        public void draw(GraphicsContext gc, boolean isHovered, boolean isSelected, int hoveredInPin, int hoveredOutPin, VisualWire selectedWire, boolean isConnectionInvalid) {
            boolean isOn = node.getOut() > 0;
            
            boolean isBodyHovered = isHovered && hoveredInPin == -1 && hoveredOutPin == -1;
            
            if (isSelected) {
                gc.setLineWidth(4);
                gc.setStroke(Color.web("#00FFFF"));
            } else if (isBodyHovered) {
                gc.setLineWidth(4);
                gc.setStroke(Color.web("#FFD700"));
            } else {
                gc.setLineWidth(2);
                gc.setStroke(Color.WHITE);
            }

            if (node instanceof Joint) {
                gc.setFill(Color.web("#888888"));
                gc.fillOval(x, y, width, height);
                gc.strokeOval(x, y, width, height);
            } else if (node instanceof InputPin) {
                gc.setFill(isOn ? Color.web("#FF3366") : Color.web("#444444"));
                gc.fillRoundRect(x, y, width, height, 10, 10);
                gc.strokeRoundRect(x, y, width, height, 10, 10);
                gc.setFill(Color.WHITE);
                gc.fillText(isOn ? "ON" : "OFF", x + 13, y + 30);
            } else if (node instanceof OutputPin) {
                gc.setFill(isOn ? Color.web("#33FF66") : Color.web("#333333"));
                gc.fillOval(x, y, width, height);
                gc.strokeOval(x, y, width, height);
                gc.setFill(isOn ? Color.BLACK : Color.WHITE);
                gc.fillText("LED", x + 13, y + 30);
            } else {
                gc.setFill(Color.web("#4A90E2"));
                gc.fillRect(x, y, width, height);
                gc.strokeRect(x, y, width, height);
                gc.setFill(Color.WHITE);
                gc.fillText(label, x + 25, y + 30);
            }

            // 입력 핀 그리기
            for (int i = 0; i < node.getInputSize(); i++) {
                boolean isPinHovered = isHovered && hoveredInPin == i;
                boolean isPinSelected = (selectedWire != null && selectedWire.to == this && selectedWire.inPin == i);
                
                if (isPinHovered && isConnectionInvalid) {
                    gc.setFill(Color.RED);
                } else if (isPinSelected) {
                    gc.setFill(Color.web("#00FFFF")); // 강조색 (Cyan)
                } else if (isPinHovered) {
                    gc.setFill(Color.web("#FFD700"));
                } else {
                    gc.setFill(Color.web("#AAAAAA"));
                }
                
                double radius = (isPinHovered || isPinSelected) ? 6 : 4;
                gc.fillOval(getInPinX(i) - radius, getInPinY(i) - radius, radius * 2, radius * 2);
            }

            // 출력 핀 그리기
            for (int i = 0; i < node.getOutputSize(); i++) {
                boolean isPinHovered = isHovered && hoveredOutPin == i;
                boolean isPinSelected = (selectedWire != null && selectedWire.from == this && selectedWire.outPin == i);

                if (isPinHovered && isConnectionInvalid) {
                    gc.setFill(Color.RED);
                } else if (isPinSelected) {
                    gc.setFill(Color.web("#00FFFF")); // 강조색 (Cyan)
                } else if (isPinHovered) {
                    gc.setFill(Color.web("#FFD700"));
                } else {
                    gc.setFill(Color.web("#AAAAAA"));
                }
                
                double radius = (isPinHovered || isPinSelected) ? 6 : 4;
                gc.fillOval(getOutPinX(i) - radius, getOutPinY(i) - radius, radius * 2, radius * 2);
            }
        }
    }
}