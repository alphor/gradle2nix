package org.nixos.gradle2nix

import java.io.File
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection

fun connect(config: Config): ProjectConnection =
    GradleConnector.newConnector()
        .apply {
            when(val cfg = config.gradleProvider) {
                is GradleProvider.Wrapper -> useBuildDistribution()
                is GradleProvider.Version -> useGradleVersion(cfg.version)
                is GradleProvider.File -> useInstallation(File(cfg.path))
            }
        }
        .forProjectDirectory(config.projectDir)
        .connect()

fun ProjectConnection.getBuildModel(config: Config, path: String): DefaultBuild {
    val arguments = mutableListOf(
        "--init-script=$shareDir/init.gradle",
        "-Dorg.nixos.gradle2nix.configurations='${config.configurations.joinToString(",")}'"
    )

    if (path.isNotEmpty()) {
        arguments += "--project-dir=$path"
    }

    if (config.projectCacheDir != null) {
        arguments += "--project-cache-dir=${config.projectCacheDir}"
    }

    return model(Build::class.java)
        .withArguments(arguments)
        .apply {
            if (!config.quiet) {
                setStandardOutput(System.err)
                setStandardError(System.err)
            }
        }
        .get()
        .let { DefaultBuild(it) }
}
