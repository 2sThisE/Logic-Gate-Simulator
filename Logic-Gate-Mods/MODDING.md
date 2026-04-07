# Logic Gate Simulator 모드 제작 가이드 🛠️

이 프로젝트는 외부 JAR 파일을 통해 새로운 논리 게이트나 디스플레이 컴포넌트를 추가할 수 있는 모드 시스템을 지원합니다.

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

## 3. 심볼 구현 (Symbol)
화면에 그려질 모양과 핀의 위치를 정의하는 클래스입니다.

- **상속**: `com.logicgate.editor.rendering.symbol.AbstractGateSymbol`
- **어노테이션**: `@ComponentMeta` 필수 (Node와 동일한 `typeId` 사용)

### 주요 오버라이드 메서드:
- `getSvgPathData(VisualNode vn)`: 기본 배경 모양을 SVG 경로 데이터로 반환
- `drawExtra(GraphicsContext gc, VisualNode vn)`: 게이트 내부의 추가 그래픽(텍스트, LED 등)을 그림
- `getInPinX/Y`, `getOutPinX/Y`: 핀이 위치할 좌표 정의
- `getInPinName`, `getOutPinName`: 핀 위에 마우스를 올렸을 때 표시될 툴팁 이름

---

## 4. 모드 적용 방법
1. 프로젝트를 **JAR 파일**로 빌드합니다.
2. 생성된 JAR 파일을 시뮬레이터 실행 경로의 `mods/` 폴더에 넣습니다.
3. 시뮬레이터를 실행하면 좌측 컴포넌트 리스트에 지정한 `section` 아래에 새로운 컴포넌트가 나타납니다.

---

## 5. 주의 사항 ⚠️
- **typeId 일치**: Node와 Symbol의 `typeId`가 정확히 일치해야 정상적으로 화면에 표시됩니다.
- **기본 생성자**: 모드 로더가 리플렉션을 사용하므로, 매개변수가 없는 기본 생성자가 반드시 존재해야 합니다.
- **JavaFX**: 심볼 렌더링은 JavaFX의 `GraphicsContext`를 사용합니다.
