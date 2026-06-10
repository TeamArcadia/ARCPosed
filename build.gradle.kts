plugins {
	kotlin("jvm") version "2.3.21"
	id("com.google.devtools.ksp") version "2.3.7"
}

group = "kr.arcadia.arcposed"
version = "1.0.0"

repositories {
	mavenCentral()
	maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
	testImplementation(kotlin("test"))
	compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
	compileOnly(files("libs/arc-core-1.0.0.jar"))
	ksp(files("libs/arc-ksp-1.0.0.jar"))

	// sqlite-jdbc / Exposed 는 @ModuleSpec(libraries=...) 로 선언되어 호스트가 런타임에
	// Maven Central 에서 전이 의존성까지 자동 다운로드한다. implementation 으로 셰이딩하지 말 것.
	// 아래는 IDE 자동완성·컴파일 체크용 compileOnly.
	compileOnly("org.jetbrains.exposed:exposed-core:1.3.0")
	compileOnly("org.jetbrains.exposed:exposed-jdbc:1.3.0")
	compileOnly("org.jetbrains.exposed:exposed-dao:1.3.0")
	compileOnly("org.xerial:sqlite-jdbc:3.49.1.0")
}

kotlin {
	jvmToolchain(21)
}

tasks.test {
	useJUnitPlatform()
}