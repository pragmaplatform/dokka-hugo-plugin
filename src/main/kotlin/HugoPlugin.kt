package org.jetbrains.dokka.hugo

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.PackageListCreator
import org.jetbrains.dokka.base.renderers.RootCreator
import org.jetbrains.dokka.base.renderers.sourceSets
import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProvider
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.gfm.GfmPlugin
import org.jetbrains.dokka.gfm.renderer.CommonmarkRenderer
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.*
import org.jetbrains.dokka.transformers.pages.PageTransformer

class HugoPlugin : DokkaPlugin() {

    val hugoPreprocessors by extensionPoint<PageTransformer>()

    private val dokkaBase by lazy { plugin<DokkaBase>() }

    val renderer by extending {
        (CoreExtensions.renderer
                providing { HugoRenderer(it) }
                override plugin<GfmPlugin>().renderer)
    }

    val locationProvider by extending {
        (dokkaBase.locationProviderFactory
                providing { HugoLocationProviderFactory(it) }
                override plugin<GfmPlugin>().locationProvider)
    }

    val rootCreator by extending {
        hugoPreprocessors with RootCreator
    }

    val packageListCreator by extending {
        hugoPreprocessors providing {
            PackageListCreator(it, RecognizedLinkFormat.DokkaJekyll)
        } order { after(rootCreator) }
    }
}

private fun isUndocumented(documentable: Documentable, sourceSet: DokkaConfiguration.DokkaSourceSet): Boolean {
    fun resolveDependentSourceSets(sourceSet: DokkaConfiguration.DokkaSourceSet): List<DokkaConfiguration.DokkaSourceSet> {
        return sourceSet.dependentSourceSets.mapNotNull { sourceSetID ->
            documentable.sourceSets.singleOrNull { it.sourceSetID == sourceSetID }
        }
    }

    fun withAllDependentSourceSets(sourceSet: DokkaConfiguration.DokkaSourceSet): Sequence<DokkaConfiguration.DokkaSourceSet> = sequence {
        yield(sourceSet)
        for (dependentSourceSet in resolveDependentSourceSets(sourceSet)) {
            yieldAll(withAllDependentSourceSets(dependentSourceSet))
        }
    }

    return withAllDependentSourceSets(sourceSet).all { sourceSetOrDependentSourceSet ->
        documentable.documentation[sourceSetOrDependentSourceSet]?.children?.isEmpty() ?: true
    }
}

