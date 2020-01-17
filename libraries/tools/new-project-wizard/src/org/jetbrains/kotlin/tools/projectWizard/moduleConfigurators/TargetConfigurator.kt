package org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators

import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.RawGradleIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.DefaultTargetConfigurationIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.TargetAccessIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleSubType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.ModuleKind


interface TargetConfigurator : ModuleConfigurator {
    override val moduleKind get() = ModuleKind.target

    fun canCoexistsWith(other: List<TargetConfigurator>): Boolean = true

    fun createTargetIrs(module: Module): List<BuildSystemIR>
    fun createInnerTargetIrs(module: Module): List<BuildSystemIR> = emptyList()
}

abstract class TargetConfiguratorWithTests : ModuleConfiguratorWithTests(), TargetConfigurator

interface SingleCoexistenceTargetConfigurator : TargetConfigurator {
    override fun canCoexistsWith(other: List<TargetConfigurator>): Boolean =
        other.none { it == this }
}

interface SimpleTargetConfigurator : TargetConfigurator, SingleCoexistenceTargetConfigurator {
    val moduleSubType: ModuleSubType
    override val moduleType get() = moduleSubType.moduleType
    override val id get() = "${moduleSubType.name}Target"
    override val text get() = moduleSubType.name.capitalize()

    override val suggestedModuleName: String? get() = moduleSubType.name


    override fun createTargetIrs(module: Module): List<BuildSystemIR> = buildList {
        +DefaultTargetConfigurationIR(
            module.createTargetAccessIr(moduleSubType),
            createInnerTargetIrs(module)
        )
    }
}

private fun Module.createTargetAccessIr(moduleSubType: ModuleSubType) =
    TargetAccessIR(
        moduleSubType,
        name.takeIf { it != moduleSubType.name }
    )


abstract class JsTargetConfigurator : TargetConfiguratorWithTests(), SingleCoexistenceTargetConfigurator {
    override val moduleType: ModuleType get() = ModuleType.js

    override fun defaultTestFramework(): KotlinTestFramework = KotlinTestFramework.JS
}

object JsBrowserTargetConfigurator : JsTargetConfigurator() {
    override val id = "jsBrowser"
    override val text = "Browser"
    override val suggestedModuleName = "browser"

    override fun createTargetIrs(module: Module): List<BuildSystemIR> = buildList {
        +DefaultTargetConfigurationIR(
            module.createTargetAccessIr(ModuleSubType.js),
            buildList {
                +RawGradleIR {
                    sectionCall("browser") {}
                }
            }
        )
    }
}

object JsNodeTargetConfigurator : JsTargetConfigurator() {
    override val id = "jsNode"
    override val text = "Node.js"
    override val suggestedModuleName = "nodeJs"


    override fun createTargetIrs(module: Module): List<BuildSystemIR> = buildList {
        +DefaultTargetConfigurationIR(
            module.createTargetAccessIr(ModuleSubType.js),
            buildList {
                +RawGradleIR {
                    sectionCall("nodejs") {}
                }
            }
        )
    }
}

object CommonTargetConfigurator : TargetConfiguratorWithTests(), SimpleTargetConfigurator, SingleCoexistenceTargetConfigurator {
    override val moduleSubType = ModuleSubType.common

    override fun defaultTestFramework(): KotlinTestFramework = KotlinTestFramework.COMMON
}

object JvmTargetConfigurator : TargetConfiguratorWithTests(),
    TargetConfigurator,
    SimpleTargetConfigurator,
    JvmModuleConfigurator,
    SingleCoexistenceTargetConfigurator {
    override val moduleSubType = ModuleSubType.jvm

    override fun defaultTestFramework(): KotlinTestFramework = KotlinTestFramework.JUNIT4
}

object AndroidTargetConfigurator : TargetConfigurator,
    SimpleTargetConfigurator,
    AndroidModuleConfigurator,
    SingleCoexistenceTargetConfigurator {
    override val moduleSubType = ModuleSubType.android
}