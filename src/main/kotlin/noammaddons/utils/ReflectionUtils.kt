package noammaddons.utils


object ReflectionUtils {
	
	fun invoke(`object`: Any, methodName: String): Boolean {
		try {
			val method = `object`.javaClass.getDeclaredMethod(methodName)
			method.isAccessible = true
			method.invoke(`object`)
			return true
		}
		catch (ignored: Exception) {}
		return false
	}
	
	fun field(`object`: Any, name: String): Any? {
		try {
			val field = `object`.javaClass.getDeclaredField(name)
			field.isAccessible = true
			return field[`object`]
		}
		catch (ignored: Exception) {}
		return null
	}
}
