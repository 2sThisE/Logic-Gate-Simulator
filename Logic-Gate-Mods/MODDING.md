# Logic Gate Simulator 모드 제작 가이드

[English](MODDING.en.md) | 한국어

Logic Gate Simulator는 외부 JAR 파일을 통해 새로운 논리 게이트, 표시 장치, 메모리, 연산 컴포넌트를 추가할 수 있는 모딩 시스템을 제공합니다.

이 문서는 모드 컴포넌트를 만드는 기본 구조와 주의할 점을 설명합니다.

## 1. 모딩 시스템 개요

모드 컴포넌트는 보통 두 클래스로 구성됩니다.

- **Node**: 핀 개수, 내부 상태, 논리 연산을 담당합니다.
- **Symbol**: 에디터 화면에서 컴포넌트를 어떻게 그릴지와 핀 위치를 담당합니다.

두 클래스는 `@ComponentMeta`의 `typeId`로 연결됩니다. 앱은 모드 JAR을 스캔해 같은 `typeId`를 가진 Node와 Symbol을 등록합니다.

```java
@ComponentMeta(section = "Arithmetic", name = "Full Adder", typeId = "FULL_ADDER")
public class FullAdderNode extends Node {
    ...
}

@ComponentMeta(section = "Arithmetic", name = "Full Adder Symbol", typeId = "FULL_ADDER")
public class FullAdderSymbol extends AbstractGateSymbol {
    ...
}
```

## 2. Node 만들기

Node는 실제 회로 시뮬레이션 로직을 담당합니다.

- `com.logicgate.api.component.Node`를 상속합니다.
- 매개변수가 없는 기본 생성자가 필요합니다.
- 생성자에서 `super(inputSize, outputSize)`로 입력/출력 핀 개수를 지정합니다.
- `compute()`에서 입력 비트 `in`을 읽고 출력 비트 `out`을 설정합니다.
- `in`과 `out`은 `int` 기반 비트 필드입니다. 현재 구조에서는 최대 32개의 입력/출력 비트를 다룰 수 있습니다.

```java
package com.example.logicgate.mods;

import com.logicgate.api.component.ComponentMeta;
import com.logicgate.api.component.Node;

@ComponentMeta(section = "Logic", name = "My AND", typeId = "MY_AND")
public class MyAndNode extends Node {

    public MyAndNode() {
        super(2, 1);
    }

    @Override
    public void compute() {
        int a = in & 1;
        int b = (in >> 1) & 1;
        out = a & b;
    }
}
```

## 3. Symbol 만들기

Symbol은 컴포넌트의 모양, 크기, 핀 위치, 툴팁 이름을 정의합니다.

- `com.logicgate.api.rendering.AbstractGateSymbol`을 상속합니다.
- Node와 같은 `typeId`를 가진 `@ComponentMeta`를 붙입니다.
- `getSvgPathData()`에서 컴포넌트 외형을 SVG path 문자열로 반환합니다.
- 추가 그리기가 필요하면 JavaFX 대신 `DrawingContext` 명령을 사용합니다.
- 기본 핀 위치 계산이 충분하면 `getInPinX/Y`, `getOutPinX/Y`는 생략할 수 있습니다.

```java
package com.example.logicgate.mods;

import com.logicgate.api.rendering.SymbolContext;
import com.logicgate.api.component.ComponentMeta;
import com.logicgate.api.rendering.AbstractGateSymbol;

@ComponentMeta(section = "Logic", name = "My AND Symbol", typeId = "MY_AND")
public class MyAndSymbol extends AbstractGateSymbol {

    @Override
    public String getSvgPathData(SymbolContext context) {
        return String.format(
            "M 0 0 L %f 0 L %f %f L 0 %f Z",
            context.width(), context.width(), context.height(), context.height()
        );
    }

    @Override
    public String getDefaultLabel() {
        return "MY AND";
    }

    @Override
    public String getInPinName(int index) {
        return index == 0 ? "A" : "B";
    }

    @Override
    public String getOutPinName(int index) {
        return "OUT";
    }

    @Override
    public int getUnitWidth() {
        return 8;
    }

    @Override
    public int getUnitHeight() {
        return 6;
    }
}
```

## 4. 속성 추가

컴포넌트 속성은 오른쪽 속성 패널에 표시됩니다. 속성 값은 프로젝트 저장/불러오기와 실행 취소/다시 실행에 포함되도록 `properties` Map에도 저장해야 합니다.

지원 타입:

- `COLOR`
- `BOOLEAN`
- `INTEGER`
- `STRING`
- `CHOICE`

```java
import com.logicgate.api.component.Property;
import java.util.List;

private String color = "#336699";
private boolean inverted = false;

@Override
public List<Property<?>> getComponentProperties() {
    List<Property<?>> props = super.getComponentProperties();

    props.add(new Property<>("Color", color, Property.Type.COLOR, value -> {
        color = (String) value;
        properties.put("color", color);
    }));

    props.add(new Property<>("Inverted", inverted, Property.Type.BOOLEAN, value -> {
        inverted = (Boolean) value;
        properties.put("inverted", Boolean.toString(inverted));
    }));

    return props;
}

@Override
protected void applyProperties() {
    if (properties.containsKey("color")) {
        color = properties.get("color");
    }
    if (properties.containsKey("inverted")) {
        inverted = Boolean.parseBoolean(properties.get("inverted"));
    }
}
```

