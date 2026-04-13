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
    public double rotation = 0; // 회전각 (도 단위) ✨
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
            // 칸(Unit) 단위를 픽셀 단위로 변환 ✨
            this.width = symbol.getUnitWidth() * GateSymbol.UNIT_SIZE;
            this.height = symbol.getUnitHeight() * GateSymbol.UNIT_SIZE;
            this.label = (label == null || label.isEmpty()) ? symbol.getDefaultLabel() : label;
        } else {
            this.width = 80;
            this.height = 60;
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
        // 회전된 사각형 내부에 점이 있는지 확인 🔪💕
        // 점을 역회전시켜 축 정렬된 사각형과 비교하는 방식
        double cx = x + width / 2;
        double cy = y + height / 2;
        
        double rad = Math.toRadians(-rotation);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        
        double dx = px - cx;
        double dy = py - cy;
        
        double rx = dx * cos - dy * sin;
        double ry = dx * sin + dy * cos;
        
        return rx >= -width / 2 && rx <= width / 2 && ry >= -height / 2 && ry <= height / 2;
    }

    private double getRotatedX(double localX, double localY) {
        double cx = width / 2;
        double cy = height / 2;
        double rad = Math.toRadians(rotation);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double dx = localX - cx;
        double dy = localY - cy;
        return (x + cx) + (dx * cos - dy * sin);
    }

    private double getRotatedY(double localX, double localY) {
        double cx = width / 2;
        double cy = height / 2;
        double rad = Math.toRadians(rotation);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double dx = localX - cx;
        double dy = localY - cy;
        return (y + cy) + (dx * sin + dy * cos);
    }

    public double getInPinX(int index) {
        GateSymbol symbol = SymbolRegistry.getSymbol(node.getTypeId());
        if (symbol == null) return x;
        return getRotatedX(symbol.getInPinX(this, index) - x, symbol.getInPinY(this, index) - y);
    }

    public double getInPinY(int index) {
        GateSymbol symbol = SymbolRegistry.getSymbol(node.getTypeId());
        if (symbol == null) return y;
        return getRotatedY(symbol.getInPinX(this, index) - x, symbol.getInPinY(this, index) - y);
    }

    public double getOutPinX(int index) {
        GateSymbol symbol = SymbolRegistry.getSymbol(node.getTypeId());
        if (symbol == null) return x + width;
        return getRotatedX(symbol.getOutPinX(this, index) - x, symbol.getOutPinY(this, index) - y);
    }

    public double getOutPinY(int index) {
        GateSymbol symbol = SymbolRegistry.getSymbol(node.getTypeId());
        if (symbol == null) return y + height / 2;
        return getRotatedY(symbol.getOutPinX(this, index) - x, symbol.getOutPinY(this, index) - y);
    }

    public void draw(GraphicsContext gc, boolean isHovered, boolean isSelected, int hoveredInPin, int hoveredOutPin, VisualWire selectedWire, boolean isConnectionInvalid) {
        boolean isBodyHovered = isHovered && hoveredInPin == -1 && hoveredOutPin == -1;
        
        gc.save();
        // 부품 중앙을 기준으로 회전 행렬 적용 ✨
        gc.translate(x + width / 2, y + height / 2);
        gc.rotate(rotation);
        gc.translate(-width / 2, -height / 2);

        GateSymbol symbol = SymbolRegistry.getSymbol(node.getTypeId());
        if (symbol != null) {
            symbol.draw(gc, this, isBodyHovered, isSelected);
        } else {
            gc.setFill(Color.web("#4A90E2", 0.8));
            gc.fillRect(0, 0, width, height); 
            gc.setStroke(isSelected ? Color.web("#00FFFF") : (isBodyHovered ? Color.web("#FFD700") : Color.WHITE));
            gc.setLineWidth(isSelected || isBodyHovered ? 4 : 2);
            gc.strokeRect(0, 0, width, height);
        }

        // 공통 라벨 드로잉 (심볼 내부 로직과 분리하여 모든 부품 적용) ✨
        if (showLabel && label != null && !label.isEmpty()) {
            gc.save();
            double lx, ly;
            if (symbol != null) {
                lx = symbol.getLabelX(this);
                ly = symbol.getLabelY(this);
            } else {
                lx = width * 0.3;
                ly = height + 15;
            }
            gc.translate(lx, ly);
            gc.rotate(-rotation); // 역회전 적용 ✨
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", 12));
            gc.fillText(label, 0, 0);
            gc.restore();
        }
        gc.restore();

        // 핀 그리기 (회전된 위치 계산)
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