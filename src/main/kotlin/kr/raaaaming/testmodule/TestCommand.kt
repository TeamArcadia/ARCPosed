package kr.raaaaming.testmodule

import cc.arccore.api.command.ARCCommand
import cc.arccore.api.command.CommandContext
import cc.arccore.api.command.CommandResult
import cc.arccore.api.command.CommandSpec

/**
 * `@CommandSpec` 이 붙은 커맨드. KSP 가 메타데이터를 생성하고 자동 등록한다.
 *
 * 생성자 파라미터([TestService])는 리스너와 동일하게 ServiceRegistry 에서 자동 주입된다.
 */
@CommandSpec(
    name = "test",
    aliases = ["t"],
    description = "A test command",
    usage = "/test",
)
class TestCommand(private val testService: TestService) : ARCCommand {

    override fun execute(context: CommandContext): CommandResult {
        context.sender.sendMessage("Hello from TestModule! (greets so far: ${testService.getGreetCount()})")
        return CommandResult.Success
    }
}
