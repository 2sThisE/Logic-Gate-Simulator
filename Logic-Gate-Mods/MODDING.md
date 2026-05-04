# Logic Gate Simulator 모드(Mod) 제작 가이드

Logic Gate Simulator는 외부 JAR 파일을 통해 새로운 논리 게이트나 디스플레이 컴포넌트, 메모리 등을 동적으로 추가할 수 있는 강력한 **모딩(Modding) 시스템**을 지원합니다.

---

## 1. 모딩 시스템 개요
모드 컴포넌트는 크게 **로직(Node)**과 **그래픽(Symbol)** 두 부분으로 나뉘며, 메타데이터 어노테이션인 `@ComponentMeta`를 통해 서로 연결됩니다. 시뮬레이터 구동 중에 모드 로더(ModLoader)가 JAR 파일을 스캔하여 이 두 가지 클래스를 런타임에 등록합니다.

- **Node**: 컴포넌트의 내부 상태, 핀의 개수, 시뮬레이션 연산 로직을 정의합니다.
- **Symbol**: 에디터 상에서 컴포넌트가 어떻게 그려질지, 핀의 위치는 어디일지를 정의합니다.
- **ComponentMeta**: 트리 뷰에 표시될 정보와 Node, Symbol을 하나로 묶어주는 `typeId`를 제공합니다.

---

## 2. 노드 구현 (Node 클래스)
실제 논리 연산이나 상태를 관리하는 클래스입니다.

- **상속**: `com.logicgate.gates.Node`
- **어노테이션**: `@ComponentMeta` 필수
  - `name`: 좌측 컴포넌트 트리에 표시될 이름
  - `section`: 컴포넌트 트리의 카테고리 (예: "Arithmetic", "Memory")
  - `typeId`: 심볼과 노드를 연결하는 고유 ID 문자열

```java
import com.logicgate.editor.mod.ComponentMeta;
import com.logicgate.gates.Node;

@ComponentMeta(
    name = "My Custom Gate",
    section = "Logic",
    typeId = "MY_CUSTOM_GATE"
)
public class MyNode extends Node {
    
    public MyNode() {
        // 부모 생성자: (입력 핀 개수, 출력 핀 개수)
        super(2, 1); 
        this.typeId = "MY_CUSTOM_GATE"; // 부모 필드에 typeId 지정
    }

    /**
     * 시뮬레이터 틱마다 호출되는 연산 로직.
     * in 필드(long 타입)에는 연결된 입력 핀들의 비트 상태가 들어있습니다.
     * 연산 후 out 필드(long 타입)에 결과를 저장합니다.
     */
    @Override
    public void compute() {
        // 비트 연산을 통한 입력 값 추출 (예: 최대 64개 핀)
        long a = (in & 1);
        long b = (in >> 1) & 1;

        // 예시: AND 연산 후 출력에 저장
        out = a & b;
    }
}
```

---

## 3. 심볼 구현 (Symbol 클래스)
화면에 그려질 모양과 각 핀의 정확한 렌더링 위치를 정의합니다. Node의 상태나 동적 속성을 참조하여 그래픽을 그릴 수 있습니다.

- **상속**: `com.logicgate.editor.rendering.symbol.AbstractGateSymbol`
- **어노테이션**: Node와 동일한 `typeId`를 가진 `@ComponentMeta` 필수

```java
import com.logicgate.editor.mod.ComponentMeta;
import com.logicgate.editor.model.VisualNode;
import com.logicgate.editor.rendering.symbol.AbstractGateSymbol;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

@ComponentMeta(
    name = "My Custom Gate",
    section = "Logic",
    typeId = "MY_CUSTOM_GATE"
)
public class MySymbol extends AbstractGateSymbol {

    @Override
    public void draw(GraphicsContext gc, VisualNode vn, boolean isHovered, boolean isSelected) {
        gc.save();
        // Hover/Select 상태에 따라 그림자 등을 기본 처리
        prepareFill(gc, vn, isHovered, isSelected);

        // 노드의 커스텀 속성을 가져와 렌더링에 반영 (선택 사항)
        String color = "#888888";
        if (vn.node instanceof MyNode) {
            // color = ((MyNode) vn.node).getBgColor();
        }

        // 컴포넌트 본체 그리기
        gc.setFill(Color.web(color));
        gc.fillRect(0, 0, vn.width, vn.height);

        gc.restore();
    }

    // 각 핀의 절대 좌표(Canvas 기준) 반환
    @Override
    public double getInPinX(VisualNode vn, int index) { return vn.x; }
    
    @Override
    public double getInPinY(VisualNode vn, int index) { 
        // 핀의 인덱스에 따라 Y축 위치 계산
        return vn.y + 10 + (index * 20); 
    }

    @Override
    public double getOutPinX(VisualNode vn, int index) { return vn.x + vn.width; }
    
    @Override
    public double getOutPinY(VisualNode vn, int index) { return vn.y + vn.height / 2.0; }

    // 핀에 마우스를 올렸을 때 나타날 툴팁 텍스트
    @Override
    public String getInPinName(int index) { return "Input " + index; }
    
    @Override
    public String getOutPinName(int index) { return "Output " + index; }

    // 에디터 배치 시 크기 단위
    @Override
    public int getUnitWidth(){ return 6; }
    @Override
    public int getUnitHeight(){ return 6; }
}
```

