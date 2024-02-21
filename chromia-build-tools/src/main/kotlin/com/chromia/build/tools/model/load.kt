package com.chromia.cli.model

import com.chromia.cli.parser.loadAnchor
import java.io.File

fun parseModel(src: File): ChromiaModel = ChromiaModel.load(loadAnchor(src, ChromiaModel.schema))
