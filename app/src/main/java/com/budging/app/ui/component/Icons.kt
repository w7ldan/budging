package com.budging.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.budging.app.ui.theme.AppPrimarySoft

data class CategoryAccent(
    val icon: ImageVector,
    val tint: Color,
    val background: Color,
)

data class CategoryIconOption(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

private val categoryIconOptions = listOf(
    CategoryIconOption("food", "Food", Icons.Default.Restaurant),
    CategoryIconOption("transport", "Transport", Icons.Default.DirectionsCar),
    CategoryIconOption("home", "Home", Icons.Default.Home),
    CategoryIconOption("utilities", "Utilities", Icons.Default.Bolt),
    CategoryIconOption("shopping", "Shopping", Icons.Default.ShoppingBag),
    CategoryIconOption("fun", "Fun", Icons.Default.Celebration),
    CategoryIconOption("health", "Health", Icons.Default.MedicalServices),
    CategoryIconOption("gym", "Gym", Icons.Default.FitnessCenter),
    CategoryIconOption("education", "Education", Icons.Default.School),
    CategoryIconOption("coffee", "Coffee", Icons.Default.LocalCafe),
    CategoryIconOption("travel", "Travel", Icons.Default.Flight),
    CategoryIconOption("savings", "Savings", Icons.Default.Savings),
    CategoryIconOption("other", "Other", Icons.Default.Category),
)

@Composable
fun CategoryIconBubble(
    categoryName: String,
    iconKey: String? = null,
    modifier: Modifier = Modifier,
    accent: CategoryAccent = categoryAccent(categoryName, iconKey),
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(accent.background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = accent.icon,
            contentDescription = null,
            tint = accent.tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

fun categoryAccent(name: String, iconKey: String? = null): CategoryAccent {
    val resolvedKey = resolveCategoryIconKey(name, iconKey)
    val icon = categoryIcon(resolvedKey)
    return CategoryAccent(
        icon = icon,
        tint = PrimaryText,
        background = AppPrimarySoft,
    )
}

fun categoryIcon(key: String?): ImageVector =
    categoryIconOptions.firstOrNull { it.key == key }?.icon ?: Icons.Default.Category

fun allCategoryIconOptions(): List<CategoryIconOption> = categoryIconOptions

fun resolveCategoryIconKey(name: String, iconKey: String? = null): String {
    if (iconKey != null && categoryIconOptions.any { it.key == iconKey }) return iconKey
    val lower = name.lowercase()
    return when {
        "food" in lower || "restaurant" in lower -> "food"
        "transport" in lower || "car" in lower || "commute" in lower -> "transport"
        "home" in lower || "rent" in lower -> "home"
        "utility" in lower || "electric" in lower || "water" in lower -> "utilities"
        "shop" in lower -> "shopping"
        "fun" in lower || "entertain" in lower -> "fun"
        "health" in lower || "doctor" in lower -> "health"
        "gym" in lower || "fitness" in lower -> "gym"
        "school" in lower || "education" in lower || "course" in lower -> "education"
        "coffee" in lower || "cafe" in lower -> "coffee"
        "travel" in lower || "flight" in lower || "holiday" in lower || "trip" in lower -> "travel"
        "save" in lower || "emergency fund" in lower -> "savings"
        else -> "other"
    }
}

private val PrimaryText = Color(0xFF131B2E)
