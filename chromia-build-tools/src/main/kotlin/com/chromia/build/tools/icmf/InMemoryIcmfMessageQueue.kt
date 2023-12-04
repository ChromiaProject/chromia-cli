package com.chromia.build.tools.icmf

import java.util.concurrent.ConcurrentHashMap
import mu.KLogging

object InMemoryIcmfMessageQueue: KLogging() {
    private val messageTopicQueues = ConcurrentHashMap<String, MutableList<IcmfMessageOp>>()

    fun addMessage(msg: IcmfMessageOp, height: Long) {
        logger.info("Added message $msg to queue at height $height")
        messageTopicQueues.getOrPut(msg.topic) { mutableListOf() }.add(msg)
    }

    fun getMessages(from: Int, topic: String): List<IcmfMessageOp> {
        val messages = messageTopicQueues.getOrPut(topic) { mutableListOf() }
        return messages.subList(from, messages.size).also {
            logger.info("Found messages $it in queue")
        }
    }
}
