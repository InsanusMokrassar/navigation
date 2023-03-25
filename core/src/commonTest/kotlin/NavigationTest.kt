import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel

abstract class NavigationTest {
    init {
        KSLog.default = KSLog("test", LogLevel.INFO)
    }
}
