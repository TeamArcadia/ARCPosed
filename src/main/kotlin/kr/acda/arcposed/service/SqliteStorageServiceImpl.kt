package kr.acda.arcposed.service

import kr.acda.arccore.api.module.ModuleLogger
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.sqlite.SQLiteDataSource
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class SqliteStorageServiceImpl(
    val baseDir: Path,
    private val logger: ModuleLogger,
    private val moduleClassLoader: ClassLoader
) : SqliteStorageService {

    private val open = ConcurrentHashMap<String, SqliteDatabaseImpl>()

    init {
        Files.createDirectories(baseDir)
    }

    /**
     * Exposed/JDBC 의 다이얼렉트·드라이버 탐색(ServiceLoader/Class.forName)이
     * 모듈 클래스로더를 보도록, DB 관련 작업을 이 로더의 thread-context 안에서 실행한다.
     */
    internal fun <T> inModuleContext(action: () -> T): T {
        val prev = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = moduleClassLoader
        try {
            return action()
        } finally {
            Thread.currentThread().contextClassLoader = prev
        }
    }

    private fun fileFor(name: String): Path {
        require(name.isNotBlank()) { "database name must not be blank" }
        require(!name.contains('/') && !name.contains('\\') && !name.contains("..")) {
            "database name must not contain path separators: '$name'"
        }
        val fileName = if (name.endsWith(".db")) name else "$name.db"
        return baseDir.resolve(fileName)
    }

    override fun open(name: String): SqliteDatabase =
        open.computeIfAbsent(name) {
            val path = fileFor(name)
            // DriverManager 대신 명시적 DataSource 사용 — 드라이버 클래스를 직접 참조해
            // 클래스로더 의존적인 DriverManager 탐색 문제를 피한다.
            val dataSource = SQLiteDataSource().apply {
                url = "jdbc:sqlite:${path.toAbsolutePath()}"
            }
            val db = inModuleContext { Database.connect(dataSource) }
            logger.info("Opened Exposed SQLite database '$name' at $path")
            SqliteDatabaseImpl(name, db, this) { open.remove(name) }
        }

    override fun open(name: String, vararg tables: Table): SqliteDatabase {
        val handle = open(name)
        if (tables.isNotEmpty()) handle.createTables(*tables)
        return handle
    }

    override fun get(name: String): SqliteDatabase? = open[name]

    override fun isOpen(name: String): Boolean = open.containsKey(name)

    override fun close(name: String) {
        open.remove(name)?.closeInternal()
    }

    override fun closeAll() {
        open.keys.toList().forEach { open.remove(it)?.closeInternal() }
    }

    override fun delete(name: String): Boolean {
        close(name)
        val deleted = Files.deleteIfExists(fileFor(name))
        if (deleted) logger.info("Deleted SQLite database file '$name'.")
        return deleted
    }

    override fun openAsync(name: String, vararg tables: Table): CompletableFuture<SqliteDatabase> =
        CompletableFuture.supplyAsync { open(name, *tables) }
}

internal class SqliteDatabaseImpl(
    override val name: String,
    private val db: Database,
    private val service: SqliteStorageServiceImpl,
    private val onClosed: () -> Unit
) : SqliteDatabase {

    override fun createTables(vararg tables: Table) {
        transaction { SchemaUtils.create(*tables) }
    }

    override fun dropTables(vararg tables: Table) {
        transaction { SchemaUtils.drop(*tables) }
    }

    override fun <R> transaction(block: JdbcTransaction.() -> R): R =
        service.inModuleContext {
            // db 를 명시적으로 넘겨, 여러 파일을 열어도 올바른 Database 에 바인딩되도록 한다.
            transaction(db) { block() }
        }

    override fun close() {
        closeInternal()
        onClosed()
    }

    internal fun closeInternal() {
        // Exposed/SQLite DataSource 는 별도 풀이 없으면 명시적 종료 자원이 없다.
        // (HikariCP 등을 쓰면 여기서 close. 현재는 no-op 으로 충분.)
    }
}
