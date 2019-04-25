package com.kazurayam.visualtestinginks.gradle

import java.nio.file.Path

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.Zip

class VTPlugin implements Plugin<Project> {

    private void applyPlugins(Project project) {
        project.getPluginManager().apply(GroovyPlugin.class)
        // https://github.com/michel-kraemer/gradle-download-task
        project.getPluginManager().apply('de.undercouch.download')
    }

    /**
     *
     */
    public void apply(Project project) {

        // apply another plugins that VTPlugin depends upon
        applyPlugins(project)

        // Extension object
        VTPluginExtension extension = project.extensions.create("vt", VTPluginExtension)

        // tasks to generate VisualTesting distributables
        Task createDistributableGradlew  = project.getTasks().create(
                'createDistributableGradlew', Zip.class, {
                    archiveFileName = Constants.gradlewFileName
                    destinationDirectory = project.file("${project.buildDir}/dist")
                    from(".") {
                        // include the gradle wrapper
                        include "gradlew"
                        include "gradlew.bat"
                        include "gradlewks.bat"   // customized launcher using Java bundled in %KATALONSTUDIO_Home%\jre
                        include "gradle/**/*"
                    }
                })

        Task createVTComponents = project.getTasks().create(
                'createVTComponents', Zip.class, {
                    archiveFileName = Constants.vtComponentsFileName
                    destinationDirectory = project.file("${project.buildDir}/dist")
                    from(".") {
                        include "Test Cases/VT/**"
                        include "Scripts/VT/**"
                        include "Test Listeners/**/VT*"
                        include "Test Suites/VT/**"
                        include "Keywords/com/kazurayam/visualtesting/**"
                    }
                    from(".") {
                        include ".gitignore"
                    }
                })

        Task createVTExample    = project.getTasks().create(
                'createVTExample', Zip.class, {
                    archiveFileName = Constants.vtExampleFileName
                    destinationDirectory = project.file("${project.buildDir}/dist")
                    from(".") {
                        include "Profiles/CURA*"
                        include "Test Cases/CURA/**"
                        include "Object Repository/CURA/**"
                        include "Test Suites/CURA/**"
                        include "Scripts/CURA/**"
                        include "vt-run-CURA*"
                    }
                })

        Task createDist = project.getTasks().create(
            'createDist', {
                doLast {
                    project.mkdir "${project.buildDir}/dist"
                }
            })

        Task cleanDist = project.getTasks().create(
                'cleanDist', Delete.class, {
                    def dirName = "build/dist"
                    project.file( dirName ).list().each { f ->
                        delete "${dirName}/${f}"
                    }
                })
        cleanDist.dependsOn(createDist)

        Task distributables = project.getTasks().create('distributables')
        distributables.dependsOn(cleanDist)
        distributables.dependsOn(createDistributableGradlew)
        distributables.dependsOn(createVTComponents)
        distributables.dependsOn(createVTExample)
        createDistributableGradlew.mustRunAfter('cleanDist')
        createVTComponents.mustRunAfter('cleanDist')
        createVTExample.mustRunAfter('cleanDist')

        // tasks to import distributables into the people's VisualTesting projects
        Task importVTComponents = project.getTasks().create(
                'importVTComponents', ImportVTComponentsTask.class)
        Task importVTExample    = project.getTasks().create(
                'importVTExample', ImportVTExampleTask.class)

        // tasks to update jar files in the Drivers directory in a Katalon Studio project
        Task deleteExternalLibraries = project.getTasks().create(
                'deleteExternalLibraries', DeleteExternalLibrariesTask.class)
        Task importExternalLibraries = project.getTasks().create(
                'importExternalLibraries', ImportExternalLibrariesTask.class)
        Task updateDrivers = project.getTasks().create(
                'updateDrivers')
        updateDrivers.dependsOn(deleteExternalLibraries)
        updateDrivers.dependsOn(importExternalLibraries)
        importExternalLibraries.mustRunAfter('deleteExternalLibraries')

        // task as a single entry point, which enables a new Katalon Project of VisualTesting
        Task enableVisualTesting = project.getTasks().create(
                'enableVisualTesting')
        enableVisualTesting.dependsOn(importVTComponents)
        enableVisualTesting.dependsOn(importVTExample)
        enableVisualTesting.dependsOn(updateDrivers)



        // for DEBUG: Add a task 'greeting' that uses configuration from the extension object
        Task greeting = project.task('greeting') {
            doLast {
                println extension.message
            }
        }
    }
}
