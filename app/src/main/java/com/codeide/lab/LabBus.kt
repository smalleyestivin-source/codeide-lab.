package com.codeide.lab

/** Canal sencillo para que otras actividades escriban en el registro de resultados. */
object LabBus {
    @Volatile
    var sink: ((String) -> Unit)? = null

    fun log(msg: String) {
        sink?.invoke(msg)
    }
}
