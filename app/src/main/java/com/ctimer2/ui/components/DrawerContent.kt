package com.ctimer2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ctimer2.data.SessionEntity
import com.ctimer2.ui.ListViewMode
import com.ctimer2.ui.theme.DragonCatBlue

@Composable
fun DrawerContent(
    sessions: List<SessionEntity>,
    currentSessionId: Long?,
    viewMode: ListViewMode,
    countOf: (Long) -> Int,
    onSelect: (Long) -> Unit,
    onSelectView: (ListViewMode) -> Unit,
    onOpenSummary: () -> Unit,
    onOpenFormula: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onCreate: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuSession by remember { mutableStateOf<SessionEntity?>(null) }
    var showNewDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<SessionEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<SessionEntity?>(null) }

    FrostedGlass(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
        tintAlphaShift = 0.08f
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "功能",
            style = MaterialTheme.typography.labelSmall,
            color = DragonCatBlue.TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        DrawerEntryItem(
            title = "成绩总评",
            subtitle = "最佳 / 平均 / 稳定性",
            selected = false,
            onClick = { onOpenSummary() },
            icon = { IconSummary() }
        )
        Spacer(Modifier.height(4.dp))
        DrawerEntryItem(
            title = "公式速记",
            subtitle = "七步口诀与字母对照",
            selected = false,
            onClick = { onOpenFormula() },
            icon = { IconFormula() }
        )
        Spacer(Modifier.height(4.dp))
        DrawerEntryItem(
            title = "云端排行",
            subtitle = "GitHub Issues 云端成绩",
            selected = false,
            onClick = { onOpenLeaderboard() },
            icon = { IconCloud() }
        )

        Spacer(Modifier.height(14.dp))
        Text(
            text = "列表",
            style = MaterialTheme.typography.labelSmall,
            color = DragonCatBlue.TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(sessions, key = { it.id }) { session ->
                Box {
                    SessionItem(
                        session = session,
                        count = countOf(session.id),
                        selected = session.id == currentSessionId,
                        onClick = { onSelect(session.id) },
                        onLongClick = { menuSession = session }
                    )
                    DropdownMenu(
                        expanded = menuSession?.id == session.id,
                        onDismissRequest = { menuSession = null }
                    ) {
                        DropdownMenuItem(
                            text = { Text("重命名") },
                            onClick = {
                                renameTarget = session
                                menuSession = null
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("删除", color = DragonCatBlue.ResultWorse) },
                            onClick = {
                                deleteTarget = session
                                menuSession = null
                            }
                        )
                    }
                }
            }
        }

        Button(
            onClick = { showNewDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DragonCatBlue.PrimaryBlue)
        ) {
            Text("＋ 新建列表", color = DragonCatBlue.TimerText)
        }
    }

    // 新建列表对话框
    if (showNewDialog) {
        NameInputDialog(
            title = "新建列表",
            initial = "",
            confirmLabel = "创建",
            onConfirm = { name ->
                onCreate(name)
                showNewDialog = false
            },
            onDismiss = { showNewDialog = false }
        )
    }

    // 重命名对话框
    renameTarget?.let { target ->
        NameInputDialog(
            title = "重命名",
            initial = target.name,
            confirmLabel = "保存",
            onConfirm = { name ->
                onRename(target.id, name)
                renameTarget = null
            },
            onDismiss = { renameTarget = null }
        )
    }

    // 删除二次确认
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除列表") },
            text = { Text("确定删除「${target.name}」及其全部成绩吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target.id)
                    deleteTarget = null
                }) { Text("删除", color = DragonCatBlue.ResultWorse) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }
    }
}

@Composable
private fun NameInputDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var field by remember { mutableStateOf(TextFieldValue(initial)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = field,
                onValueChange = { field = it },
                singleLine = true,
                placeholder = { Text("输入列表名称") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (field.text.isNotBlank()) onConfirm(field.text) }
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
