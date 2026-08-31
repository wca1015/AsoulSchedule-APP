package com.example.asoul.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.asoul.data.model.Member

/**
 * 成员头像：优先使用真实头像图片（res/drawable/avatar_*.png），
 * 无图片资源时回退为 emoji + 成员色圆形背景。
 */
@Composable
fun MemberAvatar(member: Member, modifier: Modifier = Modifier) {
    val avatarRes = member.avatarRes
    if (avatarRes != null) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(avatarRes),
            contentDescription = "${member.name}头像",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(56.dp)
                .clip(CircleShape)
                .border(1.dp, member.color.copy(alpha = 0.5f), CircleShape),
        )
    } else {
        Box(
            modifier = modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(member.color.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = member.emoji.ifBlank { "\uD83C\uDFA4" }, fontSize = 28.sp)
        }
    }
}

/** 成员面板卡片（模块1）：头像、昵称、附加说明。 */
@Composable
fun MemberCard(member: Member, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = member.color.copy(alpha = 0.10f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MemberAvatar(member)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (member.subText.isNotBlank()) {
                    Text(
                        text = member.subText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (member.roomId != null) {
                Text(
                    text = "直播间\n${member.roomId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}
