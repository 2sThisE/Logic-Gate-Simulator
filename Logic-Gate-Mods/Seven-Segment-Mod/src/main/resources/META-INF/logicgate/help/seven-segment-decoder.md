# 7-Segment Decoder

4비트 입력을 받아 7-segment의 `a~g` 출력 패턴으로 변환합니다.

## 핀

| 핀 | 방향 | 의미 |
| --- | --- | --- |
| D0~D3 | 입력 | 4비트 값. D0이 LSB입니다. |
| a~g | 출력 | 표시할 세그먼트 상태 |

## 결과

입력값 `0x0`부터 `0xF`까지 숫자/16진수 문자 모양으로 변환합니다.

| 입력 | 출력 세그먼트 |
| --- | --- |
| 0 | a b c d e f |
| 1 | b c |
| 2 | a b d e g |
| 3 | a b c d g |
| 4 | b c f g |
| 5 | a c d f g |
| 6 | a c d e f g |
| 7 | a b c |
| 8 | a b c d e f g |
| 9 | a b c d f g |
| A | a b c e f g |
| b | c d e f g |
| C | a d e f |
| d | b c d e g |
| E | a d e f g |
| F | a e f g |
