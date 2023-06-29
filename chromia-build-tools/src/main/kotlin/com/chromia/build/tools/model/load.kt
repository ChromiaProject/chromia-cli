package com.chromia.cli.model

import com.chromia.cli.parser.loadAnchor
import net.postchain.gtv.yaml.GtvYaml
import java.io.File

fun parseModel(src: File) = GtvYaml().loadAnchor<ChromiaCliModel>(src)
