package com.example.asoul.data.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.asoul.R

/** 成员所属期别。 */
enum class Cohort { FIRST, SECOND, OFFICIAL }

/**
 * 固定 UP 主数据模型。
 *
 * 设计原则（来自产品方案）：UP 主列表硬编码内置，保证离线可用；
 * 后续 P2 阶段接入云端配置实现热更新。
 */
data class Member(
    val id: String,
    val name: String,
    /** 成员卡片上的附加显示文本（英文名 / 身份说明）。 */
    val subText: String = "",
    val uid: Long? = null,
    val roomId: Long? = null,
    val cohort: Cohort,
    val color: Color,
    val emoji: String = "",
    /** 昵称别名，用于 OCR 结果模糊匹配。 */
    val aliases: List<String> = emptyList(),
    /** 成员头像图片资源（res/drawable/avatar_*.png），null 时用 emoji 占位。 */
    @DrawableRes val avatarRes: Int? = null,
)

/** 内置成员库：Asoul 一期 + 二期 + 官方账号。 */
object MemberCatalog {

    /** 团播虚拟成员（周程表中「团播/特别企划」行会匹配到这里）。 */
    val GROUP = Member(
        id = "group",
        name = "团播",
        subText = "全体企划",
        cohort = Cohort.OFFICIAL,
        color = Color(0xFF8E7CC3),
        emoji = "\uD83C\uDF1F",
        aliases = listOf(
            "团播", "全员", "A-SOUL", "ASOUL", "asoul", "枝江", "枝江综艺", "综艺",
            "特别企划", "聊天室",
        ),
    )

    val OFFICIAL = Member(
        id = "official",
        name = "Asoul_Official",
        subText = "官方动态 · 周程表发布源",
        uid = 700858850L,
        cohort = Cohort.OFFICIAL,
        color = Color(0xFF607D8B),
        emoji = "\uD83D\uDCE2",
        aliases = listOf("枝江娱乐", "官方", "Official"),
    )

    val ALL: List<Member> = listOf(
        Member(
            id = "bella",
            name = "贝拉",
            subText = "Bella",
            uid = 672353429L,
            roomId = 22632424L,
            cohort = Cohort.FIRST,
            // 官方周程表海报色
            color = Color(0xFFDB7D74),
            emoji = "\uD83C\uDF39",
            aliases = listOf("贝拉", "Bella", "bella", "贝拉Bella", "贝贝", "拉姐"),
            avatarRes = R.drawable.avatar_bella,
        ),
        Member(
            id = "diana",
            name = "嘉然",
            subText = "Diana",
            uid = 672328094L,
            roomId = 22637261L,
            cohort = Cohort.FIRST,
            // 官方周程表海报色
            color = Color(0xFFFCB97C),
            emoji = "\uD83C\uDF3B",
            aliases = listOf("嘉然", "Diana", "diana", "嘉然Diana", "然然", "然姐"),
            avatarRes = R.drawable.avatar_diana,
        ),
        Member(
            id = "eileen",
            name = "乃琳",
            subText = "Eileen",
            uid = 672342685L,
            roomId = 22634198L,
            cohort = Cohort.FIRST,
            // 官方周程表海报色
            color = Color(0xFF57A9A7),
            emoji = "\uD83C\uDF19",
            aliases = listOf("乃琳", "Eileen", "eileen", "乃琳Eileen", "乃宝"),
            avatarRes = R.drawable.avatar_eileen,
        ),
        // 二期成员（UID / 直播间 ID 待官宣后补充）
        Member(
            id = "sinuo",
            name = "思诺",
            subText = "二期生",
            cohort = Cohort.SECOND,
            roomId = 30858592L,
            // 应援色 #7252C0
            color = Color(0xFF7252C0),
            emoji = "\uD83C\uDF80",
            aliases = listOf("思诺", "Sinuo", "sinuo", "诺宝"),
            avatarRes = R.drawable.avatar_sinuo,
        ),
        Member(
            id = "xinyi",
            name = "心宜",
            subText = "二期生",
            cohort = Cohort.SECOND,
            roomId = 30849777L,
            // 应援色 #C93773
            color = Color(0xFFC93773),
            emoji = "\uD83D\uDCAB",
            aliases = listOf("心宜", "Xinyi", "xinyi", "宜宝"),
            avatarRes = R.drawable.avatar_xinyi,
        ),
        GROUP,
        OFFICIAL,
    )

    val FIRST_GEN: List<Member> = ALL.filter { it.cohort == Cohort.FIRST }

    val SECOND_GEN: List<Member> = ALL.filter { it.cohort == Cohort.SECOND }

    /** 可被排期的成员（成员选择器使用）。 */
    val SCHEDULABLE: List<Member> = FIRST_GEN + SECOND_GEN + GROUP

    /**
     * 团播分组的参与成员：
     * - [GroupType.ASOUL]：一期全员
     * - [GroupType.XINYI_SINUO]：心宜 & 思诺
     * - [GroupType.ZHIJIANG_VARIETY]：一期 + 二期共同参与
     */
    fun participantsOf(groupType: GroupType): List<Member> = when (groupType) {
        GroupType.NONE -> emptyList()
        GroupType.ASOUL -> FIRST_GEN
        GroupType.XINYI_SINUO -> SECOND_GEN
        GroupType.ZHIJIANG_VARIETY -> FIRST_GEN + SECOND_GEN
    }

    // ===== P6：服务端 member key → App 内置成员 id 映射 =====

    /**
     * 服务端静态 JSON（latest.json / flash.json）中的 member 枚举：
     * bella / jiaran / nailin / xinyi / sinuo / unknown。
     *
     * 与服务端约定不同的 key：嘉然=jiaran→diana、乃琳=nailin→eileen。
     * `unknown` 或未知值返回 null（UI 层用 title 或「未知成员」兜底展示）。
     */
    private val SERVER_KEY_TO_ID = mapOf(
        "bella" to "bella",
        "jiaran" to "diana",
        "nailin" to "eileen",
        "xinyi" to "xinyi",
        "sinuo" to "sinuo",
    )

    /** 将服务端 member key 映射为内置成员 id；`unknown`/未知值返回 null。 */
    fun memberIdFromServerKey(serverKey: String?): String? =
        SERVER_KEY_TO_ID[serverKey?.lowercase()?.trim()]
}
