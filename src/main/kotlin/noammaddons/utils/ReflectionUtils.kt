package noammaddons.utils

import kotlinx.coroutines.runBlocking
import kotlin.coroutines.Continuation


object ReflectionUtils {
    fun invoke(`object`: Any, methodName: String): Boolean {
        return try {
            val method = `object`.javaClass.getDeclaredMethod(methodName)
            method.isAccessible = true
            method.invoke(`object`)
            true
        }
        catch (ignored: Exception) {
            false
        }
    }

    fun invokeSuspendMethod(`object`: Any, methodName: String): Boolean {
        return try {
            val method = `object`.javaClass.getDeclaredMethod(methodName, Continuation::class.java)
            method.isAccessible = true
            runBlocking {
                method.invoke(`object`, this)
            }
            true
        }
        catch (ignored: Exception) {
            false
        }
    }


    fun getField(`object`: Any, name: String): Any? {
        return try {
            val field = `object`.javaClass.getDeclaredField(name)
            field.isAccessible = true
            field[`object`]
        }
        catch (ignored: Exception) {
            null
        }
    }

    fun setField(`object`: Any, fieldName: String, value: Any?): Boolean {
        return try {
            val field = `object`.javaClass.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(`object`, value)
            true
        }
        catch (ignored: Exception) {
            false
        }
    }
}

