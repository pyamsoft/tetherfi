package com.pyamsoft.tetherfi.core

import androidx.annotation.CheckResult
import dagger.Binds
import dagger.Module

@Module
abstract class CoreAppModule {

  @Binds @CheckResult internal abstract fun bindFeatureFlags(impl: AppFeatureFlags): FeatureFlags
}
