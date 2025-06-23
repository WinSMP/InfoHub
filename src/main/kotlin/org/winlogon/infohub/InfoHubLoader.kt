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
            "central" to "https://repo.maven.apache.org/maven2/"
        )

        val dependencies = mapOf(
            "com.github.oshi:oshi-core-java11" to "6.8.0"
        )

        repositories.forEach { (name, url) -> 
            resolver.addRepository(
                RemoteRepository.Builder(
                    name, 
                    "default", 
                    url
                ).build()
            )
        }

        dependencies.forEach { (package, version) -> 
            resolver.addDependency(
                Dependency(
                    DefaultArtifact("$package:$version"),
                    null
                )
            )
        }

        classpathBuilder.addLibrary(resolver)
    }
}
