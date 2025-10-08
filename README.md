# Pie-Ray
[![Java](https://img.shields.io/badge/Java-21-ED8B00.svg?logo=openjdk)](https://www.azul.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-585DEF.svg?logo=kotlin)](http://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/Gradle-8.13-02303A.svg?logo=gradle)](https://gradle.org)

#### 벡터기반 엑스레이 탐지 플러그인

---

* ### Features
  * 4개의 벡터를 이용한 엑스레이 심층 분석
  * _config_ 를 통한 세부 설정 관리
  * _VersionHandler_ 을 이용한 호환성 자동 관리(현재 미구현 - build.libs를 참고해주세요)

* ### Supported Minecraft Versions
  * 1.21
  * 1.20
  * 1.19
  * 1.18
  * 1.17
  * 1.16.5

마인크래프트 서버에는 빌런이 많습니다.  
그들은 핵 클라이언트나 오토클릭커등 외부 프로그램을 가지고 오는 경우도 있지만,  
그중에서도 특히 구하기도 쉽고 쓰기도 쉬운 엑스레이를 많이 선호하는 편입니다.  

Paper에는 이것을 막는 기본적인 엑스레이 방지 플러그인이 존재하지만,  
가상 블록을 렌더링시키는 방식이다 보니 서버에 주는 부담이 크며 클라이언트 기반 엑스레이는 또 잡아내지 못한다는 단점이 있습니다.  
물론 다른 사설 엑스레이 방지 플러그인도 존재는 하지만, 영 마음에 들지 않습니다.  
이것을 해결하기 위해 좀 더 새로운 방식으로 엑스레이를 잡아내는 플러그인이 필요해졌습니다.  

_PieRay_ 는 최대 4개의 벡터를 활용한 연산으로 보다 깔끔한 엑스레이 탐지 기능을 제공합니다.  
_PieRay_ 의 탐지 로직은 다음과 같이 동작합니다.

`Diagram`
```YAML
    실시간 추적 및 의심도 계산:
        J[주기적 실행 (5초마다)] --> K[모든 온라인 플레이어 확인];
        K --> L{저장된 광물 근처에 있는가?};
        L -- 예 --> M[가장 가까운 광물 추적 시작];
        M --> N[벡터 분석 (시야/이동/원뿔)];
        N --> O[의심도 점수 계산 및 누적];
        L -- 아니오 --> P[추적 종료];

        O --> Q{의심도 임계치 초과?};
        Q -- 예 --> R[패널티 적용 (채굴 피로)];
        R --> S[관리자에게 경고 메시지 전송];
        Q -- 아니오 --> T[의심도 유지 및 지속 추적];

        M --> U{광물로부터 멀어지는가? (Pass 거리 초과)};
        U -- 예 --> V[의심도 감소 및 추적 초기화];
    end

    채굴 시점 분석:
        W[플레이어, 광물 채굴] --> X{감시 대상 광물인가?};
        X -- 예 --> Y[채굴 시점 벡터 분석 수행];
        Y --> Z["- 시야 벡터 (3x3 그리드/블록 면 분석)  
        - 이동 벡터 (이동 경로 일치도)
        - 원뿔 벡터 (진입점-목표 경로 일치도)
        - 시야 확보 보너스 (장애물 여부)"];
        Z --> AA[채굴 분석 기반 의심도 추가];
        AA --> Q;
        X -- 아니오 --> BB[분석 종료];
    end
```

이러한 형태로 이루어지는 탐지 로직에 걸릴 시 점수가 누적되며, 설정치만큼 도달하면 경고가 부괴됩니다.  
알고리즘 우회 방지를 위해 기술된 벡터 외에도 한 가지가 더 포함되어 있습니다.  

`Config`
```YAML
# PieRay 플러그인 설정

areart: true # 의심 대상 경고 메시지 활성화 여부
areartMessage: true # 개별 플레이어 경고 메시지 활성화 여부

blocks:
  - DIAMOND_ORE
  - GOLD_ORE
  - IRON_ORE
  - EMERALD_ORE
  - ANCIENT_DEBRIS # 탐지할 광물 블록 목록

detection:
  range: 10.0 # 플레이어 주변에서 대상 블록을 탐지할 최대 거리
  passDistance: 5.0 # 플레이어가 대상 블록을 지나쳤다고 판단하는 거리. 이 거리 이상 멀어지면 의심도 감소.

suspicion:
  threshold: 100.0 # 플레이어 처벌을 위한 의심도 임계치
  decreaseRate: 0.5 # 대상 블록을 지나쳤을 때 의심도 감소율 (현재 의심도 * 감소율)

vector:
  sight:
    weight: 10.0 # 시야 벡터 분석의 의심도 가중치
    threshold: 60.0 # 시야 벡터 의심도 점수 부여를 위한 최소 임계치 (백분율)
    gridMode: "dynamic" # 시야 그리드 모드 (static 또는 dynamic)
    gridSize: 0.6 # 시야 그리드 크기
    centerWeight: 1.5 # 시야 그리드 중앙 지점의 가중치
  movement:
    weight: 8.0 # 이동 벡터 분석의 의심도 가중치
    threshold: 70.0 # 이동 벡터 의심도 점수 부여를 위한 최소 임계치 (백분율)
  cone:
    weight: 10.0 # 원뿔 벡터 분석의 의심도 가중치
    angle: 45.0 # 원뿔 벡터 분석에 사용되는 각도 (도)
    threshold: 50.0 # 원뿔 벡터 의심도 점수 부여를 위한 최소 임계치 (백분율)

filtering:
  maxExposedSides: 1 # 블록이 노출되었다고 판단하는 최소 면의 개수. 이 값 이상 노출되면 필터링되어 제거됨.
  enableWaterCheck: true # 물 블록에 인접한 경우 노출로 간주할지 여부
  enableLavaCheck: true # 용암 블록에 인접한 경우 노출로 간주할지 여부

advanced:
  enableWiggleDetection: true # 와리가리 움직임 탐지 활성화 여부
  wiggleAngleThreshold: 45.0 # 와리가리 탐지 시 각도 변화 임계치 (도)
  wiggleDistanceThreshold: 3.0 # 와리가리 탐지 시 이동 거리 임계치
  wiggleTimeThreshold: 1000 # 와리가리 탐지 시 시간 임계치 (밀리초)
  wiggleCountThreshold: 3 # 와리가리 탐지 시 반복 횟수 임계치
  wiggleSuspicionBonus: 5.0 # 와리가리 탐지 시 추가 의심도 점수
  enableLineOfSight: true # 시야선 분석 활성화 여부
  lineOfSightMaxDistance: 50 # 시야선 분석 최대 거리
  enableBlockFaceAnalysis: false # 블록 면 분석 활성화 여부 (시야 벡터의 세부 분석)
  enableAdjacentRemoval: true # 블록 채굴 시 인접 블록 제거 활성화 여부
```

---
* ### UPDATE
  * 엑세스 타입 변경: 리플렉션 -> getter
  * 사운드가 추가되었습니다.
  * 오탐지 의심 로직이 제거되었습니다.
  * _config_ 의 기본값이 수정되었습니다.

* ### NOTE
  * 설정은 되도록 기본값을 사용하시길 권장합니다.
  * 이전 스캔에서 존재하지 않았던 블록은 다시 스캔을 돌려도 무시됩니다.

### Contributors
* **[MoreGrayner](https://github.com/moregrayner)**
    * 알고리즘 설계 및 제작
    
