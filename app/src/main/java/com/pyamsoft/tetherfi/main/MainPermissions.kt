package com.pyamsoft.tetherfi.main

import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.bus.EventConsumer
import com.pyamsoft.pydroid.util.PermissionRequester
import com.pyamsoft.pydroid.util.doOnDestroy
import com.pyamsoft.tetherfi.status.PermissionRequests
import com.pyamsoft.tetherfi.status.PermissionResponse
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class MainPermissions
@Inject
internal constructor(
    private val permissionRequestBus: EventConsumer<PermissionRequests>,
    private val permissionResponseBus: EventBus<PermissionResponse>,
    @Named("server") private val serverPermissionRequester: PermissionRequester,
    @Named("notification") private val notificationPermissionRequester: PermissionRequester,
) {

  private var serverRequester: PermissionRequester.Requester? = null
  private var notificationRequester: PermissionRequester.Requester? = null

  fun register(activity: ComponentActivity) {
    // Watch lifecycle
    activity.doOnDestroy { unregister() }

    // Unregister any existing (double call?)
    unregister()

    val sr =
        serverPermissionRequester
            .registerRequester(activity) { granted ->
              if (granted) {
                Timber.d("Network permission granted, toggle proxy")

                // Broadcast in the background
                activity.lifecycleScope.launch(context = Dispatchers.Default) {
                  permissionResponseBus.emit(PermissionResponse.ToggleProxy)
                }
              } else {
                Timber.w("Network permission not granted")
              }
            }
            .also { serverRequester = it }

    val nr =
        notificationPermissionRequester
            .registerRequester(activity) { granted ->
              if (granted) {
                Timber.d("Notification permission granted")

                // Broadcast in the background
                activity.lifecycleScope.launch(context = Dispatchers.Default) {
                  permissionResponseBus.emit(PermissionResponse.RefreshNotification)
                }
              } else {
                Timber.w("Notification permission not granted")
              }
            }
            .also { notificationRequester = it }

    permissionRequestBus.also { f ->
      activity.lifecycleScope.launch(context = Dispatchers.Default) {
        f.collect { req ->
          when (req) {
            is PermissionRequests.Notification -> {
              nr.requestPermissions()
            }
            is PermissionRequests.Server -> {
              sr.requestPermissions()
            }
          }
        }
      }
    }
  }

  fun unregister() {
    serverRequester?.unregister()
    notificationRequester?.unregister()
  }
}
