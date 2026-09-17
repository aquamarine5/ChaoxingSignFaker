/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingSignFakerDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.DataStoreTreeNode
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.buildDataStoreTree
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore

private val INDENTATION_WIDTH = 14.dp
private val INDICATOR_SIZE = 10.dp

@Composable
fun CurrentDataStoreDialog(onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    var dataStore by remember { mutableStateOf<ChaoxingSignFakerDataStore?>(null) }
    LaunchedEffect(Unit) {
        dataStore = withContext(Dispatchers.IO) {
            context.chaoxingDataStore.data.first()
        }
    }
    SnackbarAlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text("当前DataStore")
        },
        text = {
            val current = dataStore
            if (current == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                DataStoreTreeView(
                    nodes = remember(current) { buildDataStoreTree(current) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismissRequest) {
                Text("关闭")
            }
        }
    )
}

@Composable
fun DataStoreTreeView(
    nodes: List<DataStoreTreeNode>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .verticalScroll(rememberScrollState())
            .width(IntrinsicSize.Max)
    ) {
        nodes.forEach { node ->
            DataStoreTreeNodeRow(node, 0)
        }
    }
}

@Composable
private fun DataStoreTreeNodeRow(node: DataStoreTreeNode, depth: Int) {
    val textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
    when (node) {
        is DataStoreTreeNode.Value -> DataStoreTreeNodeValueRow(node, depth, textStyle)
        is DataStoreTreeNode.Group -> DataStoreTreeNodeGroupRow(node, depth, textStyle)
    }
}

@Composable
private fun DataStoreTreeNodeValueRow(
    node: DataStoreTreeNode.Value,
    depth: Int,
    textStyle: TextStyle
) {
    Row(
        modifier = Modifier.padding(start = INDENTATION_WIDTH * depth, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(INDENTATION_WIDTH))
        Text(
            text = "${node.title}=${node.value}",
            style = textStyle,
            softWrap = false,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DataStoreTreeNodeGroupRow(
    node: DataStoreTreeNode.Group,
    depth: Int,
    textStyle: TextStyle
) {
    val hapticFeedback = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    isExpanded = !isExpanded
                }
                .padding(start = INDENTATION_WIDTH * depth, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(INDICATOR_SIZE)
                    .rotate(if (isExpanded) -90f else 180f)
            )
            Spacer(modifier = Modifier.width(INDENTATION_WIDTH - INDICATOR_SIZE))
            Text(
                text = node.title,
                style = textStyle,
                softWrap = false,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(isExpanded) {
            Column {
                node.children.forEach { child ->
                    DataStoreTreeNodeRow(child, depth + 1)
                }
            }
        }
    }
}
