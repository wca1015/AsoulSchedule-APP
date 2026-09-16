package com.example.asoul.ui.dialog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.asoul.BuildConfig
import com.example.asoul.util.BilibiliLauncher

/** 鸣谢 & 站点地址（可点击跳转）。 */
private const val ASOUL_LOVE_URL = "https://asoul.love"

/** 反馈邮箱（点击唤起邮件应用；无邮件应用时复制到剪贴板）。 */
private const val FEEDBACK_EMAIL = "1015249947@qq.com"

/** 反馈用 B 站 UID（点击打开个人空间）。 */
private const val FEEDBACK_BILI_UID = 1474801L

/** 唤起邮件应用；无可用邮件应用时复制邮箱到剪贴板并提示。 */
private fun sendEmail(context: Context, email: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val launched = runCatching { context.startActivity(intent) }.isSuccess
    if (!launched) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("邮箱", email))
        Toast.makeText(context, "未找到邮件应用，邮箱已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }
}

/**
 * 「关于」弹窗（Header 右上角「i」入口唤起）：
 *
 * - 应用名与版本（BuildConfig）
 * - 数据来源说明：自建服务端（OSS 静态数据）+ 枝江站 asoul.love（额外数据支持）
 * - 特别鸣谢（可点击打开 asoul.love）
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // ===== 标题栏 =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "关于",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(text = "Asoul · 枝江日历", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "版本 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )

                Spacer(Modifier.height(16.dp))
                Text(text = "数据来源", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "· 周程表 / 突击直播 / 录播回填：自建服务端（阿里云 OSS 静态数据）",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "· 突击预告与直播类型标签：枝江站（asoul.love 日历订阅）",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(16.dp))
                // 特别鸣谢（点击打开站点）
                Text(
                    text = "特别鸣谢枝江站 https://asoul.love 提供的额外数据支持",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { BilibiliLauncher.openUrl(context, ASOUL_LOVE_URL) }
                        .padding(vertical = 6.dp),
                )

                Spacer(Modifier.height(8.dp))
                Text(text = "反馈与联系", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "· 邮箱：$FEEDBACK_EMAIL（点击发送邮件）",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { sendEmail(context, FEEDBACK_EMAIL) }
                        .padding(vertical = 4.dp),
                )
                Text(
                    text = "· B 站：UID $FEEDBACK_BILI_UID（点击打开个人空间）",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { BilibiliLauncher.openSpace(context, FEEDBACK_BILI_UID) }
                        .padding(vertical = 4.dp),
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    text = "本应用为粉丝自制工具，与 A-SOUL 官方及相关企业无关；" +
                        "数据来自公开动态，版权归原作者所有。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }
        }
    }
}
