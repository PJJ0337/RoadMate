plugins {
    id("com.android.application") version "8.8.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
}

// 빌드 결과물(수천 개 파일)을 OneDrive 밖(C:\Users\사용자\RoadMateBuild)에 만든다.
// OneDrive 동기화 때문에 생기는 "파일 사용 중" 오류를 막기 위한 설정.
allprojects {
    layout.buildDirectory.set(file("${System.getProperty("user.home")}/RoadMateBuild/${project.name}"))
}
