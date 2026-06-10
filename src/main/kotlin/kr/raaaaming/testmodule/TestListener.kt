package kr.raaaaming.testmodule

import cc.arccore.api.annotation.ARCListener
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

/**
 * `@ARCListener` 가 붙은 Bukkit 리스너.
 *
 * 생성자 파라미터([TestService])는 ServiceRegistry 에서 자동 주입되며,
 * KSP 가 생성한 부트스트랩이 `context.listeners.register(...)` 로 등록한다.
 * (non-nullable → `require()`, nullable → `get()` 로 주입된다.)
 */
@ARCListener
class TestListener(private val testService: TestService) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        event.player.sendMessage("§a[TestModule] Welcome, ${event.player.name}!")
        event.player.sendMessage(testService.greet(event.player.name))
    }
}
