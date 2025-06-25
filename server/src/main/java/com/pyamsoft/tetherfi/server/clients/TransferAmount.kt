/*
 * Copyright 2025 pyamsoft
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

package com.pyamsoft.tetherfi.server.clients

import androidx.annotation.CheckResult
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Stable
@Immutable
data class TransferAmount(
    val bytes: Long,
    val amount: Long,
    val unit: TransferUnit,
) {

  val display by lazy { "$amount ${unit.displayName}" }

  constructor(
      amount: Long,
      unit: TransferUnit
  ) : this(
      bytes = toBytes(amount, unit),
      amount = amount,
      unit = unit,
  )

  companion object {

    private const val UNIT_JUMP = 1024L

    @CheckResult
    private fun toBytes(amount: Long, unit: TransferUnit): Long {
      var total = amount
      var suffix = unit
      while (suffix != TransferUnit.BYTE) {
        suffix = suffix.previousSmallest()
        total *= UNIT_JUMP
      }

      return total
    }

    @CheckResult
    internal fun fromBytes(total: Long): TransferAmount {
      var amount = total
      var suffix = TransferUnit.BYTE
      while (amount > UNIT_JUMP) {
        suffix = suffix.nextLargest()
        amount /= UNIT_JUMP
      }

      return TransferAmount(
          bytes = total,
          amount = amount,
          unit = suffix,
      )
    }
  }
}

enum class TransferUnit(val displayName: String) {
  BYTE("bytes"),
  KB("KB"),
  MB("MB"),
  GB("GB"),
  TB("TB"),
  PB("PB");

  @CheckResult
  fun nextLargest(): TransferUnit =
      when (this) {
        BYTE -> KB
        KB -> MB
        MB -> GB
        GB -> TB
        TB -> PB
        else -> throw IllegalStateException("Bytes payload too big: $this")
      }

  @CheckResult
  fun previousSmallest(): TransferUnit =
      when (this) {
        PB -> TB
        TB -> GB
        GB -> MB
        MB -> KB
        KB -> BYTE
        else -> throw IllegalStateException("Bytes payload too small: $this")
      }
}
