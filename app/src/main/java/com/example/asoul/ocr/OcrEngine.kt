package com.example.asoul.ocr

import android.graphics.Rect

/**
 * OCR 识别出的单个文本块：文字内容 + 坐标位置 + 置信度。
 * 与 MLKit TextRecognition 的输出结构对齐。
 */
data class TextBlock(
    val text: String,
    val boundingBox: Rect? = null,
    val confidence: Float = 1f,
)

/**
 * OCR 引擎抽象接口。
 *
 * P0 MVP 阶段提供 [FakeOcrEngine]（离线演示用）；
 * P1 接入 Google MLKit（方案中的首选端侧方案），
 * P2 再补充云端多模态大模型兜底实现，UI 层无感知。
 */
interface OcrEngine {
    /** 引擎展示名称，用于设置页选择。 */
    val name: String

    /** 对图片字节流做 OCR，返回结构化文本块。 */
    suspend fun recognize(imageBytes: ByteArray): List<TextBlock>
}

/**
 * 演示引擎：不依赖 MLKit/网络即可跑通完整 Pipeline。
 * 返回一张「方案文档中的典型周程表」样例文本块，
 * 用于验证语义解析、确认编辑、写入日历全链路。
 */
class FakeOcrEngine : OcrEngine {
    override val name: String = "演示引擎（离线样例）"

    override suspend fun recognize(imageBytes: ByteArray): List<TextBlock> {
        val sample = listOf(
            "周一 20:00 贝拉 舞蹈练习",
            "周二 19:30 嘉然 歌回",
            "周三 20:00 Asoul团播 小剧场",
            "周四 — 休息 —",
            "周五 20:00 乃琳 工商直播",
            "周六 14:00 心宜 联动",
            "周六 20:00 Asoul团播 夜谈",
            "周六 21:00 Asoul团播 游戏室",
            "周日 19:00 心宜思诺团播 夜谈",
        )
        return sample.mapIndexed { index, line ->
            TextBlock(text = line, confidence = 0.97f - index * 0.001f)
        }
    }
}
