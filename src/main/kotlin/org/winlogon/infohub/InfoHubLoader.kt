package org.winlogon.infohub

import io.papermc.paper.plugin.loader.PluginClasspathBuilder
import io.papermc.paper.plugin.loader.PluginLoader
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.repository.RemoteRepository

class ScalaPluginLoader : PluginLoader {
    override fun classloader(classpathBuilder: PluginClasspathBuilder) {
        val resolver = MavenLibraryResolver()

        resolver.addRepository(
            RemoteRepository.Builder(
                "central", 
                "default", 
                "https://repo.maven.apache.org/maven2/"
            ).build()
        )

        resolver.addDependency(
            Dependency(
                DefaultArtifact("com.github.oshi:oshi-core-java11:6.8.0"),
                null
            )
        )

        classpathBuilder.addLibrary(resolver)
    }
}