## 5. 알림 표시

모드는 `NotificationApi`를 사용해 에디터 우측 하단에 안내, 경고, 에러 알림을 표시할 수 있습니다. 짧은 상태 보고나 사용자가 알아야 하는 문제를 알릴 때 사용하세요.

```java
import com.logicgate.api.notification.NotificationApi;

NotificationApi.info("출력 완료", "16비트 컴퓨터가 값을 출력했습니다.");
NotificationApi.warning("입력 범위 초과", "입력 값이 16비트 범위를 벗어났습니다.");
NotificationApi.error("실행 오류", "명령어를 해석할 수 없습니다.");
```

타입을 직접 지정할 수도 있습니다.

```java
NotificationApi.show(
    NotificationApi.Type.WARNING,
    "상태 확인 필요",
    "일부 입력 핀이 연결되지 않았습니다."
);
```

알림 제목과 본문은 모드가 문자열로 전달합니다. 다국어 지원이 필요하면 모드 JAR 안에 자체 `ResourceBundle`을 넣고 현재 `Locale`에 맞는 문자열을 꺼내서 전달하세요.

```java
import java.util.Locale;
import java.util.ResourceBundle;

ResourceBundle bundle = ResourceBundle.getBundle("com.example.my_mod.strings", Locale.getDefault());

NotificationApi.warning(
    bundle.getString("warning.title"),
    bundle.getString("warning.body")
);
```

`compute()`는 매우 자주 호출되므로 알림을 매번 띄우면 안 됩니다. 같은 상태에서 한 번만 알림을 띄우도록 모드 내부에서 플래그나 마지막 상태 값을 관리하세요.

```java
private boolean warnedMissingInput = false;

@Override
public void compute() {
    boolean missingInput = (in & 1) == 0;
    if (missingInput && !warnedMissingInput) {
        NotificationApi.warning("입력 누락", "IN0 핀이 LOW 상태입니다.");
        warnedMissingInput = true;
    } else if (!missingInput) {
        warnedMissingInput = false;
    }
}
```

## 6. 도움말 문서 추가

모드 JAR 안에 마크다운 문서를 포함하면 앱의 도움말 창에서 자동으로 읽을 수 있습니다.

1. `src/main/resources/META-INF/logicgate/help/` 디렉터리를 만듭니다.
2. 그 안에 `.md` 파일을 추가합니다.
3. 문서의 첫 번째 `# 제목`이 도움말 항목 제목으로 사용됩니다.

예시:

```markdown
# My AND

My AND는 두 입력이 모두 HIGH일 때 출력이 HIGH가 되는 예제 컴포넌트입니다.
```

## 7. Maven 프로젝트 구성

모드 프로젝트는 별도로 분리된 Mod API를 `provided` 의존성으로 참조합니다. API 클래스는 앱이 런타임에 제공하므로 모드 JAR에 포함하지 마세요. 예제 모드들의 `pom.xml`을 복사해서 시작하는 것을 권장합니다.

```xml
<dependency>
  <groupId>com.logicgate</groupId>
  <artifactId>logicgate-mod-api</artifactId>
  <version>1.1.1-SNAPSHOT</version>
  <scope>provided</scope>
</dependency>
```

논리와 렌더링 계약 모두 JavaFX와 본체에 의존하지 않습니다. 커스텀 Symbol도 위 API 하나만 사용하며, 모든 모드는 JDK 21을 사용해야 합니다.

이 저장소의 루트 디렉터리에서 API를 로컬 Maven 저장소에 설치하려면 다음을 실행합니다.

```bash
mvn install -pl Logic-Gate-Mod-API
```

빌드:

```bash
mvn clean package
```

생성된 JAR 파일을 앱의 **Mod Manager**에서 추가하면 됩니다.

## 8. 예제 모드

이 저장소의 `Logic-Gate-Mods/` 아래에 예제 모드가 포함되어 있습니다.

- `FullAdder-Mod`
- `Bus-Mod`
- `RAM-Mod`
- `Seven-Segment-Mod`

새 모드를 만들 때는 이 프로젝트들의 구조와 `pom.xml`을 참고하세요.

## 주의 사항

- Node와 Symbol 클래스 모두 기본 생성자가 필요합니다.
- `compute()`는 시뮬레이션 스레드에서 반복 호출됩니다. UI 객체를 직접 조작하지 마세요.
- Symbol에서는 JavaFX 클래스 대신 `DrawingContext`와 `SymbolContext`만 사용하세요.
- `compute()`에서 알림을 사용할 때는 같은 상태에서 반복 표시되지 않도록 중복 방지 로직을 넣으세요.
- 알림의 다국어 처리는 모드 내부의 `ResourceBundle`에서 처리한 뒤 문자열로 전달하세요.
- `typeId`는 고유해야 합니다. 다른 기본 컴포넌트나 모드와 충돌하지 않도록 접두사를 붙이는 것을 권장합니다.
- 저장해야 하는 속성은 반드시 `properties` Map에도 반영하세요.
- 신뢰할 수 없는 JAR 모드는 실행하지 마세요. 모드는 앱 내부에서 외부 코드를 로드합니다.
