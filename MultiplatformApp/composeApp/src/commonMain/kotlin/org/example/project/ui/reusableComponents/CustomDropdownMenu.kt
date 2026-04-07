package org.example.project.ui.reusableComponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.example.project.TUMColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> CustomDropdownMenu(
    onItemSelected: (T?) -> Unit,
    items: List<T>,
    selectedItem: T?,
    menuTitle: String,
    dropdownTitle: String,
    itemLabel: (T?) -> String,
    isNoneOption: Boolean = true,
    showRedStar: Boolean = true,
    selectedItems: List<T> = emptyList(),
    isMultiSelect: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    menuTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )

                if (showRedStar) {
                    Text("*", color = Color.Red, modifier = Modifier.padding(start = 2.dp))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = itemLabel(selectedItem),
                    color = TUMColors.tumBlue,
                    style = MaterialTheme.typography.bodyMedium
                )
                Icon(
                    imageVector = Icons.Default.UnfoldMore,
                    contentDescription = null,
                    tint = TUMColors.tumBlue,
                    modifier = Modifier.size(24.dp).padding(start = 4.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize(),
            shape = RoundedCornerShape(24.dp),
            containerColor = TUMColors.tumGray9,
        ) {
            if (isNoneOption) {
                DropdownMenuItem(
                    text = { Text(dropdownTitle) },
                    onClick = {
                        onItemSelected(null)
                        if (!isMultiSelect) expanded = false
                    },
                    leadingIcon = {
                        if (selectedItem == null && (selectedItems.isEmpty() || !isMultiSelect)) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )
            }
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemLabel(item)) },
                    onClick = {
                        onItemSelected(item)
                        if (!isMultiSelect) expanded = false
                    },
                    leadingIcon = {
                        val isSelected = if (isMultiSelect) {
                            selectedItems.contains(item)
                        } else {
                            selectedItem == item
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun MessageCardAllTypesPreview() {
    MaterialTheme {
        Surface(color = TUMColors.tumGray9) {
            CustomDropdownMenu(
                onItemSelected = {},
                items = listOf("Option 1", "Option 2", "Option 3"),
                selectedItem = "Option 1",
                menuTitle = "Menu Title",
                dropdownTitle = "Dropdown Title",
                itemLabel = { it ?: "None" },
            )
        }
    }
}
