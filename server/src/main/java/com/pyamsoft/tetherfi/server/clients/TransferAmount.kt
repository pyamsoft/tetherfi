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

package com.pyamsoft.tetherfi.server.clients

import androidx.annotation.CheckResult
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Stable
@Immutable
data class TransferAmount(
    val bytes: ULong,
    val amount: ULong,
    val unit: TransferUnit,
) {

  val display by lazy { "$amount ${unit.displayName}" }

  constructor(
      amount: ULong,
      unit: TransferUnit
  ) : this(
      bytes = toBytes(amount, unit),
      amount = amount,
      unit = unit,
  )

  companion object {

    private const val UNIT_JUMP = 1024UL

    @CheckResult
    private fun toBytes(amount: ULong, unit: TransferUnit): ULong {
      var total = amount
      var suffix = unit
      while (suffix != TransferUnit.BYTE) {
        suffix = suffix.previousSmallest()
        total *= UNIT_JUMP
      }

      return total
    }

    @CheckResult
    internal fun fromBytes(total: ULong): TransferAmount {
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
        TransferUnit.PB -> TransferUnit.TB
        TransferUnit.TB -> TransferUnit.GB
        TransferUnit.GB -> TransferUnit.MB
        TransferUnit.MB -> TransferUnit.KB
        TransferUnit.KB -> TransferUnit.BYTE
        else -> throw IllegalStateException("Bytes payload too small: $this")
      }

  @CheckResult
  fun previousSmallest(): TransferUnit =
      when (this) {
        TransferUnit.BYTE -> TransferUnit.KB
        TransferUnit.KB -> TransferUnit.MB
        TransferUnit.MB -> TransferUnit.GB
        TransferUnit.GB -> TransferUnit.TB
        TransferUnit.TB -> TransferUnit.PB
        else -> throw IllegalStateException("Bytes payload too big: $this")
      }
}
