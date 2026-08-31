package com.example.asoul.ui.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.asoul.data.model.GroupType
import com.example.asoul.data.model.LiveSchedule
import com.example.asoul.data.model.Member
import com.example.asoul.data.model.MemberCatalog
import com.example.asoul.data.model.Weeks
import com.example.asoul.ui.theme.AsoulColors
import com.example.asoul.util.BilibiliLauncher
import java.time.format.DateTimeFormatter

private val DETAIL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy年M月d日")

/**
 * 直播详情弹窗：点击日历中的直播卡片唤起。
 *
 * 内容：
 * - 头部：团播/成员头像 + 标题 + 时间日期
 * - 直播形式 / 团播分组 / 来源信息
 * - 团播时展示参与成员头像列表（点击头像 → 唤起 B 站对应直播间）
 * - 「打开直播间」按钮：单人直播直接打开该成员直播间
 */
@Composable
fun ScheduleDetailDialog(
    schedule: LiveSchedule,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val member = schedule.member(MemberCatalog.ALL)
    val accent = when (schedule.groupType) {
        GroupType.XINYI_SINUO ->
            MemberCatalog.SECOND_GEN.firstOrNull { it.id == "xinyi" }?.color ?: Color.Gray
        GroupType.ASOUL -> Color(0xFF8E7CC3)
        GroupType.ZHIJIANG_VARIETY -> AsoulColors.BadgeGroup
        GroupType.NONE -> member?.color ?: Color.Gray
    }
    val participants = MemberCatalog.participantsOf(schedule.groupType)

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
                        text = "直播详情",
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

                // ===== 头部：头像 + 标题 + 副标题 =====
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 头像
                    val avatarRes = schedule.groupType.avatarRes ?: member?.avatarRes
                    if (avatarRes != null) {
                        Image(
                            painter = painterResource(avatarRes),
                            contentDescription = "${schedule.memberName}头像",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(2.dp, accent, CircleShape),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = member?.emoji ?: "🌟", fontSize = 26.sp)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = schedule.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = schedule.memberName,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ===== 信息区 =====
                DetailRow("时间", "${schedule.date.format(DETAIL_DATE_FORMAT)} ${Weeks.weekdayLabel(schedule.date)} ${schedule.time.format(DateTimeFormatter.ofPattern("HH:mm"))}")
                DetailRow(
                    "类型",
                    if (schedule.isGroupLive) "团播 · ${schedule.groupType.label}" else "单播",
                )
                if (schedule.formatTag != null) {
                    DetailRow("形式", "${schedule.formatTag!!.emoji} ${schedule.formatTag!!.label}")
                }
                DetailRow("来源", schedule.source.label)

                // ===== 团播参与成员 =====
                if (participants.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "参与成员（点击头像打开直播间）",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        participants.forEach { p ->
                            ParticipantAvatar(
                                member = p,
                                onClick = { BilibiliLauncher.openLiveRoom(context, p) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ===== 打开直播间按钮 =====
                // 单人直播 → 该成员直播间；团播 → 首个已配置房间号的参与成员直播间，
                // 也可点击上方参与成员头像直接跳转各自直播间。
                val targetMember = if (schedule.isGroupLive) {
                    participants.firstOrNull { it.roomId != null }
                } else {
                    member
                }
                Button(
                    onClick = { BilibiliLauncher.openLiveRoom(context, targetMember) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            (!schedule.isGroupLive && targetMember?.roomId != null) ->
                                "打开 ${targetMember.name} 的直播间"
                            schedule.isGroupLive -> "打开团播直播间"
                            else -> "打开直播间"
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/** 详情键值行。 */
@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp),
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** 参与成员小头像（可点击跳转直播间）。 */
@Composable
private fun ParticipantAvatar(member: Member, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        val avatarRes = member.avatarRes
        if (avatarRes != null) {
            Image(
                painter = painterResource(avatarRes),
                contentDescription = "${member.name}头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, member.color.copy(alpha = 0.6f), CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(member.color.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = member.emoji.ifBlank { "🎤" }, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = member.name,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
