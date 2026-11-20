package com.kmab.deft.feature.reader

/** Bounds the expanded render tree, including repeated references in an otherwise acyclic SVG. */
internal class ReaderSvgReferences {
    private val ids = mutableMapOf<String, SvgElement>()
    private val stack = ArrayDeque<SvgElement>()
    private var root: SvgElement? = null

    fun begin(
        tag: String,
        id: String?,
        references: List<String>,
    ): Boolean {
        val uniqueId = id == null || !ids.containsKey(id)
        val validParent = stack.lastOrNull()?.tag != "style"
        val singleRoot = validParent && (stack.isNotEmpty() || root == null)
        if (stack.size >= MAX_SVG_DEPTH || !uniqueId || !singleRoot) return false
        val element = SvgElement(tag, references)
        if (id != null) ids[id] = element
        stack.lastOrNull()?.children?.add(element) ?: run { root = element }
        stack.addLast(element)
        return true
    }

    fun end() {
        stack.removeLast()
    }

    fun isBounded(): Boolean = root?.let { SvgExpansionBudget(ids).visit(it, 1) } == true
}

private class SvgElement(
    val tag: String,
    val references: List<String>,
) {
    val children = mutableListOf<SvgElement>()
}

private class SvgExpansionBudget(
    private val ids: Map<String, SvgElement>,
) {
    private val active = mutableSetOf<SvgElement>()
    private var expanded = 0

    fun visit(
        element: SvgElement,
        depth: Int,
    ): Boolean {
        val withinBudget = depth <= MAX_SVG_DEPTH && ++expanded <= MAX_SVG_EXPANSIONS
        if (!withinBudget || !active.add(element)) return false
        val valid =
            element.children.all { visit(it, depth + 1) } &&
                element.references.all { id -> ids[id]?.let { visit(it, depth + 1) } == true }
        active.remove(element)
        return valid
    }
}

private const val MAX_SVG_DEPTH = 64
private const val MAX_SVG_EXPANSIONS = 50_000
