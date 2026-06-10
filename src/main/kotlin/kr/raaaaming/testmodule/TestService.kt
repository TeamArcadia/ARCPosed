package kr.raaaaming.testmodule

/**
 * 모듈이 제공하는 서비스 예시.
 *
 * [TestModule.onEnable] 에서 ServiceRegistry 에 등록되며,
 * `@ARCListener` / `@CommandSpec` 클래스의 생성자 파라미터로 자동 주입된다.
 */
class TestService {
    private var greetCount = 0

    fun greet(name: String): String {
        greetCount++
        return "§e[TestModule] Hello, $name! (total greets: $greetCount)"
    }

    fun getGreetCount(): Int = greetCount
}
