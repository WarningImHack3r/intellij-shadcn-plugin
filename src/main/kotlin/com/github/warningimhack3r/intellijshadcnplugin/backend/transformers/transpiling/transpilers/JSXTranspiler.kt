package com.github.warningimhack3r.intellijshadcnplugin.backend.transformers.transpiling.transpilers

import com.caoccao.javet.swc4j.ast.expr.Swc4jAstJsxElement
import com.caoccao.javet.swc4j.ast.interfaces.ISwc4jAstProgram
import com.caoccao.javet.swc4j.ast.visitors.Swc4jAstVisitor
import com.caoccao.javet.swc4j.ast.visitors.Swc4jAstVisitorResponse
import com.caoccao.javet.swc4j.plugins.ISwc4jPluginHost

class JSXTranspiler : ISwc4jPluginHost, Swc4jAstVisitor() {

    override fun process(program: ISwc4jAstProgram<*>?): Boolean {
        program?.visit(this)
        return true
    }

    override fun visitJsxElement(node: Swc4jAstJsxElement?): Swc4jAstVisitorResponse {
        node?.eval()?.ifPresent { value ->
            println(value)
        }
        return super.visitJsxElement(node)
    }
}
