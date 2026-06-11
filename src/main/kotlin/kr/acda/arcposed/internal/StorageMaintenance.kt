package kr.acda.arcposed.internal

import cc.arccore.api.di.ArcComponent
import cc.arccore.api.di.ArcSingleton
import cc.arccore.api.di.Inject
import kr.acda.arcposed.service.SqliteStorageService

/**
 * 모듈 내부 객체 그래프에서 서비스를 주입받는 예시 컴포넌트.
 *
 * 서비스가 onEnable() 에서 ServiceRegistry 에 등록되므로(경로 6),
 * @ArcComponent 클래스가 생성자 주입으로 받을 수 있다. 생성자가 1개라 @Inject 는 생략 가능.
 */
@ArcComponent
@ArcSingleton
class StorageMaintenance @Inject constructor(
    private val storage: SqliteStorageService
) {
    fun vacuum(database: String) {
        storage.open(database).transaction {
            // VACUUM 은 DSL 로 표현 불가능한 SQLite 전용 명령이라 raw SQL 로 실행한다.
            // 일반 CRUD 는 Exposed 타입 안전 DSL 로 처리할 것.
            exec("VACUUM")
        }
    }
}
