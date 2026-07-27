package com.github.noamm9.init

import com.github.noamm9.NoammAddons
import com.github.noamm9.commands.CommandManager
import com.github.noamm9.features.FeatureManager
import com.github.noamm9.init.types.ISelfInit
import io.github.classgraph.ClassGraph
import io.github.classgraph.ScanResult

class ClassGraphInitializer {
    private val scan = ClassGraph().enableAllInfo().apply {
        acceptPackages(NoammAddons::class.java.`package`.name)
        overrideClassLoaders(Thread.currentThread().contextClassLoader)
    }.scan()

    fun initAll() = scan.use { scan ->
        registerSelfInits(scan)
        CommandManager.registerAll(scan)
        FeatureManager.registerFeatures(scan)
    }

    private fun registerSelfInits(result: ScanResult) {
        result.getClassesImplementing(ISelfInit::class.java).forEach { ci ->
            val obj = ci.loadClass().getDeclaredField("INSTANCE").get(null) as? ISelfInit
            obj?.init()
        }
    }
}