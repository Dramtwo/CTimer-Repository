package com.ctimer2.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.RectangleShape
import com.ctimer2.ui.theme.DragonCatBlue

private data class MoveMap(val word: String, val letter: String, val desc: String)

private data class Formula(
    val title: String,
    val step: String,
    val chant: String,
    val letters: String,
    val tip: String
)

private val MOVE_MAP = listOf(
    MoveMap("上", "R", "右层顺时针"),
    MoveMap("下", "R'", "右层逆时针"),
    MoveMap("左", "U'", "顶层向左"),
    MoveMap("右", "U", "顶层向右"),
    MoveMap("勾", "U", "顶层顺时针（食指勾）"),
    MoveMap("回", "U'", "顶层逆时针"),
    MoveMap("顺", "F", "前面顺时针"),
    MoveMap("逆", "F'", "前面逆时针"),
    MoveMap("提", "B", "后面顺时针（向上提）"),
    MoveMap("压", "B'", "后面逆时针（向下压）"),
    MoveMap("拨", "U'", "顶层逆时针（拨一下）"),
    MoveMap("上上 / 顺顺", "R2 / F2", "连转两下")
)

private val FORMULAS = listOf(
    Formula(
        step = "第二步",
        title = "底层白色面 + 第一层",
        chant = "上左下右",
        letters = "R U R' U'",
        tip = "白角块在右侧做 1 遍、在上方做 3 遍、正对自己做 5 遍。"
    ),
    Formula(
        step = "第三步",
        title = "第二层棱块",
        chant = "勾上回下",
        letters = "U R U' R'（右手）／U' L U L'（左手）",
        tip = "看顶层棱块正上方的颜色倒向哪侧：倒左用左手、倒右用右手；出现「白色小火车」时，车头朝自己，另一只手再补一次。"
    ),
    Formula(
        step = "第四步",
        title = "顶面黄十字",
        chant = "顺勾上拨下逆",
        letters = "F U R U' R' F'",
        tip = "顶面一个黄点做 3 遍、小拐弯做 2 遍、一字横着做 1 遍；做完要回到原朝向。"
    ),
    Formula(
        step = "第五步",
        title = "顶面铺满黄色（小鱼公式）",
        chant = "上勾下勾上勾勾下",
        letters = "R U R' U R U2 R'",
        tip = "鱼头放左下角；若黄色朝左的是相反小鱼，用左手镜像：上回下回上回回下（L' U' L U' L' U2 L）。"
    ),
    Formula(
        step = "第六步",
        title = "顶层角块归位",
        chant = "下顺下提提　上逆下提提　顺顺拨",
        letters = "R' F R' B2　R F' R' B2　R2 U'",
        tip = "先把已对好的一个角柱放后层，连做直到四个角柱都建成。"
    ),
    Formula(
        step = "第七步",
        title = "顶层棱块归位",
        chant = "顺顺勾移窗向左",
        letters = "F2 U L R' F2 L' R U F2",
        tip = "窗口要往右移时改用：顺顺拨移窗向右（F2 U' L R' F2 L' R U' F2）。"
    )
)

/** 公式速记独立页面 */
@Composable
fun FormulaPage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    FrostedGlass(
        modifier = modifier.fillMaxSize(),
        shape = RectangleShape,
        showBorder = false,
        tintAlphaShift = -0.14f
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DragonCatBlue.CardSurface)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                BackArrow(tint = DragonCatBlue.TextPrimary)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("公式速记", style = MaterialTheme.typography.titleLarge, color = DragonCatBlue.TextPrimary)
                Text("三阶入门七步 · 中文口诀对照", style = MaterialTheme.typography.labelSmall, color = DragonCatBlue.TextSecondary)
            }
        }

        Spacer(Modifier.height(18.dp))

        // 口令对照表
        SectionTitle("口令对照")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp)
        ) {
            MOVE_MAP.forEach { m ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        m.word,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DragonCatBlue.PrimaryBlue,
                        modifier = Modifier.width(78.dp)
                    )
                    Text(
                        m.letter,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DragonCatBlue.TextPrimary,
                        modifier = Modifier.width(62.dp)
                    )
                    Text(
                        m.desc,
                        style = MaterialTheme.typography.labelSmall,
                        color = DragonCatBlue.TextSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionTitle("七步公式")

        FORMULAS.forEach { f ->
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DragonCatBlue.CardSurface)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DragonCatBlue.PrimaryBlue)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            f.step,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        f.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DragonCatBlue.TextPrimary
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    f.chant,
                    style = MaterialTheme.typography.titleMedium,
                    color = DragonCatBlue.PrimaryBlue
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    f.letters,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DragonCatBlue.TextPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    f.tip,
                    style = MaterialTheme.typography.labelSmall,
                    color = DragonCatBlue.TextSecondary
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "口诀为常见民间叫法，标准字母按 CFOP/层先法记法；不同教程可能把「勾」与「回」对调，以实际手法为准。",
            style = MaterialTheme.typography.labelSmall,
            color = DragonCatBlue.TextSecondary
        )
        Spacer(Modifier.height(24.dp))
    }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = DragonCatBlue.TextSecondary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun BackArrow(tint: Color) {
    Canvas(Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(w * 0.62f, 0f),
            end = androidx.compose.ui.geometry.Offset(w * 0.22f, h / 2f),
            strokeWidth = stroke
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(w * 0.22f, h / 2f),
            end = androidx.compose.ui.geometry.Offset(w * 0.62f, h),
            strokeWidth = stroke
        )
    }
}
