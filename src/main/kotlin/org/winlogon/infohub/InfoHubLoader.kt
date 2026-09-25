package org.winlogon.infohub

import io.papermc.paper.plugin.loader.PluginClasspathBuilder
import io.papermc.paper.plugin.loader.PluginLoader
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver

import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.repository.RemoteRepository

class InfoHubLoader : PluginLoader {
    override fun classloader(classpathBuilder: PluginClasspathBuilder) {
        val resolver = MavenLibraryResolver()

        val repositories = mapOf(
            "central" to MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR,
            "winlogon-libs" to "https://maven.winlogon.org/releases/",
        )

        val dependencies = mapOf(
            "com.github.oshi:oshi-core" to "7.6.1",

            "com.zaxxer:HikariCP" to "7.1.0",
            "org.postgresql:postgresql" to "42.7.13",
            "com.mysql:mysql-connector-j" to "26.7.0",
            "io.lettuce:lettuce-core" to "7.8.0.RELEASE",

            "org.winlogon:asynccraftr" to "0.2.0",
        )

        repositories.forEach { (name, url) ->
            resolver.addRepository(RemoteRepository.Builder(name, "default", url).build())
        }

        dependencies.forEach { (dependencyPackage, version) ->
            resolver.addDependency(
                Dependency(
                    DefaultArtifact("$dependencyPackage:$version"),
                    null
                )
            )
        }

        classpathBuilder.addLibrary(resolver)
    }
}
