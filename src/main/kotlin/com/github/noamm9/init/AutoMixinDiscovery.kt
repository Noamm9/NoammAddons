package com.github.noamm9.init

import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin
import org.spongepowered.asm.mixin.extensibility.IMixinInfo
import java.io.File
import java.net.JarURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.*
import java.util.jar.*

class AutoMixinDiscovery: IMixinConfigPlugin {
    private val basePackage = "com.github.noamm9.mixin"
    private val basePath = basePackage.replace('.', '/')
    private var mixins = emptyList<String>()

    override fun onLoad(mixinPackage: String) {
        mixins = discoverMixins()
    }

    private fun discoverMixins(): List<String> {
        if (FabricLoader.getInstance().environmentType != EnvType.CLIENT) return emptyList()
        val result = TreeSet<String>()
        val resources = javaClass.classLoader.getResources(basePath)

        while (resources.hasMoreElements()) {
            val url = resources.nextElement()
            when (url.protocol) {
                "jar" -> collectFromJar(url, result)
                "file" -> collectFromDir(url, result)
            }
        }

        return result.toList()
    }

    private fun collectFromJar(url: URL, result: MutableSet<String>) {
        val connection = url.openConnection() as JarURLConnection
        JarFile(connection.jarFile.name).use { jar ->
            val entries = jar.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name
                if (! name.startsWith(basePath) || ! name.endsWith(".class")) continue
                if (name.contains('$') || name.endsWith("/module-info.class")) continue
                if (name.endsWith("${this.javaClass.simpleName}.class")) continue

                val className = name.removeSuffix(".class").replace('/', '.')
                if (! className.startsWith("$basePackage.")) continue
                result.add(className.substring(basePackage.length + 1))
            }
        }
    }

    private fun collectFromDir(url: URL, result: MutableSet<String>) {
        val rootPath = URLDecoder.decode(url.path, "UTF-8")
        val root = File(rootPath).takeIf { it.exists() } ?: return

        root.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".class") }
            .forEach { file ->
                val rel = file.relativeTo(root).invariantSeparatorsPath
                if (! rel.endsWith(".class")) return@forEach
                if (rel.contains('$') || rel.endsWith("/module-info.class")) return@forEach
                if (rel.endsWith("${this.javaClass.simpleName}.class")) return@forEach

                val className = (basePath + "/" + rel.removeSuffix(".class")).replace('/', '.')
                if (! className.startsWith("$basePackage.")) return@forEach
                result.add(className.substring(basePackage.length + 1))
            }
    }

    override fun getMixins() = mixins.toMutableList()
    override fun getRefMapperConfig() = null
    override fun shouldApplyMixin(targetClassName: String?, mixinClassName: String?) = true
    override fun acceptTargets(myTargets: MutableSet<String>?, otherTargets: MutableSet<String>?) {}
    override fun preApply(targetClassName: String?, targetClass: ClassNode?, mixinClassName: String?, info: IMixinInfo?) {}
    override fun postApply(targetClassName: String?, targetClass: ClassNode?, mixinClassName: String?, info: IMixinInfo?) {}
}