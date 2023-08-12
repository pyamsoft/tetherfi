/*
 * Copyright 2023 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration

fun AnnotatedString.Builder.appendLink(
    tag: String,
    linkColor: Color,
    text: String,
    url: String,
) {
  val startIndex = this.length
  append(text)
  val endIndex = this.length

  addStyle(
      style =
          SpanStyle(
              color = linkColor,
              textDecoration = TextDecoration.Underline,
          ),
      start = startIndex,
      end = endIndex,
  )

  addStringAnnotation(
      tag = tag,
      annotation = url,
      start = startIndex,
      end = endIndex,
  )
}
