package com.pyamsoft.tetherfi.service.prereq

import com.pyamsoft.tetherfi.server.prereq.permission.PermissionGuard
import com.pyamsoft.tetherfi.server.prereq.vpn.VpnChecker
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class AndroidHotspotRequirements
@Inject
internal constructor(
    private val permission: PermissionGuard,
    private val vpnChecker: VpnChecker,
) : HotspotRequirements {

  override suspend fun blockers(): Collection<HotspotStartBlocker> =
      withContext(context = Dispatchers.Default) {
        val blockers = mutableSetOf<HotspotStartBlocker>()

        if (!permission.canCreateNetwork()) {
          blockers.add(HotspotStartBlocker.PERMISSION)
        }

        if (vpnChecker.isUsingVpn()) {
          blockers.add(HotspotStartBlocker.VPN)
        }

        return@withContext blockers
      }
}
