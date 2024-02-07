package io.github.xiaobaicz.common.content

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.auto.service.AutoService
import io.github.xiaobaicz.common.app.Application.ActivityLifecycleCallbacksDefault
import io.github.xiaobaicz.common.provider.ContextProvider
import io.github.xiaobaicz.common.spi.ApplicationLifecycleSpi
import java.util.UUID

// 传输数据缓存
private val dataCacheMap = HashMap<String, Any>()

// 传输数据Key A -> B
const val KEY_DATA = "intent_x_data"

// 返回数据Key A <- B
const val KEY_RESULT_DATA = "intent_x_result_data"

// ----------------------------------

/**
 * 获取缓存
 */
private fun getCache(key: String): Any? {
    return dataCacheMap[key]
}

/**
 * 插入缓存
 */
private fun putCache(obj: Any): String {
    val key = UUID.randomUUID().toString()
    dataCacheMap[key] = obj
    return key
}

/**
 * 删除缓存
 */
private fun delCache(key: String) {
    dataCacheMap.remove(key)
}

// ----------------------------------

/**
 * 解析传输数据
 * @return 数据
 * @throws ClassCastException 数据类型转换失败
 */
@Throws(ClassCastException::class)
fun <T> Intent?.parseData(): T? {
    if (this == null) return null
    return parse(KEY_DATA)
}

/**
 * 解析返回数据
 * @return 数据
 * @throws ClassCastException 数据类型转换失败
 */
@Throws(ClassCastException::class)
fun <T> Intent?.parseResultData(): T? {
    if (this == null) return null
    return parse<T>(KEY_RESULT_DATA, true)
}

// 解析数据
@Suppress("UNCHECKED_CAST")
@Throws(ClassCastException::class)
private fun <T> Intent.parse(iKey: String, del: Boolean = false): T? {
    val key = getStringExtra(iKey) ?: return null
    val obj = getCache(key)
    if (del)
        delCache(key)
    return obj as T?
}

// ----------------------------------

/**
 * 创建数据Intent, 禁止外部调用
 * @hide
 */
fun <T : Any> dataIntent(iKey: String, obj: T? = null): Intent {
    val intent = Intent()
    obj?.also { intent.putExtra(iKey, putCache(it)) }
    return intent
}

/**
 * 创建跳转Activity的数据Intent
 */
inline fun <reified A : Activity> Context.startIntent(obj: Any? = null): Intent {
    val intent = dataIntent(KEY_DATA, obj)
    intent.component = ComponentName(this@startIntent, A::class.java)
    return intent
}

// ----------------------------------

/**
 * 跳转指定Activity
 * @param obj 数据
 */
inline fun <reified A : Activity> toActivity(obj: Any? = null) {
    ContextProvider.visibleActivity.get {
        it.toActivity<A>(obj)
    }
}

/**
 * 跳转指定Activity
 * @param obj 数据
 */
inline fun <reified A : Activity> Activity.toActivity(obj: Any? = null) {
    startActivity(startIntent<A>(obj))
}

/**
 * 跳转指定Activity
 * @param obj 数据
 */
inline fun <reified A : Activity> Fragment.toActivity(obj: Any? = null) {
    startActivity(requireContext().startIntent<A>(obj))
}

// ----------------------------------

/**
 * 关闭并返回数据
 */
fun <T : Any> Activity.finish(code: Int, obj: T? = null) {
    val intent = dataIntent(KEY_RESULT_DATA, obj)
    setResult(code, intent)
    finish()
}

/**
 * 关闭并返回数据
 */
fun <T : Any> Fragment.finish(code: Int, obj: T? = null) {
    requireActivity().finish(code, obj)
}

// ----------------------------------

fun ComponentActivity.registerForActivityResult(callback: ActivityResultCallback<ActivityResult>): ActivityResultLauncher<Intent> {
    return registerForActivityResult(ActivityResultContracts.StartActivityForResult(), callback)
}

inline fun <reified A : Activity> ActivityResultLauncher<Intent>.launch(context: Context, obj: Any? = null) {
    launch(context.startIntent<A>(obj))
}

// ----------------------------------

/**
 * 传输数据缓存管理器
 */
@AutoService(ApplicationLifecycleSpi::class)
class DataCacheHandler : ApplicationLifecycleSpi, ActivityLifecycleCallbacksDefault {

    override fun onCreate(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityPostDestroyed(activity: Activity) {
        // 存在缓存则删除
        val key = activity.intent.getStringExtra(KEY_DATA) ?: return
        delCache(key)
    }

}