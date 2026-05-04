# InputPin / Switch

회로에 사용자가 지정한 LOW/HIGH 값을 공급하는 입력 스위치입니다.

## 핀

| 핀 | 방향 | 의미 |
| --- | --- | --- |
| Q | 출력 | 현재 스위치 상태 |

## 동작 방식

- `Toggle`: 클릭할 때마다 LOW/HIGH가 전환됩니다.
- `Momentary`: 누르고 있는 동안만 HIGH가 되고, 놓으면 LOW로 돌아갑니다.

## 결과

`compute()`는 내부 상태 `in`을 그대로 `out`으로 복사합니다.
