# Logic Gate Simulator (논리 회로 시뮬레이터)

Java와 JavaFX를 기반으로 구축된 강력하고 상호작용이 뛰어난 확장 가능한 디지털 논리 회로 시뮬레이터입니다.

복잡한 디지털 회로를 쉽게 설계, 테스트 및 시각화하고, 내장된 모딩(Modding) API를 사용하여 시뮬레이터의 기능을 확장해보세요!

![Logic Gate Simulator](https://img.shields.io/badge/Java-21-blue.svg) ![JavaFX](https://img.shields.io/badge/JavaFX-21-orange.svg) ![Build](https://img.shields.io/badge/build-Maven-C71A36.svg)

## 🌟 주요 기능

*   **인터랙티브 회로 에디터:** 드래그 앤 드롭 인터페이스를 통해 게이트, 핀을 배치하고 와이어를 연결할 수 있습니다.
*   **실시간 시뮬레이션 엔진:** 백그라운드에서 실행되는 강력한 멀티스레드 시뮬레이션 엔진(기본 ~60Hz)이 UI 차단 없이 신호 상태를 매끄럽게 계산하고 전송합니다.
*   **다양한 핵심 컴포넌트:** 표준 논리 게이트(AND, OR, NOT, NAND, NOR, XOR, XNOR), 입출력 핀(Input/Output pins), 와이어 정리를 위한 조인트(Joint)가 포함되어 있습니다.
*   **동적 속성 패널:** 실행 취소/다시 실행(Undo/Redo)을 지원하며, 색상 변경이나 작동 모드 등 개별 컴포넌트의 속성을 실시간으로 구성할 수 있습니다.
*   **확장 가능한 모딩 시스템:** 외부 JAR 파일을 사용하여 사용자 정의 게이트, 연산 장치, 메모리 모듈 및 디스플레이를 추가할 수 있습니다.
*   **프로젝트 관리:** 회로와 작업 공간을 `.prj` 및 `.lgs` 파일로 저장하고 언제든지 다시 불러올 수 있습니다.
*   **다국어 지원:** 영어와 한국어를 기본으로 지원합니다.

## 📦 프로젝트 구조

이 저장소는 두 개의 주요 영역으로 나뉩니다:

*   **`Logic-Gate-Simulator/`**: 시뮬레이터의 핵심 애플리케이션으로 UI, 시뮬레이션 엔진, 저장/불러오기 로직 및 기본 게이트 구현을 포함합니다.
*   **`Logic-Gate-Mods/`**: 모딩 API의 강력함을 보여주는 공식 모드 모음입니다.
    *   **`Bus-Mod/`**: 8비트 버스 통합기 및 트라이스테이트 버퍼(Tri-state buffer)를 추가합니다.
    *   **`FullAdder-Mod/`**: 전가산기(Full Adder)와 같은 산술 논리 모듈을 추가합니다.
    *   **`RAM-Mod/`**: 256x8비트 RAM(Random Access Memory) 모듈을 도입합니다.
    *   **`Seven-Segment-Mod/`**: 시각적 출력을 위한 7세그먼트 디스플레이와 이진-7세그먼트 디코더를 포함합니다.

## 🚀 시작하기

### 요구 사항

*   **Java Development Kit (JDK) 21** 이상
*   프로젝트 빌드를 위한 **Maven** 3.8 이상

### 프로젝트 빌드

1.  로컬 환경에 저장소를 클론(Clone)합니다.
2.  핵심 시뮬레이터 디렉토리로 이동합니다:
    ```bash
    cd Logic-Gate-Simulator
    ```
3.  Maven을 사용하여 프로젝트를 빌드합니다:
    ```bash
    mvn clean install
    ```

### 시뮬레이터 실행

빌드 후 JavaFX Maven Plugin을 사용하여 시뮬레이터를 직접 실행할 수 있습니다:

```bash
mvn javafx:run
```

또는 생성된 실행 가능한 JAR 파일을 실행하거나 선호하는 IDE에서 `com.logicgate.ui.MainApp` 메인 클래스를 실행할 수 있습니다.

## 🧩 모드(Mod) 제작 가이드

나만의 사용자 정의 컴포넌트를 만들고 싶으신가요? 시뮬레이터는 사용자 정의 로직(`Node`), 시각적 표현(`Symbol`) 및 사용자 구성 가능한 속성(`Property`)을 정의할 수 있는 풍부한 API를 제공합니다.

1. 새로운 Maven 프로젝트를 생성하고 코어 시뮬레이터를 `provided` 의존성으로 추가합니다.
2. `com.logicgate.gates.Node`를 상속받는 `Node` 클래스를 만들고 `@ComponentMeta` 어노테이션을 추가합니다.
3. `com.logicgate.editor.rendering.symbol.AbstractGateSymbol`을 상속받는 `Symbol` 클래스를 만듭니다.
4. 모드를 `.jar` 파일로 패키징합니다.
5. 시뮬레이터에서 **모드 관리자(Mod Manager)**를 사용하여 JAR 파일을 불러옵니다. 새로운 컴포넌트가 부품 트리에 즉시 나타납니다!

자세한 지침은 `Logic-Gate-Mods` 디렉토리에 있는 [모딩 가이드](Logic-Gate-Mods/MODDING.md)를 참조하세요.

## 🛠️ 기술 스택

*   **언어:** Java 21
*   **UI 프레임워크:** JavaFX 21
*   **데이터 직렬화:** Google Gson (JSON)
*   **도움말 및 문서 렌더링:** CommonMark (JavaFX WebEngine에서의 마크다운 렌더링)
*   **아이콘:** Ikonli (Material Design)
*   **빌드 도구:** Apache Maven
