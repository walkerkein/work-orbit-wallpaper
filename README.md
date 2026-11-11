# Work Orbit 라이브 배경화면

Work Orbit 현황판의 항성계를 안드로이드 홈·잠금화면 배경으로 실시간 표시하는 앱입니다.

## 설치

1. 휴대폰에서 [WorkOrbit.apk](https://github.com/walkerkein/work-orbit-wallpaper/releases/download/latest/WorkOrbit.apk)를 내려받습니다.
2. 설치 화면에서 '출처를 알 수 없는 앱 설치'를 한 번 허용합니다.
3. 앱을 열고 **배경화면으로 설정**을 누릅니다.

## 표시 내용

- 프로젝트 행성과 남은 할 일 개수, 다음 마감과 D-day
- 데이터: `data/orbit.json` (요약만 담은 공개 파일). 앱이 1시간마다 다시 받습니다.

## 빌드

`app/` 아래가 바뀌어 `main`에 올라가면 GitHub Actions가 APK를 만들어 `latest` 릴리스에 올립니다. `data/orbit.json`만 바뀔 때는 빌드하지 않습니다.
