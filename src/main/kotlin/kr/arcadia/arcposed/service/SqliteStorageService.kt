package kr.arcadia.arcposed.service

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import java.util.concurrent.CompletableFuture

/**
 * 다른 모듈이 의존성으로 가져다 쓰는 공개 서비스 인터페이스.
 *
 * 파일 생명주기(생성/로드/삭제/닫기)는 이 서비스가 담당하고,
 * 실제 CRUD 는 Exposed 의 타입 안전 DSL/DAO 로 [SqliteDatabase.transaction] 안에서 수행한다.
 */
interface SqliteStorageService {

    /**
     * 이름에 해당하는 SQLite 파일을 생성(없으면)하고 Exposed Database 핸들을 연다.
     * 이미 열려 있으면 기존 핸들을 반환한다.
     */
    fun open(name: String): SqliteDatabase

    /** 핸들을 열면서 주어진 테이블들의 스키마를 보장한다(없으면 CREATE, 컬럼 추가분 반영). */
    fun open(name: String, vararg tables: Table): SqliteDatabase

    fun get(name: String): SqliteDatabase?
    fun isOpen(name: String): Boolean
    fun close(name: String)
    fun closeAll()

    /** 파일을 삭제한다. 열려 있으면 먼저 닫는다. */
    fun delete(name: String): Boolean

    /** 비동기 open (+ 선택적 스키마 보장). 파일 I/O 를 메인 스레드 밖에서 수행. */
    fun openAsync(name: String, vararg tables: Table): CompletableFuture<SqliteDatabase>
}

/**
 * 단일 SQLite 파일에 대한 Exposed Database 핸들.
 *
 * 모든 읽기/쓰기는 [transaction] 블록 안에서 Exposed DSL/DAO 로 수행한다.
 * 블록은 모듈 클래스로더의 thread-context 에서 실행되며, 트랜잭션 커밋/롤백을 자동 처리한다.
 */
interface SqliteDatabase : AutoCloseable {

    val name: String

    /** 주어진 테이블들의 스키마를 보장한다(SchemaUtils.create, 누락 컬럼 반영). */
    fun createTables(vararg tables: Table)

    /** 주어진 테이블들을 DROP 한다. */
    fun dropTables(vararg tables: Table)

    /**
     * Exposed 트랜잭션을 연다. 블록 안에서 insert/select/update/delete DSL 또는 DAO 를 사용한다.
     * 예외가 나면 자동 롤백, 정상 종료 시 커밋.
     */
    fun <R> transaction(block: JdbcTransaction.() -> R): R

    /** 핸들을 닫는다. */
    override fun close()
}
