package com.logicgate.editor.model;

import com.logicgate.editor.rendering.symbol.GateSymbol;
import com.logicgate.editor.rendering.symbol.SymbolRegistry;
import com.logicgate.gates.InputPin;
import com.logicgate.gates.Joint;
import com.logicgate.gates.Node;
import com.logicgate.gates.OutputPin;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class VisualNode {
    public Node node;
    public double x, y;
    public double width = 80, height = 50;
    public String label;
    public boolean showLabel = false;
    public String group = null;
    
    private double dragStartX, dragStartY;

    public VisualNode(Node node, double x, double y, String label) {
        this.node = node;
        this.x = x;
        this.y = y;
        
        GateSymbol symbol = SymbolRegistry.getSymbol(node.getTypeId());
        
        if (symbol != null) {
            this.width = symbol.getPreferredWidth();
            this.height = symbol.getPreferredHeight();
            this.label = (label == null || label.isEmpty()) ? symbol.getDefaultLabel() : label;
        } else {
            this.width = 80;
            this.height = 50;
            this.label = (label == null || label.isEmpty()) ? node.getClass().getSimpleName() : label;
        }
    }

    public java.util.List<Property<?>> getProperties(com.logicgate.editor.state.EditorContext context) {
        java.util.List<Property<?>> props = new java.util.ArrayList<>();
        
        // 공통 속성: 라벨 💖
        props.add(new Property<>("라벨", label, Property.Type.STRING, newVal -> {
            this.label = (String) newVal;
            context.setDirty(true);
        }));

        // 공통 속성: 라벨 표시 여부 🔪💕
        props.add(new Property<>("라벨 표시", showLabel, Property.Type.BOOLEAN, newVal -> {
            this.showLabel = (Boolean) newVal;
            context.setDirty(true);
        }));

        // 공통 속성: 그룹 🔪
        props.add(new Property<>("그룹 이름", group == null ? "" : group, Property.Type.STRING, newVal -> {
            String newGroup = (String) newVal;
            if (newGroup.isEmpty()) {
                this.group = null;
            } else {
                if (this.group != null && !newGroup.equals(this.group)) {
                    String oldGroup = this.group;
                    String targetName = newGroup;
                    int suffix = 1;
                    
                    while (true) {
                        boolean exists = false;
                        for (VisualNode vn : context.visualNodes) {
                            if (targetName.equals(vn.group) && (oldGroup == null || !oldGroup.equals(vn.group))) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) break;
                        targetName = newGroup + "-" + suffix;
                        suffix++;
                    }
                    
                    for (VisualNode vn : context.visualNodes) {
                        if (oldGroup.equals(vn.group)) {
                            vn.group = targetName;
                        }
                    }
                    
                    if (!targetName.equals(newGroup)) {
                        javafx.application.Platform.runLater(() -> {
                            if (context.onSelectionChanged != null) {
                                context.onSelectionChanged.run();
                            }
                        });
                    }
                } else {
                    this.group = newGroup;
                }
            }
            context.setDirty(true);
        }));

        // 컴포넌트 전용 속성들 추가 ✨
        // 노드 속성들의 콜백을 래핑하여 변경 시 dirty 플래그가 설정되도록 함 🔪💕
        for (Property<?> nodeProp : node.getComponentProperties()) {
            props.add(new Property(nodeProp.getName(), nodeProp.getValue(), nodeProp.getType(), nodeProp.getOptions(), newVal -> {
                
                // 조인트 핀 수 감소 시 안전 검사 🔪💕
                if (this.node instanceof com.logicgate.gates.Joint && "단자 수 (2~8)".equals(nodeProp.getName())) {
                    int newCount = (Integer) newVal;
                    int currentCount = (Integer) nodeProp.getValue();
                    
                    if (newCount < currentCount) {
                        // 삭제될 범위(newCount ~ currentCount-1)에 연결된 와이어가 있는지 확인
                        for (com.logicgate.editor.model.VisualWire vw : context.visualWires) {
                            if ((vw.from == this && vw.outPin >= newCount) || 
                                (vw.to == this && vw.inPin >= newCount)) {
                                System.err.println("연결된 선이 있는 단자는 제거할 수 없습니다! 먼저 선을 지워주세요.");
                                return; // 변경을 적용하지 않고 리턴 ✨
                            }
                        }
                    }
                }

                ((Property<Object>) nodeProp).setValue(newVal);
                context.setDirty(true);
            }));
        }
        
        return props;
    }

    public void setDragStart(double x, double y) {
        this.dragStartX = x;
        this.dragStartY = y;
    }

    public double getDragStartX() { return dragStartX; }
    public double getDragStartY() { return dragStartY; }

    public boolean contains(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    public double getInPinX(int index) {
        GateSymbol symbol = SymbolRegistry.getSymbol(node.getTypeId());
        return symbol != null ? symbol.getInPinX(this, index) : x;
    }

    public double getInPinY(int index) {
        GateSymbol symbol = SymbolRegistry.getSymbol(node.getTypeId());
        return symbol != null ? symbol.getInPinY(this, index) : y;
    }

    public double getOutPinX(int index) {
        GateSymbol symbol = SymbolRegistry.getSymbol(node.getTypeId());
        return symbol != null ? symbol.getOutPinX(this, index) : x;
    }

    public double getOutPinY(int index) {
        GateSymbol symbol = SymbolRegistry.getSymbol(node.getTypeId());
        return symbol != null ? symbol.getOutPinY(this, index) : y;
    }

    public void draw(GraphicsContext gc, boolean isHovered, boolean isSelected, int hoveredInPin, int hoveredOutPin, VisualWire selectedWire, boolean isConnectionInvalid) {
        boolean isBodyHovered = isHovered && hoveredInPin == -1 && hoveredOutPin == -1;
        
        GateSymbol symbol = SymbolRegistry.getSymbol(node.getTypeId());
        if (symbol != null) {
            symbol.draw(gc, this, isBodyHovered, isSelected);
        } else {
            gc.setFill(Color.web("#4A90E2"));
            gc.fillRect(x, y, width, height);
            gc.strokeRect(x, y, width, height);
            if (showLabel) {
                gc.setFill(Color.WHITE);
                gc.fillText(label, x + 25, y + 30);
            }
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