package com.pyamsoft.tetherfi.service.prereq

import com.pyamsoft.tetherfi.server.prereq.permission.PermissionGuard
import com.pyamsoft.tetherfi.server.prereq.vpn.VpnChecker
import javax.inject.Inject

internal class AndroidHotspotRequirements
@Inject
internal constructor(
    private val permission: PermissionGuard,
    private val vpnChecker: VpnChecker,
) : HotspotRequirements {

  override fun blockers(): List<HotspotStartBlocker> {
    val blockers = mutableListOf<HotspotStartBlocker>()

    if (!permission.canCreateWiDiNetwork()) {
      blockers.add(HotspotStartBlocker.PERMISSION)
    }

    if (vpnChecker.isUsingVpn()) {
      blockers.add(HotspotStartBlocker.VPN)
    }

    return blockers
  }
}
