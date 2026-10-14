package com.tailormyresume.app.ai

import com.tailormyresume.core.domain.IdGenerator

object FixedIds : IdGenerator {
    private var next = 0

    override fun newId(): String = "request-${++next}"
}
