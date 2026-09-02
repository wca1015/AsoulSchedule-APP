package com.example.asoul.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.asoul.data.remote.dto.AppVersionDto

/**
 * 「发现新版本」更新弹窗。
 *
 * 两个明确动作：
 * - [立即更新]：触发下载并安装（[downloading] 期间禁用，避免重复下载）
 * - [跳过此版本]：记住该版本，之后不再提示
 * 通过返回键 / 点击弹窗外关闭 = 「稍后再说」（今日不再打扰，明天再提示）。
 */
@Composable
fun AppUpdateDialog(
    update: AppVersionDto,
    downloading: Boolean,
    onUpdate: () -> Unit,
    onSkipThisVersion: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("发现新版本 ${update.versionName}")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    "更新内容：",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    update.notes.ifBlank { "修复已知问题并优化体验" },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            Button(onClick = onUpdate, enabled = !downloading) {
                Text(if (downloading) "下载中…" else "立即更新")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkipThisVersion, enabled = !downloading) {
                Text("跳过此版本")
            }
        },
    )
}
