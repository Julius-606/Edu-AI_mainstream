package com.example.edu_ai.utils

object MathSanitizer {

    private val subscriptMap = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
        'a' to 'ₐ', 'e' to 'ₑ', 'h' to 'ₕ', 'i' to 'ᵢ', 'j' to 'ⱼ',
        'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ', 'o' to 'ₒ',
        'p' to 'ₚ', 'r' to 'ᵣ', 's' to 'ₛ', 't' to 'ₜ', 'u' to 'ᵤ',
        'v' to 'ᵥ', 'x' to 'ₓ'
    )

    private val superscriptMap = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
        'n' to 'ⁿ', 'i' to 'ⁱ'
    )

    /**
     * Cleans mathematical expressions, LaTeX commands, raw dollar delimiters,
     * and converts clinical subscripts/superscripts to readable Unicode.
     * Thoroughly strips unwanted '$' characters while preserving true currency.
     */
    fun sanitize(text: String): String {
        if (text.isEmpty()) return text

        var result = text

        // 1. Remove LaTeX display math blocks: $$...$$ and \[...\]
        result = Regex("""\$\$([\s\S]*?)\$\$""").replace(result) { it.groupValues[1].trim() }
        result = Regex("""\\\[([\s\S]*?)\\\]""").replace(result) { it.groupValues[1].trim() }

        // 2. Remove LaTeX inline math delimiters: \(...\) and $...$ (NON-GREEDY)
        result = Regex("""\\\(([\s\S]*?)\\\)""").replace(result) { it.groupValues[1].trim() }
        result = Regex("""\$([^\$\n]+?)\$""").replace(result) { it.groupValues[1].trim() }

        // 3. Remove begin/end environment blocks like \begin{aligned} ... \end{aligned}
        result = Regex("""\\begin\{[a-zA-Z*]+\}""").replace(result, "")
        result = Regex("""\\end\{[a-zA-Z*]+\}""").replace(result, "")

        // 4. Common Greek, calculus & math symbols
        result = result
            .replace(Regex("""\\rightarrow\b|\\to\b"""), "→")
            .replace(Regex("""\\leftarrow\b"""), "←")
            .replace(Regex("""\\uparrow\b"""), "↑")
            .replace(Regex("""\\downarrow\b"""), "↓")
            .replace(Regex("""\\Rightarrow\b"""), "⇒")
            .replace(Regex("""\\Leftarrow\b"""), "⇐")
            .replace(Regex("""\\leftrightarrow\b"""), "↔")
            .replace(Regex("""\\ge\b|\\geq\b"""), "≥")
            .replace(Regex("""\\le\b|\\leq\b"""), "≤")
            .replace(Regex("""\\neq\b|\\ne\b"""), "≠")
            .replace(Regex("""\\pm\b"""), "±")
            .replace(Regex("""\\approx\b"""), "≈")
            .replace(Regex("""\\sim\b"""), "~")
            .replace(Regex("""\\times\b"""), "×")
            .replace(Regex("""\\div\b"""), "÷")
            .replace(Regex("""\\cdot\b"""), "·")
            .replace(Regex("""\\bullet\b"""), "•")
            .replace(Regex("""\\circ\b|\\degree\b"""), "°")
            .replace(Regex("""\\infty\b"""), "∞")
            .replace(Regex("""\\alpha\b"""), "α")
            .replace(Regex("""\\beta\b"""), "β")
            .replace(Regex("""\\gamma\b"""), "γ")
            .replace(Regex("""\\delta\b"""), "δ")
            .replace(Regex("""\\Delta\b"""), "Δ")
            .replace(Regex("""\\epsilon\b"""), "ε")
            .replace(Regex("""\\theta\b"""), "θ")
            .replace(Regex("""\\lambda\b"""), "λ")
            .replace(Regex("""\\mu\b|\\micro\b"""), "µ")
            .replace(Regex("""\\pi\b"""), "π")
            .replace(Regex("""\\sigma\b"""), "σ")
            .replace(Regex("""\\Sigma\b"""), "Σ")
            .replace(Regex("""\\tau\b"""), "τ")
            .replace(Regex("""\\phi\b"""), "φ")
            .replace(Regex("""\\omega\b"""), "ω")
            .replace(Regex("""\\Omega\b"""), "Ω")
            .replace(Regex("""\\partial\b"""), "∂")

        // 5. Clean escaped characters
        result = result
            .replace(Regex("""\\%"""), "%")
            .replace(Regex("""\\_"""), "_")
            .replace(Regex("""\\&"""), "&")
            .replace(Regex("""\\#"""), "#")

        // 6. LaTeX formatting macros
        result = Regex("""\\text(?:rm|sf|tt|normal)?\{([^}]*)\}""").replace(result) { it.groupValues[1] }
        result = Regex("""\\mathbf\{([^}]*)\}""").replace(result) { "**${it.groupValues[1]}**" }
        result = Regex("""\\textbf\{([^}]*)\}""").replace(result) { "**${it.groupValues[1]}**" }
        result = Regex("""\\mathit\{([^}]*)\}""").replace(result) { "*${it.groupValues[1]}*" }
        result = Regex("""\\textit\{([^}]*)\}""").replace(result) { "*${it.groupValues[1]}*" }
        result = Regex("""\\operatorname\{([^}]*)\}""").replace(result) { it.groupValues[1] }
        result = Regex("""\\underline\{([^}]*)\}""").replace(result) { it.groupValues[1] }
        result = Regex("""\\frac\{([^}]*)\}\{([^}]*)\}""").replace(result) {
            val num = it.groupValues[1].trim()
            val den = it.groupValues[2].trim()
            if (num.length <= 4 && den.length <= 4) "$num/$den" else "($num)/($den)"
        }
        result = Regex("""\\sqrt\{([^}]*)\}""").replace(result) { "√(${it.groupValues[1]})" }
        result = Regex("""\\left\s*([(\[{|.])""").replace(result) { it.groupValues[1] }
        result = Regex("""\\right\s*([)\]}|.])""").replace(result) { it.groupValues[1] }
        result = Regex("""\\[,;!]|\\quad|\\qquad""").replace(result, " ")

        // 7. Known clinical and medical formula subscripts/superscripts
        result = result
            .replace(Regex("""\bFEV_?1\b|\bFEV_\{1\}\b""", RegexOption.IGNORE_CASE), "FEV₁")
            .replace(Regex("""\bFEV_?1/FVC\b|\bFEV_\{1\}/FVC\b""", RegexOption.IGNORE_CASE), "FEV₁/FVC")
            .replace(Regex("""\bPaO_?2\b|\bPaO_\{2\}\b""", RegexOption.IGNORE_CASE), "PaO₂")
            .replace(Regex("""\bPaCO_?2\b|\bPaCO_\{2\}\b""", RegexOption.IGNORE_CASE), "PaCO₂")
            .replace(Regex("""\bSaO_?2\b|\bSaO_\{2\}\b""", RegexOption.IGNORE_CASE), "SaO₂")
            .replace(Regex("""\bSpO_?2\b|\bSpO_\{2\}\b""", RegexOption.IGNORE_CASE), "SpO₂")
            .replace(Regex("""\bHCO_?3\b|\bHCO_\{3\}\b""", RegexOption.IGNORE_CASE), "HCO₃")
            .replace(Regex("""\bH_?2CO_?3\b|\bH_\{2\}CO_\{3\}\b""", RegexOption.IGNORE_CASE), "H₂CO₃")
            .replace(Regex("""\bH_?2O\b|\bH_\{2\}O\b""", RegexOption.IGNORE_CASE), "H₂O")
            .replace(Regex("""\bCO_?2\b|\bCO_\{2\}\b""", RegexOption.IGNORE_CASE), "CO₂")
            .replace(Regex("""\bO_?2\b|\bO_\{2\}\b""", RegexOption.IGNORE_CASE), "O₂")
            .replace(Regex("""\bFiO_?2\b|\bFiO_\{2\}\b""", RegexOption.IGNORE_CASE), "FiO₂")
            .replace(Regex("""\bCa_?2\+?\b|\bCa\^\{2\+\}\b""", RegexOption.IGNORE_CASE), "Ca²⁺")
            .replace(Regex("""\bNa\+\b|\bNa\^\{\+\}\b"""), "Na⁺")
            .replace(Regex("""\bK\+\b|\bK\^\{\+\}\b"""), "K⁺")
            .replace(Regex("""\bCl-\b|\bCl\^\{-\}\b"""), "Cl⁻")
            .replace(Regex("""\bH\+\b|\bH\^\{\+\}\b"""), "H⁺")

        // 8. General LaTeX subscripts like A_{12} or A_2
        result = Regex("""_\{([a-zA-Z0-9+\-=()]+)\}""").replace(result) { match ->
            val inner = match.groupValues[1]
            inner.map { subscriptMap[it] ?: it }.joinToString("")
        }
        result = Regex("""_([0-9+\-=()aehijklmnoprstuvx])""").replace(result) { match ->
            val digit = match.groupValues[1][0]
            (subscriptMap[digit] ?: digit).toString()
        }

        // 9. General LaTeX superscripts like x^{2} or x^2
        result = Regex("""\^\{([a-zA-Z0-9+\-=()]+)\}""").replace(result) { match ->
            val inner = match.groupValues[1]
            inner.map { superscriptMap[it] ?: it }.joinToString("")
        }
        result = Regex("""\^([0-9+\-=()ni])""").replace(result) { match ->
            val digit = match.groupValues[1][0]
            (superscriptMap[digit] ?: digit).toString()
        }

        // 10. Clean escaped parenthetical wrappers
        result = result.replace(Regex("""\\([()\[\]{}])""")) { it.groupValues[1] }

        // 11. THOROUGH REMOVAL OF UNWANTED '$' CHARACTERS:
        // Remove $ adjacent to inequalities or mathematical operators: e.g. $< 70%$, $> 80%$, $\ge 80%$
        result = Regex("""\$\s*([<>≤≥=+\-±·/|~])""").replace(result) { it.groupValues[1] }
        result = Regex("""([<>≤≥=+\-±·/|~])\s*\$""").replace(result) { it.groupValues[1] }

        // Remove $ adjacent to numbers followed by % or ranges: e.g. $50% - 79%$, $80%$, $ < 0.70$
        result = Regex("""\$\s*(\d+(?:\.\d+)?\s*%)""").replace(result) { it.groupValues[1] }
        result = Regex("""(\d+(?:\.\d+)?\s*%)\s*\$""").replace(result) { it.groupValues[1] }

        // In table cells: e.g. "| $ 50% |" or "| $ |"
        result = Regex("""\|\s*\$""").replace(result, "| ")
        result = Regex("""\$\s*\|""").replace(result, " |")

        // Strip $ attached to words, letters or brackets: e.g. $FEV1, FEV1$, $(x)$, $(, )$
        result = Regex("""\$(?=[a-zA-Z(\[{])""").replace(result, "")
        result = Regex("""(?<=[a-zA-Z)\]}])\$""").replace(result, "")

        // Remove double dollar signs $$ if any remain
        result = result.replace("$$", "")

        // Remove any remaining stray single dollar sign that is NOT standard monetary currency
        // (Monetary currency: $ immediately followed by digits, with optional decimal, followed by space/punctuation/end)
        result = Regex("""\$(?!\d+(?:\.\d{2})?(?:[\s.,;!?]|$))""").replace(result, "")
        // If a $ is followed by digits but has %, operators, or range characters nearby, it's not currency
        result = Regex("""\$(?=\d+[\w\s\-–—/]*[%≤≥<>+=])""").replace(result, "")

        // Remove any solitary dollar signs surrounded by spaces
        result = Regex("""(^|\s)\$(\s|$)""").replace(result) { "${it.groupValues[1]}${it.groupValues[2]}" }

        return result
    }
}
