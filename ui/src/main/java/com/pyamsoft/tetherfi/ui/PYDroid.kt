package com.pyamsoft.tetherfi.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.widget.BillingUpsellWidget
import com.pyamsoft.pydroid.ui.widget.NewVersionWidget
import com.pyamsoft.pydroid.ui.widget.ShowChangeLogWidget
import com.pyamsoft.pydroid.ui.widget.ShowDataPolicyDialog
import com.pyamsoft.pydroid.ui.widget.UpdateProgressWidget

@Composable
fun InstallPYDroidExtras() {
  ShowDataPolicyDialog()
}

fun LazyListScope.renderPYDroidExtras() {
  item {
    UpdateProgressWidget(
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = MaterialTheme.keylines.content)
                .padding(top = MaterialTheme.keylines.content),
    )
  }

  item {
    NewVersionWidget(
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = MaterialTheme.keylines.content)
                .padding(top = MaterialTheme.keylines.content),
    )
  }

  item {
    ShowChangeLogWidget(
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = MaterialTheme.keylines.content)
                .padding(top = MaterialTheme.keylines.content),
    )
  }

  item {
    BillingUpsellWidget(
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = MaterialTheme.keylines.content)
                .padding(top = MaterialTheme.keylines.content),
    )
  }
}
