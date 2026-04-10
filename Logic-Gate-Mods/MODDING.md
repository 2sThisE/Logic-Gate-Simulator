# Logic Gate Simulator 모드 제작 가이드 🛠️

이 프로젝트는 외부 JAR 파일을 통해 새로운 논리 게이트나 디스플레이 컴포넌트를 추가할 수 있는 모드 시스템을 지원합니다.

---

## 1. 기본 구조
모드 컴포넌트는 크게 **Node(로직)**와 **Symbol(그래픽)** 두 부분으로 나뉩니다. 시뮬레이터는 `@ComponentMeta` 어노테이션의 `typeId`를 통해 이 둘을 한 쌍으로 연결합니다.

---

## 2. 노드 구현 (Node)
실제 논리 연산이나 상태를 관리하는 클래스입니다.

- **상속**: `com.logicgate.gates.Node`
- **어노테이션**: `@ComponentMeta` 필수
  - `name`: 트리뷰에 표시될 이름
  - `section`: 트리뷰의 카테고리 (예: "Arithmetic", "Display")
  - `typeId`: 심볼과 연결할 고유 ID

```java
@ComponentMeta(
    name = "My Custom Gate",
    section = "Logic",
    typeId = "MY_CUSTOM_GATE"
)
public class MyNode extends Node {
    public MyNode() {
        super(2, 1); // 입력 핀 2개, 출력 핀 1개
    }

    @Override
    public void compute() {
        // in: 32비트 비트마스크 입력 상태
        int a = (in & 1);
        int b = (in >> 1) & 1;
        
        // 연산 후 out(32비트 비트마스크)에 결과 저장
        out = a & b; 
    }
}
```

---

## 3. ✨ 컴포넌트 속성 추가 (New!)
사용자가 오른쪽 속성창에서 부품의 설정을 실시간으로 변경할 수 있게 하는 기능입니다. 모든 설정은 **자동으로 저장/불러오기 및 Undo/Redo 대상**이 됩니다.

### A. 속성 정의 (`getComponentProperties`)
노드에서 `getComponentProperties()`를 오버라이드하여 속성 리스트를 반환합니다.

```java
@Override
public List<Property<?>> getComponentProperties() {
    List<Property<?>> props = super.getComponentProperties();
    
    // 1. 색상 선택기 추가 🎨
    props.add(new Property<>("배경 색상", bgColor, Property.Type.COLOR, newVal -> {
        this.bgColor = (String) newVal;
        this.properties.put("bgColor", bgColor); // 맵에 저장 (파일 저장용)
    }));

    // 2. 선택창(ComboBox) 추가 🔘
    props.add(new Property<>("동작 모드", currentMode, Property.Type.CHOICE, 
        new String[]{"Fast", "Slow", "Normal"}, newVal -> {
            this.currentMode = (String) newVal;
            this.properties.put("mode", currentMode);
        }
    ));

    return props;
}
```

### B. 데이터 복구 (`applyProperties`)
파일을 불러올 때 저장된 `properties` 맵의 값을 실제 필드에 다시 적용합니다.

```java
@Override
protected void applyProperties() {
    if (properties.containsKey("bgColor")) {
        this.bgColor = properties.get("bgColor");
    }
    if (properties.containsKey("mode")) {
        this.currentMode = properties.get("mode");
    }
}
```

### C. 지원하는 속성 타입 (`Property.Type`)
- `COLOR`: JavaFX ColorPicker를 사용하여 `#RRGGBB` 형식의 문자열을 다룹니다.
- `BOOLEAN`: CheckBox를 생성합니다.
- `INTEGER`: 2~8 사이의 슬라이더를 생성합니다. (필요 시 범위 조절 가능)
- `STRING`: 일반 텍스트 입력창을 생성합니다.
- `CHOICE`: ComboBox(드롭다운)를 생성합니다. 옵션 목록이 필요합니다.

---

## 4. 심볼 구현 (Symbol)
화면에 그려질 모양과 핀의 위치를 정의합니다. 노드의 속성 값을 참조하여 동적으로 그릴 수 있습니다.

- **상속**: `com.logicgate.editor.rendering.symbol.AbstractGateSymbol`

### 주요 오버라이드 메서드:
- `draw(GraphicsContext gc, VisualNode vn, boolean isHovered, boolean isSelected)`: 전체적인 렌더링 로직.
- `getInPinX/Y`, `getOutPinX/Y`: 핀 위치 정의. 노드의 `inputSize` 등을 참조해 동적 배치가 가능합니다.

```java
@Override
public void draw(GraphicsContext gc, VisualNode vn, boolean isHovered, boolean isSelected) {
    gc.save();
    gc.translate(vn.x, vn.y);
    
    // 노드의 속성 값 참조 예시 🎨
    String color = "#888888";
    if (vn.node instanceof MyNode) {
        color = ((MyNode) vn.node).getBgColor();
    }
    
    gc.setFill(Color.web(color));
    gc.fillRect(0, 0, vn.width, vn.height);
    
    gc.restore();
}
```

---

## 5. 모드 적용 방법
1. 프로젝트를 **JAR 파일**로 빌드합니다.
2. 생성된 JAR 파일을 시뮬레이터 실행 경로의 `mods/` 폴더에 넣습니다.
3. 시뮬레이터를 실행하면 좌측 컴포넌트 리스트에 지정한 `section` 아래에 새로운 컴포넌트가 나타납니다.

---

## 6. 주의 사항 ⚠️
- **기본 생성자**: 모드 로더가 리플렉션을 사용하므로, 매개변수가 없는 기본 생성자가 반드시 존재해야 합니다.
- **속성 변경 시 Dirty 플래그**: 시뮬레이터가 속성 변경을 자동으로 감지하므로 `context.setDirty(true)`를 직접 호출할 필요가 없습니다.
- **JavaFX**: 심볼 렌더링은 JavaFX의 `GraphicsContext`를 사용합니다.
