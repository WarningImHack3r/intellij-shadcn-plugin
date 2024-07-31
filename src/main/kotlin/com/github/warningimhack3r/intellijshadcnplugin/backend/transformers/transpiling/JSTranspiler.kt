package com.github.warningimhack3r.intellijshadcnplugin.backend.transformers.transpiling

import com.caoccao.javet.swc4j.Swc4j
import com.caoccao.javet.swc4j.enums.Swc4jMediaType
import com.caoccao.javet.swc4j.enums.Swc4jSourceMapOption
import com.caoccao.javet.swc4j.options.Swc4jTranspileOptions
import com.github.warningimhack3r.intellijshadcnplugin.backend.transformers.transpiling.transpilers.JSXTranspiler
import com.github.warningimhack3r.intellijshadcnplugin.backend.transformers.transpiling.transpilers.VueTranspiler
import java.net.URL

object JSTranspiler {

    enum class Transpiler {
        VUE,
        JSX,
        DEFAULT
    }

    private fun extensionFromMedia(media: Swc4jMediaType): String {
        return when (media) {
            Swc4jMediaType.JavaScript -> "js"
            Swc4jMediaType.TypeScript -> "ts"
            Swc4jMediaType.Tsx -> "tsx"
            Swc4jMediaType.Jsx -> "jsx"
            Swc4jMediaType.Mjs -> "mjs"
            Swc4jMediaType.Cjs -> "cjs"
            Swc4jMediaType.Mts -> "mts"
            Swc4jMediaType.Cts -> "cts"
            Swc4jMediaType.Dts -> "d.ts"
            Swc4jMediaType.Dmts -> "d.mts"
            Swc4jMediaType.Dcts -> "d.cts"
            Swc4jMediaType.Json -> "json"
            Swc4jMediaType.Wasm -> "wasm"
            Swc4jMediaType.TsBuildInfo -> "tsbuildinfo"
            Swc4jMediaType.SourceMap -> "map"
            Swc4jMediaType.Unknown -> "unknown"
        }
    }

    fun transpileToJs(from: Swc4jMediaType, to: Transpiler, code: String): String {
        val options = Swc4jTranspileOptions()
            .setSpecifier(URL("file:///index.${extensionFromMedia(from)}"))
            .setMediaType(from)
            .setKeepComments(true)
            .setSourceMap(Swc4jSourceMapOption.None)

        with(options) {
            when (to) {
                Transpiler.VUE -> setPluginHost(VueTranspiler())
                Transpiler.JSX -> setPluginHost(JSXTranspiler())
                Transpiler.DEFAULT -> {
                    // Do nothing
                }
            }
        }

        return Swc4j().transpile(code, options).code
    }
}
