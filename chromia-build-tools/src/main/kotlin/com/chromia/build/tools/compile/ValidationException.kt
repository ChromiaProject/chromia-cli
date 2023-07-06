package com.chromia.build.tools.compile

import net.postchain.common.exception.UserMistake

class ValidationException(msg: String): UserMistake(msg)
