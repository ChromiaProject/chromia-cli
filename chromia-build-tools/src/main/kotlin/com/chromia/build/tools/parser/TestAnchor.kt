package com.chromia.build.tools.parser

import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.serializer.AnchorGenerator

class TestAnchor : AnchorGenerator {
    override fun nextAnchor(p0: Node?): String {
        return "hello"
    }
}