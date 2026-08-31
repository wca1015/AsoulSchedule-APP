package com.example.asoul.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.asoul.data.model.Member

/**
 * 唤起 bilibili 客户端打开直播间。
 *
 * 优先使用 B 站 App 的 scheme（`bilibili://live/{roomId}`）直接拉起客户端；
 * 若未安装 B 站或 scheme 无法解析，则回退到浏览器打开直播间网页版。
 *
 * 注意：不使用 resolveActivity 预检 —— Android 11+ 的包可见性限制会导致其返回 null，
 * 直接 startActivity + catch [ActivityNotFoundException] 是更可靠的做法。
 */
object BilibiliLauncher {

    /** B 站客户端直播间 scheme。 */
    private fun liveScheme(roomId: Long): String = "bilibili://live/$roomId"

    /** 网页版直播间兜底地址。 */
    private fun liveWebUrl(roomId: Long): String = "https://live.bilibili.com/$roomId"

    /** B 站客户端视频（录像）scheme。 */
    private fun videoScheme(bvid: String): String = "bilibili://video/$bvid"

    /** 网页版视频（录像）兜底地址。 */
    private fun videoWebUrl(bvid: String): String = "https://www.bilibili.com/video/$bvid"

    /**
     * 打开指定成员的直播间。
     *
     * @return 是否成功唤起（未配置直播间 id 时返回 false 并提示）。
     */
    fun openLiveRoom(context: Context, member: Member?): Boolean {
        val roomId = member?.roomId
        if (roomId == null) {
            Toast.makeText(context, "该成员暂未配置直播间 ID", Toast.LENGTH_SHORT).show()
            return false
        }
        // 1. 优先尝试 B 站客户端
        if (tryLaunch(context, liveScheme(roomId))) return true
        // 2. 兜底：浏览器打开网页版
        if (tryLaunch(context, liveWebUrl(roomId))) {
            Toast.makeText(context, "未检测到 B 站客户端，已用浏览器打开", Toast.LENGTH_SHORT).show()
            return true
        }
        Toast.makeText(context, "无法打开直播间", Toast.LENGTH_SHORT).show()
        return false
    }

    /**
     * 打开直播录像（BV 号）。
     *
     * 与 [openLiveRoom] 相同的策略：优先 `bilibili://video/{bvid}` 唤起客户端，
     * 未安装时回退浏览器打开 `bilibili.com/video/{bvid}` 网页版。
     */
    fun openRecording(context: Context, bvid: String?): Boolean {
        if (bvid.isNullOrBlank()) {
            Toast.makeText(context, "该直播暂无录像", Toast.LENGTH_SHORT).show()
            return false
        }
        if (tryLaunch(context, videoScheme(bvid))) return true
        if (tryLaunch(context, videoWebUrl(bvid))) {
            Toast.makeText(context, "未检测到 B 站客户端，已用浏览器打开", Toast.LENGTH_SHORT).show()
            return true
        }
        Toast.makeText(context, "无法打开录像", Toast.LENGTH_SHORT).show()
        return false
    }

    /**
     * 打开 B 站动态（P10 突击直播卡片点击入口）。
     *
     * 与 [openLiveRoom] 相同的策略：
     * 1. 从链接尾部提取动态 id，优先以 `bilibili://feed/{dynamicId}` 唤起 B 站客户端；
     * 2. scheme 不可用时回退直接打开原链接（https 网页，若客户端已注册对应
     *    intent-filter 也可能由 B 站 App 接管）。
     */
    fun openDynamic(context: Context, url: String?): Boolean {
        if (url.isNullOrBlank()) {
            Toast.makeText(context, "暂无动态链接", Toast.LENGTH_SHORT).show()
            return false
        }
        // 提取链接末段纯数字的动态 id（如 https://t.bilibili.com/328174562817）
        val dynamicId = url.substringAfterLast('/', "")
            .takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
        if (dynamicId != null && tryLaunch(context, dynamicScheme(dynamicId))) return true
        if (tryLaunch(context, url)) {
            Toast.makeText(context, "未检测到 B 站客户端，已用浏览器打开", Toast.LENGTH_SHORT).show()
            return true
        }
        Toast.makeText(context, "无法打开动态", Toast.LENGTH_SHORT).show()
        return false
    }

    /** B 站客户端动态（feed）scheme。 */
    private fun dynamicScheme(dynamicId: String): String = "bilibili://feed/$dynamicId"

    private fun tryLaunch(context: Context, uri: String): Boolean = try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
