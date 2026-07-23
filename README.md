# Logic Gate Simulator

[English](README.en.md) | 한국어

Logic Gate Simulator는 Java와 JavaFX로 만든 디지털 논리 회로 시뮬레이터입니다. 논리 게이트를 배치하고 전선을 연결해 회로를 설계할 수 있으며, 실시간으로 신호 흐름을 확인할 수 있습니다.

![Java](https://img.shields.io/badge/Java-21-blue.svg)
![JavaFX](https://img.shields.io/badge/JavaFX-21-orange.svg)
![Build](https://img.shields.io/badge/build-Maven-C71A36.svg)
![License](https://img.shields.io/badge/license-Apache--2.0-green.svg)

## 주요 기능

- **인터랙티브 회로 에디터**: 논리 게이트, 입출력 핀, 조인트를 배치하고 전선을 연결해 회로를 구성할 수 있습니다.
- **실시간 시뮬레이션**: 회로의 신호 상태를 실시간으로 계산하고 활성 전선을 시각적으로 표시합니다.
- **기본 논리 컴포넌트**: AND, OR, NOT, NAND, NOR, XOR, XNOR, Input Pin, Output Pin, Joint를 제공합니다.
- **프로젝트 저장 및 불러오기**: 회로와 프로젝트 설정을 저장하고 다시 열 수 있습니다.
- **모드 지원**: 외부 JAR 모드를 불러와 사용자 정의 컴포넌트를 추가할 수 있습니다.
- **다국어 UI**: 한국어와 영어를 지원합니다.
- **도움말 시스템**: 기본 사용법과 게이트별 문서를 앱 안에서 확인할 수 있습니다.

## 다운로드

최신 실행 파일은 [GitHub Releases](https://github.com/2sThisE/Logic-Gate-Simulator/releases)에서 받을 수 있습니다.

- Windows: `LogicGateSimulator-x.y.z.exe`
- macOS: `LogicGateSimulator-x.y.z.dmg`
- Linux: `logicgatesimulator_x.y.z_amd64.deb`

### Windows 설치 안내

현재 배포 파일은 코드 서명이 적용되어 있지 않습니다. Windows SmartScreen, Microsoft Defender, Smart App Control에서 경고가 표시될 수 있습니다. 설치가 차단되는 경우 Windows 보안 설정에서 Smart App Control을 끄기로 변경해야 정상 설치가 가능할 수 있습니다.

## 프로젝트 구조

- `Logic-Gate-Mod-API/`: 본체와 모드가 공유하는 JavaFX 독립 Java 21 모드 API입니다.
- `Logic-Gate-Simulator/`: 시뮬레이터 본체입니다. JavaFX UI, 회로 시뮬레이션, 저장/불러오기, 기본 컴포넌트를 포함합니다.
- `Logic-Gate-Mods/`: 예제 및 공식 모드 모음입니다.
  - `Bus-Mod/`: 8비트 버스 통합기와 트라이스테이트 버퍼를 제공합니다.
  - `FullAdder-Mod/`: 전가산기 컴포넌트를 제공합니다.
  - `RAM-Mod/`: 256x8비트 RAM 컴포넌트를 제공합니다.
  - `Seven-Segment-Mod/`: 7세그먼트 디스플레이와 디코더를 제공합니다.

## 빌드 및 실행

### 요구 사항

- JDK 21 이상
- Maven 3.8 이상

### 빌드

```bash
mvn clean package -pl Logic-Gate-Simulator -am
```

### 실행

```bash
mvn install -pl Logic-Gate-Mod-API -DskipTests
mvn -f Logic-Gate-Simulator/pom.xml javafx:run
```

또는 IDE에서 `com.logicgate.ui.MainApp` 클래스를 실행할 수 있습니다.

## 모드 제작

모드는 시뮬레이터에 새로운 컴포넌트를 추가하는 확장 기능입니다. 사용자 정의 로직, 심볼, 속성을 구현한 뒤 JAR 파일로 패키징해 앱의 모드 관리자에서 불러올 수 있습니다.

기본 흐름:

1. Maven 프로젝트를 생성합니다.
2. `com.logicgate.api.component.Node`를 상속한 컴포넌트 클래스를 만듭니다.
3. `@ComponentMeta`로 컴포넌트 정보를 정의합니다.
4. 필요하면 `AbstractGateSymbol` 기반 심볼을 추가합니다.
5. JAR로 빌드한 뒤 앱에서 불러옵니다.

자세한 내용은 [모딩 가이드](Logic-Gate-Mods/MODDING.md)를 참고하세요.

### 모드 사용 시 주의

모드는 외부 코드를 앱에 불러오는 기능입니다. 신뢰할 수 없는 모드 파일은 보안 위험이나 오작동을 일으킬 수 있으므로 출처를 확인한 뒤 사용하세요.

## 기술 스택

- Java 21
- JavaFX 21
- Maven
- Gson
- CommonMark
- Ikonli / Material Design Icons

## 라이선스

이 프로젝트는 [Apache License 2.0](LICENSE)에 따라 배포됩니다.

사용된 외부 라이브러리와 리소스의 라이선스 고지는 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)를 확인하세요.
