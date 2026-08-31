package com.example.asoul.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.asoul.R
import com.example.asoul.data.model.Cohort
import com.example.asoul.data.model.GroupType
import com.example.asoul.data.model.Member
import com.example.asoul.data.model.MemberCatalog
import com.example.asoul.data.model.ScheduleFilter

/**
 * 日历主界面成员选择行（原「成员」底部导航页整合至此）。
 *
 * 横向可滚动的头像列表：
 * - 「全部」入口
 * - 5 位成员头像（一期 + 二期）
 * - 团播分组头像（Asoul团播 / 心宜思诺团播 / 枝江综艺）
 *
 * 点击成员头像 → 日历仅显示该成员的单播及其参与的团播；
 * 点击团播头像 → 仅显示对应团播日程；再次点击取消过滤。
 */
@Composable
fun MemberSelectorRow(
    filter: ScheduleFilter,
    onFilterChange: (ScheduleFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 「全部」入口
        item(key = "all") {
            AllChip(isSelected = filter == ScheduleFilter.All) {
                onFilterChange(ScheduleFilter.All)
            }
        }
        // 成员头像
        MemberCatalog.SCHEDULABLE.filter { it.cohort != Cohort.OFFICIAL }
            .forEach { member ->
                item(key = member.id) {
                    MemberChip(
                        member = member,
                        isSelected = (filter is ScheduleFilter.MemberFilter && filter.memberId == member.id),
                    ) {
                        val next = if (filter is ScheduleFilter.MemberFilter && filter.memberId == member.id) {
                            ScheduleFilter.All
                        } else {
                            ScheduleFilter.MemberFilter(member.id)
                        }
                        onFilterChange(next)
                    }
                }
            }
        // 团播分组头像
        listOf(GroupType.ASOUL, GroupType.XINYI_SINUO, GroupType.ZHIJIANG_VARIETY).forEach { group ->
            item(key = "group_${group.name}") {
                GroupChip(
                    groupType = group,
                    isSelected = (filter is ScheduleFilter.GroupFilter && filter.groupType == group),
                ) {
                    val next = if (filter is ScheduleFilter.GroupFilter && filter.groupType == group) {
                        ScheduleFilter.All
                    } else {
                        ScheduleFilter.GroupFilter(group)
                    }
                    onFilterChange(next)
                }
            }
        }
    }
}

/** 「全部」圆形入口。 */
@Composable
private fun AllChip(isSelected: Boolean, onClick: () -> Unit) {
    val ringColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(200),
        label = "allRing",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = ringColor,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "\uD83D\uDCC5", fontSize = 22.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "全部",
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/** 成员头像入口。 */
@Composable
private fun MemberChip(member: Member, isSelected: Boolean, onClick: () -> Unit) {
    val ringColor by animateColorAsState(
        targetValue = if (isSelected) member.color else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(200),
        label = "memberRing",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        MemberAvatar(
            member = member,
            modifier = Modifier
                .size(48.dp)
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = ringColor,
                    shape = CircleShape,
                ),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = member.name,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) member.color else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 团播分组头像入口。 */
@Composable
private fun GroupChip(groupType: GroupType, isSelected: Boolean, onClick: () -> Unit) {
    val accent = Color(0xFF8E7CC3)
    val ringColor by animateColorAsState(
        targetValue = if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(200),
        label = "groupRing",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        val avatarRes = groupType.avatarRes
        if (avatarRes != null) {
            Image(
                painter = painterResource(avatarRes),
                contentDescription = "${groupType.label}头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = ringColor,
                        shape = CircleShape,
                    ),
            )
        } else {
            // 枝江综艺暂无专属头像，用 logo 兜底
            Image(
                painter = painterResource(R.drawable.logo_zhijiang),
                contentDescription = "${groupType.label}头像",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.2f))
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = ringColor,
                        shape = CircleShape,
                    ),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = groupType.label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
