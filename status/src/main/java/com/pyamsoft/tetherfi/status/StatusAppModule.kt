package com.pyamsoft.tetherfi.status

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.status.vpn.AndroidVpnChecker
import com.pyamsoft.tetherfi.status.vpn.VpnChecker
import dagger.Binds
import dagger.Module

@Module
abstract class StatusAppModule {

  @Binds @CheckResult internal abstract fun bindVpnChecker(impl: AndroidVpnChecker): VpnChecker
}