---

## 4. 커스텀 속성 추가 (Properties API)
사용자가 시뮬레이터 우측 패널에서 컴포넌트의 설정(색상, 비트 수, 모드 등)을 실시간으로 변경하도록 만들 수 있습니다. **이 속성들은 파일 저장/불러오기 및 Undo/Redo 대상에 자동 포함**됩니다.

`Node` 클래스 내부에서 다음 두 메서드를 오버라이드합니다.

### A. 속성 정의 (`getComponentProperties`)
사용자에게 노출할 `Property` 객체 리스트를 반환합니다.

```java
private String bgColor = "#336699";
private String currentMode = "Fast";

@Override
public List<Property<?>> getComponentProperties() {
    List<Property<?>> props = super.getComponentProperties();

    // 1. 색상 선택기 추가 (Property.Type.COLOR)
    props.add(new Property<>("배경 색상", bgColor, Property.Type.COLOR, newVal -> {
        this.bgColor = (String) newVal;
        this.properties.put("bgColor", bgColor); // Map에 저장 (파일 저장/Undo용)
    }));

    // 2. 콤보박스 선택기 추가 (Property.Type.CHOICE)
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
파일을 불러오거나 Undo/Redo 실행 시, 시뮬레이터가 `this.properties` Map의 데이터를 채운 뒤 이 메서드를 호출합니다. Map의 데이터를 필드에 반영하세요.

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

---

## 5. 도움말(Help) 파일 추가
새로 만든 모드에 대한 설명서를 추가하려면 JAR 파일 내의 특정 경로에 마크다운(`.md`) 파일을 포함하면 됩니다. 시뮬레이터가 이 경로를 스캔하여 메뉴얼에 자동으로 추가합니다.

1. `src/main/resources/META-INF/logicgate/help/` 디렉토리를 생성합니다.
2. 해당 디렉토리 안에 마크다운 파일을 작성합니다. (예: `my-custom-gate.md`)
3. 마크다운의 **가장 첫 번째 `# 제목` 헤딩**이 도움말 뷰어의 트리 제목(예: `Mod: 제목`)으로 사용됩니다.

**예시 (`META-INF/logicgate/help/my-custom-gate.md`):**
```markdown
# My Custom Gate

이 커스텀 게이트는 특별한 AND 연산을 수행합니다...
```

---

## 6. 빌드 및 배포
1. Maven 기반 프로젝트를 생성하고, 컴포넌트들을 작성합니다. `pom.xml` 구성은 `Logic-Gate-Mods`의 하위 모드 프로젝트(예: `Bus-Mod`)를 참조하세요.
2. 프로젝트를 **JAR 파일**로 빌드합니다. (`mvn clean package`)
3. 시뮬레이터를 실행하고 우측 상단의 **모드 관리자(Mod Manager)**를 엽니다.
4. "Add Mod" 버튼을 통해 만들어진 JAR 파일을 불러옵니다.
5. 재시작 혹은 트리를 확장하여 새 컴포넌트를 사용합니다!

---

## ⚠️ 주의 사항
- **기본 생성자**: 모드 로더가 Java Reflection을 사용해 컴포넌트를 생성하므로, `Node`와 `Symbol` 클래스 모두 **매개변수가 없는 기본 생성자(Default Constructor)**를 가져야 합니다.
- **JavaFX Thread**: `Symbol`의 `draw` 메서드는 UI 스레드에서 실행됩니다. 하지만 `Node`의 `compute` 메서드는 백그라운드 시뮬레이션 스레드에서 초당 수십~수백 번 실행될 수 있으므로 UI 객체를 직접 조작해서는 안 됩니다.
