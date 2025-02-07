/*
 * Copyright 2024 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.ui.test

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.annotation.CheckResult
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import coil3.ComponentRegistry
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.Disposable
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import org.jetbrains.annotations.TestOnly

/** Only use for tests/previews */
private class TestImageLoader(context: Context) : ImageLoader {

    private val context = context.applicationContext
    private val loadingDrawable by lazy { ColorDrawable(Color.BLACK) }
    private val successDrawable by lazy { ColorDrawable(Color.GREEN) }

    private val disposable =
        object : Disposable {

            override val isDisposed: Boolean = true
            override val job: Deferred<ImageResult> =
                MainScope().async<ImageResult> {
                    ErrorResult(
                        image = null,
                        request = ImageRequest.Builder(context).build(),
                        throwable = RuntimeException("Test"),
                    )
                }

            override fun dispose() {}
        }

    override val components = ComponentRegistry()

    override val defaults = ImageRequest.Defaults()

    override val diskCache: DiskCache? = null

    override val memoryCache: MemoryCache? = null

    override fun enqueue(request: ImageRequest): Disposable {
        request.apply {
            target?.onStart(placeholder = loadingDrawable.asImage())
            target?.onSuccess(result = successDrawable.asImage())
        }
        return disposable
    }

    override suspend fun execute(request: ImageRequest): ImageResult {
        return SuccessResult(
            image = successDrawable.asImage(),
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
@TestOnly
@Composable
@CheckResult
@VisibleForTesting
fun createNewTestImageLoader(): ImageLoader {
  val context = LocalContext.current
  return TestImageLoader(context)
}
