package com.example.asoul.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.asoul.data.model.MemberCombo
import com.example.asoul.data.model.ScheduleFilter

/**
 * 日历主界面成员选择行（原「成员」底部导航页整合至此）。
 *
 * 两行筛选（对齐 asoul.love 日历页的筛选维度）：
 * 1. 头像行：「全部」+ 5 位成员头像 + 团播分组头像——成员支持**多选**（点一下加入 / 再点取消），
 *    选中多人时显示「任一成员参与」的单播与团播（与对方页面的 include 语义一致）
 * 2. 组合行：枝江 / A-SOUL / 小心思 / 嘉贝 / 乃贝 / 琳嘉——点一下 = 同时选中多名成员
 */
@Composable
fun MemberSelectorRow(
    filter: ScheduleFilter,
    onFilterChange: (ScheduleFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedMemberIds = (filter as? ScheduleFilter.MemberFilter)?.memberIds.orEmpty()
    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 「全部」入口
            item(key = "all") {
                AllChip(isSelected = filter == ScheduleFilter.All) {
                    onFilterChange(ScheduleFilter.All)
                }
            }
            // 成员头像（多选开关）
            MemberCatalog.SCHEDULABLE.filter { it.cohort != Cohort.OFFICIAL }
                .forEach { member ->
                    item(key = member.id) {
                        MemberChip(
                            member = member,
                            isSelected = member.id in selectedMemberIds,
                        ) {
                            val next = if (member.id in selectedMemberIds) {
                                selectedMemberIds - member.id
                            } else {
                                selectedMemberIds + member.id
                            }
                            onFilterChange(
                                if (next.isEmpty()) ScheduleFilter.All
                                else ScheduleFilter.MemberFilter(next),
                            )
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
        // 组合快捷筛选（点一下 = 同时选中多名成员）
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MemberCatalog.COMBOS.forEach { combo ->
                item(key = "combo_${combo.id}") {
                    val isSelected = selectedMemberIds == combo.memberIds.toSet()
                    ComboChip(
                        combo = combo,
                        isSelected = isSelected,
                    ) {
                        onFilterChange(
                            if (isSelected) ScheduleFilter.All
                            else ScheduleFilter.MemberFilter(combo.memberIds.toSet()),
                        )
                    }
                }
            }
        }
    }
}

/** 组合快捷项胶囊（枝江 / A-SOUL / 小心思 / 嘉贝 / 乃贝 / 琳嘉）。 */
@Composable
private fun ComboChip(combo: MemberCombo, isSelected: Boolean, onClick: () -> Unit) {
    val accent = Color(0xFF8E7CC3)
    Surface(
        shape = RoundedCornerShape(50),
        color = if (isSelected) {
            accent.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = combo.label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
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
