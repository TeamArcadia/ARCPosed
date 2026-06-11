package kr.acda.arcposed


import kr.acda.arccore.api.module.BaseModule
import kr.acda.arccore.api.module.ModuleSpec
import kr.acda.arccore.runtime.context.RuntimeModuleContext
import kr.acda.arcposed.service.SqliteStorageService
import kr.acda.arcposed.service.SqliteStorageServiceImpl

/**
 * Exposed(ORM) 기반 SQLite 저장소를 다른 모듈에 서비스로 제공하는 모듈.
 *
 * SQL 문자열 대신 Exposed 의 타입 안전 DSL/DAO 로 테이블·쿼리를 다룬다.
 *
 * libraries: 호스트가 로드시 Maven Central 에서 아래 좌표 + 전이 의존성을 자동 다운로드한다.
 *   (Aether 가 transitive 까지 해석하므로 exposed-core 의 의존성도 함께 내려온다)
 *   셰이딩 금지 — build.gradle 에는 넣지 않는다.
 */
@ModuleSpec(
    id = "arcposed",
    name = "ARCPosed",
    version = "1.0.0",
    description = "Exposed-based SQLite storage (type-safe tables & queries) for other modules",
    authors = ["Raaaaming"],
    libraries = [
        "org.xerial:sqlite-jdbc:3.49.1.0",
        "org.jetbrains.exposed:exposed-core:1.3.0",
        "org.jetbrains.exposed:exposed-jdbc:1.3.0",
        "org.jetbrains.exposed:exposed-dao:1.3.0"
    ],
    exports = ["kr.acda.arcposed.service"]
)
class ArcposedModule : BaseModule() {

    private lateinit var service: SqliteStorageServiceImpl

    override fun onEnable() {
        val ctx = context as RuntimeModuleContext

        service = SqliteStorageServiceImpl(
            baseDir = dataFolder.resolve("databases"),
            logger = logger,
            // Exposed/JDBC 의 다이얼렉트·드라이버 탐색이 모듈 클래스로더를 보도록
            // 모든 DB 작업을 이 클래스로더의 thread-context 안에서 실행한다.
            moduleClassLoader = javaClass.classLoader
        )

        ctx.services.register(SqliteStorageService::class, service)

        ctx.cleanupScope.onClose {
            service.closeAll()
        }

        logger.info("ARCPosed SQLite storage service registered (base dir: ${service.baseDir}).")
    }

    override fun onDisable() {
        if (::service.isInitialized) {
            service.closeAll()
        }
    }
}
