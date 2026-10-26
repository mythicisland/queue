package net.mythicisland.queue.shared.logger

import org.apache.logging.log4j.message.MapMessage

class LogMessage(
    private val text: String,
    fields: List<Field>,
) : MapMessage<LogMessage, Any>(fields.size) {

    init {
        for (field in fields) {
            with(field.key, field.value)
        }
    }

    override fun getFormattedMessage(): String = text

    override fun formatTo(buffer: StringBuilder) {
        buffer.append(text)
    }

    override fun formatTo(formats: Array<String>, buffer: StringBuilder) {
        super.formatTo(formats, buffer)
    }

}
