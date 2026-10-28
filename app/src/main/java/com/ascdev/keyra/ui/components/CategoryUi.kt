package com.ascdev.keyra.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ascdev.keyra.R
import com.ascdev.keyra.domain.model.Category

val Category.icon: ImageVector
    get() = when (this) {
        Category.LOGIN -> Icons.Outlined.Language
        Category.EMAIL -> Icons.Outlined.Email
        Category.SOCIAL -> Icons.Outlined.People
        Category.FINANCE -> Icons.Outlined.AccountBalance
        Category.WORK -> Icons.Outlined.Work
        Category.OTHER -> Icons.Outlined.Key
    }

@get:StringRes
val Category.label: Int
    get() = when (this) {
        Category.LOGIN -> R.string.category_login
        Category.EMAIL -> R.string.category_email
        Category.SOCIAL -> R.string.category_social
        Category.FINANCE -> R.string.category_finance
        Category.WORK -> R.string.category_work
        Category.OTHER -> R.string.category_other
    }

private val Category.tint: Color
    get() = when (this) {
        Category.LOGIN -> Color(0xFF3D5AFE)
        Category.EMAIL -> Color(0xFF7C4DFF)
        Category.SOCIAL -> Color(0xFFEC407A)
        Category.FINANCE -> Color(0xFF00BFA5)
        Category.WORK -> Color(0xFF29B6F6)
        Category.OTHER -> Color(0xFF8D93B8)
    }

@Composable
fun CategoryBadge(category: Category, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .background(category.tint.copy(alpha = 0.18f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(category.icon, contentDescription = null, tint = category.tint)
    }
}
