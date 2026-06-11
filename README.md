# ARCPosed

ARCCore 프레임워크 기반 Minecraft Paper 모듈. **Exposed(ORM) + SQLite** 를 다른 모듈이
타입 안전하게 쓸 수 있도록 서비스로 제공한다. SQL 문자열 대신 코틀린 DSL 로 테이블·쿼리를
다루므로 컬럼 오타·타입 오류가 컴파일 단계에서 잡힌다.

- 리포: `TeamArcadia/ARCPosed`
- 모듈 id: `arcposed`
- 패키지: `kr.acda.arcposed`
- exports: `kr.acda.arcposed.service`
- Exposed `1.3.0` (v1 네임스페이스), sqlite-jdbc `3.49.1.0`

## 디렉터리 구조

```
ARCPosed/
├── README.md
└── src/main/kotlin/kr/arcadia/arcposed/
    ├── ArcposedModule.kt
    ├── service/SqliteStorageService.kt
    ├── service/SqliteStorageServiceImpl.kt
    └── internal/StorageMaintenance.kt
```

## 의존성

sqlite-jdbc 와 Exposed 는 `@ModuleSpec(libraries = ...)` 에 선언되어 호스트가 런타임에
Maven Central 에서 전이 의존성까지 자동 다운로드한다. `implementation` 으로 넣어 셰이딩하지 말 것.
IDE 자동완성·컴파일 체크용으로만 `compileOnly` 로 추가한다.

```kotlin
dependencies {
    // arc-core / arc-ksp 는 호스트 제공 → compileOnly / ksp 유지
    compileOnly("org.jetbrains.exposed:exposed-core:1.3.0")
    compileOnly("org.jetbrains.exposed:exposed-jdbc:1.3.0")
    compileOnly("org.jetbrains.exposed:exposed-dao:1.3.0")
    compileOnly("org.xerial:sqlite-jdbc:3.49.1.0")
}
```

## 제공 기능

- 데이터 파일 생성/로드 — `open(name)`, `open(name, vararg tables)`
- 데이터 저장/탐색/삭제(CRUD) — `transaction { }` 안에서 Exposed DSL/DAO
- 스키마 보장/삭제 — `createTables(...)`, `dropTables(...)`
- 파일 삭제 — `delete(name)`
- 비동기 열기 — `openAsync(name, vararg tables)`
- 타입 안전 DB 클래스 — 테이블을 `Table`/`IntIdTable` 로 선언

언로드 시 열린 핸들은 `cleanupScope` 로 자동 정리된다.

## 소비 모듈 사용 예시

테이블을 코드로 선언 → 컬럼 타입이 곧 스키마. SQL 한 줄 없이 CRUD.

```kotlin
package kr.acda.economy

import cc.arccore.api.module.BaseModule
import cc.arccore.api.module.ModuleSpec
import cc.arccore.runtime.context.RuntimeModuleContext
import kr.acda.arcposed.service.SqliteStorageService
import org.jetbrains.exposed.v1.core.*                  // eq 등 top-level 연산자 (1.0 권장)
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

object Balances : IntIdTable() {
    val uuid = varchar("uuid", 36).uniqueIndex()
    val amount = double("amount").default(0.0)
}

@ModuleSpec(
    id = "economy", name = "Economy", version = "1.0.0",
    dependencies = ["arcposed:>=1.0.0"]
)
class EconomyModule : BaseModule() {
    override fun onEnable() {
        val ctx = context as RuntimeModuleContext
        val storage = ctx.services.require(SqliteStorageService::class)

        val db = storage.open("economy", Balances)   // 파일 열기 + 스키마 보장

        db.transaction {
            Balances.insert {
                it[uuid] = "abc-123"
                it[amount] = 100.0
            }
        }

        val bal = db.transaction {
            Balances.selectAll()
                .where { Balances.uuid eq "abc-123" }
                .firstOrNull()
                ?.get(Balances.amount)
        }

        db.transaction {
            Balances.update({ Balances.uuid eq "abc-123" }) {
                it[amount] = 250.0
            }
        }
    }
}
```

## 검증 메모 (Exposed 1.0+)

- Exposed 1.0 부터 패키지가 `org.jetbrains.exposed.v1.*` 로 변경됨.
- `Table` / `Transaction` / DSL 표현식·연산자(`eq` 등) → `v1.core`
- 실행 함수(`Database.connect`, `transaction`, `insert`, `selectAll`, `update`, **`SchemaUtils`**) → `v1.jdbc`
- 특히 `SchemaUtils` 는 1.0 에서 core 가 아니라 **jdbc** 로 이동됨(R2DBC 분리 때문).
- `SqlExpressionBuilder` 는 deprecated → `eq` 등은 top-level 함수, `import org.jetbrains.exposed.v1.core.*` 권장.
- 클래스로더: JDBC 드라이버/다이얼렉트 탐색이 모듈 로더를 보도록 `SQLiteDataSource` +
  thread-context 래핑을 적용했다.
