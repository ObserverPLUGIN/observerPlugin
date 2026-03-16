# Minecraft 관리자 감시 플러그인 설계 가이드

## 목표 기능

이 프로젝트의 목표는 다음 기능을 가진 Minecraft Paper 플러그인을 만드는
것이다.

-   플레이어 위치 실시간 추적
-   관리자 전용 감시 모드
-   블럭 설치 / 파괴 실시간 로그
-   웹 대시보드 연동

------------------------------------------------------------------------

# 전체 아키텍처

    Minecraft Server
            │
            │ Bukkit Events
            ▼
    Plugin Core
     ├─ TrackerService
     ├─ LogService
     ├─ ObserverService
     └─ WebBridge
            │
            ▼
    Database (SQLite / Redis)
            │
            ▼
    Web Dashboard

------------------------------------------------------------------------

# 개발 환경

권장 환경

-   Java 21
-   Paper Server 1.21+
-   IntelliJ IDEA
-   Gradle

------------------------------------------------------------------------

# 프로젝트 구조

    observer-plugin
     ├─ build.gradle
     ├─ settings.gradle
     └─ src
         └─ main
             ├─ java
             │   └─ com.yangtheory.observer
             │        ├─ ObserverPlugin.java
             │        ├─ listener
             │        │    ├─ PlayerMoveListener.java
             │        │    ├─ BlockPlaceListener.java
             │        │    └─ BlockBreakListener.java
             │        ├─ tracker
             │        │    └─ PlayerTrackerService.java
             │        ├─ observer
             │        │    └─ ObserverManager.java
             │        └─ web
             │             └─ WebBridge.java
             └─ resources
                  └─ plugin.yml

------------------------------------------------------------------------

# 1단계 플레이어 위치 추적

사용 이벤트

    PlayerMoveEvent

예제 코드

``` java
public class PlayerMoveListener implements Listener {

    @EventHandler
    public void onMove(PlayerMoveEvent event){

        Player player = event.getPlayer();
        Location loc = player.getLocation();

        Bukkit.getLogger().info(
            player.getName() + " -> " +
            loc.getBlockX() + "," +
            loc.getBlockY() + "," +
            loc.getBlockZ()
        );
    }
}
```

주의

PlayerMoveEvent 는 매우 자주 호출되므로 반드시 제한이 필요하다.

    1초에 1번만 기록

------------------------------------------------------------------------

# 2단계 관리자 감시 모드

관리자가 특정 플레이어를 감시할 수 있는 기능

명령어

    /observe <player>

구조

    ObserverManager

    Map<AdminUUID , TargetUUID>

예

    관리자 -> 감시 대상 플레이어

플레이어 이동 이벤트 발생 시

    감시중이면 관리자에게 좌표 전송

------------------------------------------------------------------------

# 3단계 블럭 로그

사용 이벤트

    BlockPlaceEvent
    BlockBreakEvent

예제

``` java
@EventHandler
public void onPlace(BlockPlaceEvent event){

    Player player = event.getPlayer();
    Block block = event.getBlock();

    Bukkit.getLogger().info(
        player.getName() + " placed " + block.getType()
    );
}
```

저장 데이터 구조

    player
    world
    x
    y
    z
    blockType
    timestamp
    action

------------------------------------------------------------------------

# 4단계 웹 대시보드

플러그인이 서버로 로그를 전송한다.

구조

    Minecraft Plugin
          │
          │ HTTP / WebSocket
          ▼
    Backend API
          │
          ▼
    Web Dashboard

예시 로그 전송

    POST /logs

    {
     player:"yang",
     action:"place",
     block:"STONE",
     x:10,
     y:64,
     z:22
    }

Java HTTP 라이브러리

    OkHttp

------------------------------------------------------------------------

# 최종 목표

완성되면 다음 기능을 가진 관리자 시스템이 된다.

-   실시간 플레이어 위치 추적
-   블럭 설치 / 파괴 로그 조회
-   관리자 감시 모드
-   웹 기반 서버 모니터링

이 구조는

    Dynmap + CoreProtect + AdminPanel

을 합친 형태의 시스템이다.

------------------------------------------------------------------------

# 추천 다음 단계

1.  PlayerMoveEvent 기반 위치 추적 구현
2.  ObserverManager 구현
3.  블럭 로그 시스템 구현
4.  웹 API 연동
