package kr.raaaaming.testmodule

import cc.arccore.api.module.BaseModule
import cc.arccore.api.module.ModuleSpec
import cc.arccore.runtime.context.RuntimeModuleContext

/**
 * 모듈 진입점. `@ModuleSpec` 로부터 `META-INF/arc-module.json` 이 자동 생성된다.
 *
 * `BaseModule` 을 상속하면 `context`, `logger`, `dataFolder` 를 바로 사용할 수 있고,
 * 이 셋은 `onLoad()` 이후부터 접근 가능하다.
 */
@ModuleSpec(
    id = "test-module",
    name = "Test Module",
    version = "1.0.0",
    description = "An example ARCCore module",
    authors = ["raaaaming"],
)
class TestModule : BaseModule() {

    override fun onEnable() {
        // RuntimeModuleContext 로 캐스팅하면 services / scheduler / commands / listeners 에 접근할 수 있다.
        val ctx = context as RuntimeModuleContext

        // 서비스 등록 — @ARCListener / @CommandSpec 클래스의 생성자에 자동 주입된다.
        // onEnable() 은 KSP 가 생성한 부트스트랩(커맨드·리스너 등록)보다 먼저 실행되므로,
        // 여기서 등록한 서비스는 주입 시점에 이미 사용 가능하다.
        val service = TestService()
        ctx.services.register(TestService::class, service)
        logger.info("TestService registered")

        // 비동기 반복 태스크 (100틱 = 5초마다). 등록한 태스크는 모듈 언로드 시 자동 취소된다.
        ctx.scheduler.runAsyncRepeating(delayTicks = 100L, periodTicks = 100L) {
            logger.info("[AsyncTask] TestService has been called ${service.getGreetCount()} time(s) so far")
        }

        logger.info("Test module enabled!")
    }

    override fun onDisable() {
        logger.info("Test module disabled")
    }
}
