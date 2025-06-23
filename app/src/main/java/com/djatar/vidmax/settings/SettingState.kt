package com.djatar.vidmax.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
inline fun <reified T> rememberSetting(
    key: String,
    defValue: T
): MutableState<T> = remember { settingState(key, defValue) }

inline fun <reified T> settingState(
    key: String,
    defValue: T
): MutableState<T> {
    val state = mutableStateOf(Settings.getValue(key, defValue))

    return object : MutableState<T> {
        override var value: T
            get() = state.value
            set(value) {
                state.value = value
                when (value) {
                    is String -> Settings.putString(key, value)
                    is Int -> Settings.putInt(key, value)
                    is Boolean -> Settings.putBoolean(key, value)
                    else -> throw IllegalArgumentException("Type ${T::class.java} is not supported")
                }
            }

        override fun component1(): T = value
        override fun component2(): (T) -> Unit = { value = it }
    }
}

inline fun <reified T> Settings.getValue(key: String, defValue: T): T {
    return when (T::class) {
        String::class -> getString(key, defValue as String) as T
        Int::class -> getInt(key, defValue as Int) as T
        Boolean::class -> getBoolean(key, defValue as Boolean) as T
        else -> throw IllegalArgumentException("Type: ${T::class.java} is not supported")
    }
}