// Hugo uses Goldmark since 0.60
class HugoRenderer(
    context: DokkaContext
) : CommonmarkRenderer(context) {

    override val preprocessors = context.plugin<HugoPlugin>().query { hugoPreprocessors }

    override fun buildPage(
        page: ContentPage,
        content: (StringBuilder, ContentPage) -> Unit
    ): String {
        val documentable = page.documentable
        var atLeastOneIsDocumented = false
        documentable?.sourceSets?.forEach { sourceSet ->
            val setIsDocumented = !isUndocumented(documentable, sourceSet)
            atLeastOneIsDocumented = atLeastOneIsDocumented || setIsDocumented
            println(">>>> ANDRONIC : ${setIsDocumented} | ${documentable.name} | ${documentable.dri.packageName}")
        }

        if (atLeastOneIsDocumented) {
            val builder = StringBuilder()
            builder.append("+++\n")
            buildFrontMatter(page, builder)
            builder.append("+++\n\n")
            content(builder, page)
            return builder.toString()
        } else {
            return ""
        }
    }

    private fun buildFrontMatter(page: ContentPage, builder: StringBuilder) {
        val hugoConfiguration = getConfig()
        val title = page.name.getTitle(hugoConfiguration)
        builder.append("title = \"${title}\"\n")
        builder.append("draft = false\n")
        builder.append("toc = false\n")
        builder.append("type = \"api\"\n")

        // Add menu item for each package
        if (page is PackagePage) {
            val linkTitle = page.name.getLinkTitle(hugoConfiguration)
            builder.append("linktitle = \"${linkTitle}\"\n")
        }
    }

    private fun getConfig() : HugoConfiguration{
        var hugoConfiguration = HugoConfiguration()
        try {
            val config = configuration<HugoPlugin, HugoConfiguration>(context)
            config?.let {
                hugoConfiguration = config
            }
            return hugoConfiguration
        } catch (exception: Exception) {
            return hugoConfiguration
        }
    }

    override fun StringBuilder.buildNavigation(page: PageNode) {
        locationProvider.ancestors(page).asReversed().forEach { node ->
            if (node.isNavigable) {
                buildLink(node, page)
                append(" / ")
            } else append(node.name)
        }
        buildParagraph()
    }

    // copied from GfmPlugin
    private val PageNode.isNavigable: Boolean
        get() = this !is RendererSpecificPage || strategy != RenderingStrategy.DoNothing

    // copied from GfmPlugin
    private fun StringBuilder.buildLink(to: PageNode, from: PageNode) =
        buildLink(locationProvider.resolve(to, from)!!) {
            append(to.name)
        }

    // copied from GfmPlugin
    private fun StringBuilder.buildParagraph() {
        append("\n\n")
    }

    private fun StringBuilder.buildB() {
        append("<b>")
    }

    private fun StringBuilder.buildEndB() {
        append("</b>")
    }

    private fun StringBuilder.buildNewLine() {
        append("\n")
    }

    override fun StringBuilder.wrapGroup(
        node: ContentGroup,
        pageContext: ContentPage,
        childrenCallback: StringBuilder.() -> Unit
    ) {
        return when {
            node.hasStyle(TextStyle.Block) -> {
                childrenCallback()
                buildNewLine()
            }
            node.hasStyle(TextStyle.Paragraph) -> {
                buildParagraph()
                childrenCallback()
                buildParagraph()
            }
            node.dci.kind in setOf(ContentKind.Symbol) -> {
                buildB()
                childrenCallback()
                buildEndB()
            }
            else -> childrenCallback()
        }
    }

    override fun StringBuilder.buildLink(address: String, content: StringBuilder.() -> Unit) {
        fun isExternalHref(address: String) = address.contains(":/")

        if (isExternalHref(address)) {
            append("[")
            content()
            append("]($address)")
        } else {
            append("[")
            content()
            append("]({{< relref \"$address\" >}})")
        }
    }

    override fun StringBuilder.buildCodeBlock(
        code: ContentCodeBlock,
        pageContext: ContentPage
    ) {
        append(if (code.language.isEmpty()) "```java\n" else "```$code.language\n")
        code.children.forEach {
            buildContentNode(it, pageContext)
        }
        append("\n```\n")
    }

    override fun StringBuilder.buildCodeInline(
        code: ContentCodeInline,
        pageContext: ContentPage
    ) {
        code.children.forEach {
            buildContentNode(it, pageContext)
        }
    }

    override fun StringBuilder.buildPlatformDependent(
        content: PlatformHintedContent,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        buildPlatformDependentItem(content.inner, content.sourceSets, pageContext)
    }

    private fun StringBuilder.buildPlatformDependentItem(
        content: ContentNode,
        sourceSets: Set<DisplaySourceSet>,
        pageContext: ContentPage,
    ) {
        if (content is ContentGroup && content.children.firstOrNull { it is ContentTable } != null) {
            buildContentNode(content, pageContext, sourceSets)
        } else {
            val distinct = sourceSets.map {
                it to buildString { buildContentNode(content, pageContext, setOf(it)) }
            }.groupBy(Pair<DisplaySourceSet, String>::second, Pair<DisplaySourceSet, String>::first)

            distinct.filter { it.key.isNotBlank() }.forEach { (text, platforms) ->
                append(" ")
                append(" $text ")
                buildNewLine()
            }
        }
    }

    // based on GfmPlugin
    override fun StringBuilder.buildTable(
        node: ContentTable,
        pageContext: ContentPage,
        sourceSetRestriction: Set<DisplaySourceSet>?
    ) {
        append("<table>\n")

        buildNewLine()
        if (node.dci.kind == ContentKind.Sample || node.dci.kind == ContentKind.Parameters) {
            node.sourceSets.forEach { sourcesetData ->
                buildNewLine()
                buildTable(
                    node.copy(
                        children = node.children.filter { it.sourceSets.contains(sourcesetData) },
                        dci = node.dci.copy(kind = ContentKind.Main)
                    ), pageContext, sourceSetRestriction
                )
                buildNewLine()
            }
        } else {
            append("<thead>\n")
            append("<tr>\n")

            val size = node.header.size

            if (node.header.isNotEmpty()) {
                if (size == 1) {
                    val nodeHeader = node.header.first()
                    nodeHeader.children.forEach {
                        append("<th>")
                        it.build(this, pageContext, it.sourceSets)
                        append("</th>")
                    }
                }
                buildNewLine()
            } else {
                append("<th></th>\n".repeat(node.header.size))
            }
            append("</tr>\n")
            append("</thead>\n")
            append("<tbody>\n")

            node.children.forEach {
                append("<tr>\n")
                if (it.hasAnyContent()) {
                    it.children.forEach {
                        append("<td>\n")
                        append("{{% md %}}\n")
                        append("\n")
                        append(buildString { it.build(this, pageContext) })

                        append("\n")
                        append("{{% /md %}}\n")
                        append("</td>\n")
                    }
                    append("<td></td>\n".repeat(Math.max(0, node.header.size - it.children.size)))

                    append("</tr>\n")
                    append("\n")
                }
            }
            append("</tbody>\n")
        }

        append("</table>\n")
    }

    // copied from GfmPlugin
    override fun StringBuilder.buildDivergent(
        node: ContentDivergentGroup,
        pageContext: ContentPage
    ) {

        val distinct =
            node.groupDivergentInstances(pageContext, { instance, contentPage, sourceSet ->
                instance.before?.let { before ->
                    buildString { buildContentNode(before, pageContext, sourceSet) }
                } ?: ""
            }, { instance, contentPage, sourceSet ->
                instance.after?.let { after ->
                    buildString { buildContentNode(after, pageContext, sourceSet) }
                } ?: ""
            })

        distinct.values.forEach { entry ->
            val (instance, sourceSets) = entry.getInstanceAndSourceSets()

            instance.before?.let {
                buildContentNode(
                    it,
                    pageContext,
                    sourceSets.first()
                ) // It's workaround to render content only once
                buildNewLine()
            }

            buildNewLine()
            entry.groupBy {
                buildString {
                    buildContentNode(
                        it.first.divergent,
                        pageContext,
                        setOf(it.second)
                    )
                }
            }
                .values.forEach { innerEntry ->
                    val (innerInstance, innerSourceSets) = innerEntry.getInstanceAndSourceSets()
                    if (sourceSets.size > 1) {
                        buildSourceSetTags(innerSourceSets)
                        buildNewLine()
                    }
                    innerInstance.divergent.build(
                        this@buildDivergent,
                        pageContext,
                        setOf(innerSourceSets.first())
                    ) // It's workaround to render content only once
                    buildNewLine()
                }

            instance.after?.let {
                buildNewLine()
                buildContentNode(
                    it,
                    pageContext,
                    sourceSets.first()
                ) // It's workaround to render content only once
                buildNewLine()
            }

            buildParagraph()
        }
    }

    private fun List<Pair<ContentDivergentInstance, DisplaySourceSet>>.getInstanceAndSourceSets() =
        this.let { Pair(it.first().first, it.map { it.second }.toSet()) }

    private fun StringBuilder.buildSourceSetTags(sourceSets: Set<DisplaySourceSet>) =
        append(sourceSets.joinToString(prefix = "[", postfix = "]") { it.name })

}

class HugoLocationProviderFactory(val context: DokkaContext) : LocationProviderFactory {

    override fun getLocationProvider(pageNode: RootPageNode) =
        HugoLocationProvider(pageNode, context)
}

class HugoLocationProvider(
    pageGraphRoot: RootPageNode,
    dokkaContext: DokkaContext
) : DokkaLocationProvider(pageGraphRoot, dokkaContext, ".md") {
    override val PAGE_WITH_CHILDREN_SUFFIX = "_index"
}