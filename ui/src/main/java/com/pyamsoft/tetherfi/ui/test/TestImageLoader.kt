package com.pyamsoft.tetherfi.ui.test

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.annotation.CheckResult
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import coil.ComponentRegistry
import coil.ImageLoader
import coil.decode.DataSource
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.DefaultRequestOptions
import coil.request.Disposable
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async

/** Only use for tests/previews */
private class TestImageLoader(context: Context) : ImageLoader {

  private val context = context.applicationContext
  private val loadingDrawable by lazy(LazyThreadSafetyMode.NONE) { ColorDrawable(Color.BLACK) }
  private val successDrawable by lazy(LazyThreadSafetyMode.NONE) { ColorDrawable(Color.GREEN) }

  private val disposable =
      object : Disposable {

        override val isDisposed: Boolean = true
        override val job: Deferred<ImageResult> =
            MainScope().async<ImageResult> {
              ErrorResult(
                  drawable = null,
                  request = ImageRequest.Builder(context).build(),
                  throwable = RuntimeException("Test"),
              )
            }

        override fun dispose() {}
      }

  override val components: ComponentRegistry = ComponentRegistry()

  override val defaults: DefaultRequestOptions = DefaultRequestOptions()

  override val diskCache: DiskCache? = null

  override val memoryCache: MemoryCache? = null

  override fun enqueue(request: ImageRequest): Disposable {
    request.apply {
      target?.onStart(placeholder = loadingDrawable)
      target?.onSuccess(result = successDrawable)
    }
    return disposable
  }

  override suspend fun execute(request: ImageRequest): ImageResult {
    return SuccessResult(
        drawable = successDrawable,
        request = request,
        dataSource = DataSource.MEMORY,
    )
  }

  override fun newBuilder(): ImageLoader.Builder {
    return ImageLoader.Builder(context)
  }

  override fun shutdown() {}
}

/** Only use for tests/previews */
@Composable
@CheckResult
internal fun createNewTestImageLoader(): ImageLoader {
  val context = LocalContext.current
  return TestImageLoader(context)
}
