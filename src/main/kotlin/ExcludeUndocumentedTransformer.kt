package org.jetbrains.dokka.hugo

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.DescriptorDocumentableSource
import org.jetbrains.dokka.base.transformers.documentables.SuppressedByConditionDocumentableFilterTransformer
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.FAKE_OVERRIDE
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.SYNTHESIZED
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.File
//import ApiDocsSuppress

/**
 * Works for packages, types, fields and methods.
 */
class ExcludeUndocumentedTransformer(dokkaContext: DokkaContext) :
    SuppressedByConditionDocumentableFilterTransformer(dokkaContext) {

    /**
     * Tells whether the passed [Documentable] should be excluded from the documentation.
     *
     * [WithExtraProperties] is a Dokka-interface that adds a map-field for extra properties.
     * The information about applied annotations is present in this map.
     *
     * Not every [Documentable] subclass implements this interface.
     */
    override fun shouldBeSuppressed(d: Documentable): Boolean {
        val documentableDescription = with(d) {
            buildString {

                context.configuration.moduleName.run {
                    append(this)
                    append(" | ")
                }

                dri.packageName?.run {
                    append(this)
                    append("/")
                }

                dri.classNames?.run {
                    append(this)
                    append("/")
                }

                dri.callable?.run {
                    append(name)
                    append("/")
                    append(signature())
                    append("\n")
                }
            }
        }

        var hugoConfiguration = HugoConfiguration()

        val localConfig = try {
            val config = configuration<HugoPlugin, HugoConfiguration>(context)
            config?.let {
                hugoConfiguration = config
            }
            hugoConfiguration
        } catch (exception: Exception) {
            hugoConfiguration
        }

//        if (localConfig.generateAllPathsFile) {
//            println(">>>>>>>>> localConfig.generateAllPathsFile is ${localConfig.generateAllPathsFile}")
//            File("/Users/andronic/pragma/experimental-engine/documentation/andronic.txt").appendText(documentableDescription)
//            return false
//        }

        val manuallyMarkedAsUndocumented = (d is WithExtraProperties<*>) && hasApiDocsSuppressAnnotation(d)
        if (manuallyMarkedAsUndocumented) {
            println(">>>>> ANDRONIC manuallyMarkedAsUndocumented: ${documentableDescription.plus(" | undocumented")}")
            return true
        }


        if (shouldBeReportedIfNotDocumented(d, d.sourceSets.single())) {
            val returnValue = isUndocumented(d, d.sourceSets.single())
            val documentedLog = documentableDescription.plus(if (returnValue) " | notDocumented" else " | isDocumented")
            println(">>>>> ANDRONIC in shouldBeReportedIfNotDocumented: $documentedLog")
            return returnValue
        }

//        println(">>>>> ANDRONIC: $documentableDescription")
        return false
    }

    private fun shouldBeReportedIfNotDocumented(
        documentable: Documentable, sourceSet: DokkaConfiguration.DokkaSourceSet
    ): Boolean {
        if (documentable is DParameter || documentable is DPackage || documentable is DModule) {
            return false
        }

        if (isConstructor(documentable)) {
            return false
        }

        if (isFakeOverride(documentable, sourceSet)) {
            return false
        }

        if (isSynthesized(documentable, sourceSet)) {
            return false
        }

        return true
    }

    private fun isConstructor(documentable: Documentable): Boolean {
        if (documentable !is DFunction) return false
        return documentable.isConstructor
    }

    private fun isFakeOverride(documentable: Documentable, sourceSet: DokkaConfiguration.DokkaSourceSet): Boolean {
        return callableMemberDescriptorOrNull(documentable, sourceSet)?.kind == FAKE_OVERRIDE
    }

    private fun isSynthesized(documentable: Documentable, sourceSet: DokkaConfiguration.DokkaSourceSet): Boolean {
        return callableMemberDescriptorOrNull(documentable, sourceSet)?.kind == SYNTHESIZED
    }

    private fun callableMemberDescriptorOrNull(
        documentable: Documentable, sourceSet: DokkaConfiguration.DokkaSourceSet
    ): CallableMemberDescriptor? {
        if (documentable is WithSources) {
            return documentable.sources[sourceSet]
                .safeAs<DescriptorDocumentableSource>()?.descriptor
                .safeAs<CallableMemberDescriptor>()
        }

        return null
    }

    private fun hasApiDocsSuppressAnnotation(annotated: WithExtraProperties<*>): Boolean =
        annotated.annotations().any(ApiDocsSuppressAnnotationCheck::test)

    /**
     * Extracts annotations from a [WithExtraProperties] object.
     *
     * Information about annotations is found in the map-field `extra`, added when implementing
     * [WithExtraProperties].
     *
     * All values with the type of [Annotations], a container-class for all applied annotations,
     * are extracted from the map-field. Then plain lists of [Annotation] are extracted from
     * every [Annotations] object and merged into the result list.
     */
    private fun WithExtraProperties<*>.annotations(): List<Annotations.Annotation> {
        return this.extra.allOfType<Annotations>().flatMap {
            it.directAnnotations.values.flatten()
        }
    }

    /**
     * Provides the method to check if an [Annotation] object represents [ApiDocsSuppress] annotation.
     */
    private object ApiDocsSuppressAnnotationCheck {
//        private val c  = ApiDocsSuppress::class.java
        /**
         * Checks if provided object represents [ApiDocsSuppress] annotation.
         *
         * Compares the package and the class extracted from the object with the package and the
         * class of the [ApiDocsSuppress] annotation.
         */
        fun test(a: Annotations.Annotation): Boolean {
//            if (a.dri.classNames == c.simpleName) {
            if (a.dri.classNames == "ApiDocsSuppress") {
                println(">>>>>> checking for annotation a: ${a.dri.packageName} | ${a.dri.classNames} | $a")
                return true
            }
            return false
        }
    }
}