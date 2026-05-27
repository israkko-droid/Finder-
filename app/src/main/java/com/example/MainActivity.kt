package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.FavoriteRed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ==========================================
// 1. DOMAIN DATA MODELS
// ==========================================

data class MenuItem(
    val id: String,
    val name: String,
    val price: Double,
    val description: String,
    val isVegetarian: Boolean = false,
    val isSpicy: Boolean = false
)

data class Restaurant(
    val id: String,
    val name: String,
    val cuisine: String,
    val rating: Double,
    val description: String,
    val menu: List<MenuItem>,
    val priceRange: String = "$$",
    val reviewCount: Int = 124,
    val isFavorite: Boolean = false
)

// ==========================================
// 2. VIEWMODEL STATE ENGINE
// ==========================================

class RestaurantViewModel : ViewModel() {

    // Initial Seed Data matching user requested structures and expanded for gorgeous UI details
    private val initialRestaurants = listOf(
        Restaurant(
            id = "1",
            name = "Bella Italia",
            cuisine = "Italian",
            rating = 4.8,
            priceRange = "$$",
            reviewCount = 254,
            description = "Family-run trattoria serving authentic hand-tossed wood-fired pizzas, rich handmade pastas, and premium traditional desserts.",
            menu = listOf(
                MenuItem("it-1", "Pizza Margherita", 14.99, "Fresh mozzarella, crushed San Marzano sweet tomatoes, organic basil, and extra virgin olive oil.", isVegetarian = true),
                MenuItem("it-2", "Pasta Alfredo", 16.50, "Fresh fettuccine tossed in a rich, velvety Parmigiano-Reggiano cream and butter reduction sauce."),
                MenuItem("it-3", "Tiramisu", 8.25, "Espresso-dipped ladyfingers layered with fresh whipped mascarpone custard and dusted with sweet cocoa powder.", isVegetarian = true),
                MenuItem("it-4", "Bruschetta Pomodoro", 9.50, "Grilled sourdough rubbed with garlic cloves, topped with seasoned vine-ripe tomatoes and fresh basil pesto.", isVegetarian = true),
                MenuItem("it-5", "Gnocchi Sorrentina", 17.50, "Pillowy potato dumplings simmered with organic tomato compote, topped with melted fresh mozzarella.", isVegetarian = true)
            )
        ),
        Restaurant(
            id = "2",
            name = "Sultan Grill",
            cuisine = "Middle Eastern",
            rating = 4.7,
            priceRange = "$$",
            reviewCount = 189,
            description = "Rich traditional culinary specialties including charcoal smoke-grilled kebabs, gourmet artisan dips, and warm honey-glazed pastries.",
            menu = listOf(
                MenuItem("me-1", "Mixed Grill", 23.99, "Skewer trio of succulent lamb kebab, spiced chicken shish tawook, and seasoned beef kofta on saffron rice."),
                MenuItem("me-2", "Hummus Trio", 8.50, "Classic, garlic, and spicy red pepper hummus dips served alongside flame-baked puffy pita bread.", isVegetarian = true),
                MenuItem("me-3", "Baklava Pastry", 6.99, "Crisp golden layers of buttered phyllo dough stuffed with chopped walnuts and drizzled in rosewater honey.", isVegetarian = true),
                MenuItem("me-4", "Gourmet Falafel Plate", 13.50, "Crunchy spiced chickpea fritters served over pickled cabbage salad, custom tahini drizzle, and pita.", isVegetarian = true),
                MenuItem("me-5", "Spicy Shish Tawook", 18.90, "Hand-skewered chicken cubes marinated in sumac, garlic, wild chilies, and flame-grilled to perfection.", isSpicy = true)
            )
        ),
        Restaurant(
            id = "3",
            name = "Ocean Sushi",
            cuisine = "Japanese",
            rating = 4.9,
            priceRange = "$$$",
            reviewCount = 412,
            description = "Premium modern fusion lounge showcasing pristine market-select sashimi, artisan specialty rolls, and comforting slowly simmered rich broth ramen.",
            menu = listOf(
                MenuItem("jp-1", "Salmon Sushi Deluxe", 24.99, "Four pieces of artisan fatty salmon nigiri paired beautifully with a signature salmon avocado roll."),
                MenuItem("jp-2", "Tonkotsu Ramen", 18.50, "Thick, mineral-rich 16-hour pork bone broth topped with soy-marinated egg, chashu pork belly, and tender noodles.", isSpicy = true),
                MenuItem("jp-3", "Mochi Trio Platter", 7.99, "Indulgent assortment of traditional soft sweet rice mochi ice cream, featuring Matcha, Mango, and Black Sesame flavors.", isVegetarian = true),
                MenuItem("jp-4", "Spicy Tuna Volcano", 16.00, "Crispy spicy tuna roll topped with shredded crab sticks, green onions, toasted sesame, and sweet unagi glazes.", isSpicy = true),
                MenuItem("jp-5", "Yellowtail Sashimi", 21.00, "Thinly sliced premium sashimi-grade yellowtail fish topped with fresh jalapeno slices and ponzu vinegar.")
            )
        )
    )

    private val _restaurants = MutableStateFlow(initialRestaurants)
    val restaurants: StateFlow<List<Restaurant>> = _restaurants

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCuisine = MutableStateFlow("All")
    val selectedCuisine: StateFlow<String> = _selectedCuisine

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly

    // UI Dialog State Management
    private val _selectedRestaurantForMenu = MutableStateFlow<Restaurant?>(null)
    val selectedRestaurantForMenu: StateFlow<Restaurant?> = _selectedRestaurantForMenu

    private val _isAddRestaurantOpen = MutableStateFlow(false)
    val isAddRestaurantOpen: StateFlow<Boolean> = _isAddRestaurantOpen

    // Filter Logic combining search queries, category selections, and favorite toggles reactively
    val filteredRestaurantsToDisplay: StateFlow<List<Restaurant>> = combine(
        _restaurants, _searchQuery, _selectedCuisine, _showFavoritesOnly
    ) { list: List<Restaurant>, query: String, cuisine: String, favoritesOnly: Boolean ->
        list.filter { restaurant ->
            val matchesQuery = query.isBlank() || 
                    restaurant.name.contains(query, ignoreCase = true) ||
                    restaurant.cuisine.contains(query, ignoreCase = true) ||
                    restaurant.menu.any { it.name.contains(query, ignoreCase = true) }
            
            val matchesCuisine = cuisine == "All" || restaurant.cuisine.equals(cuisine, ignoreCase = true)
            val matchesFavorites = !favoritesOnly || restaurant.isFavorite
            
            matchesQuery && matchesCuisine && matchesFavorites
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialRestaurants)

    // Extracted set of all current cuisines to dynamically maintain category bar
    val availableCuisines: StateFlow<List<String>> = _restaurants.map { list ->
        listOf("All") + list.map { it.cuisine }.distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCuisine(cuisine: String) {
        _selectedCuisine.value = cuisine
    }

    fun toggleFavoritesFilter() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun toggleFavoriteRestaurant(restaurantId: String) {
        _restaurants.value = _restaurants.value.map { r ->
            if (r.id == restaurantId) r.copy(isFavorite = !r.isFavorite) else r
        }
    }

    fun openRestaurantMenu(restaurant: Restaurant) {
        _selectedRestaurantForMenu.value = restaurant
    }

    fun closeRestaurantMenu() {
        _selectedRestaurantForMenu.value = null
    }

    fun openAddRestaurant() {
        _isAddRestaurantOpen.value = true
    }

    fun closeAddRestaurant() {
        _isAddRestaurantOpen.value = false
    }

    fun addNewRestaurant(name: String, cuisine: String, rating: Double, priceRange: String, description: String, menuItemsSeparated: List<String>) {
        val uniqueId = (System.currentTimeMillis()).toString()
        
        // Convert plain strings into item models with random friendly descriptions and prices
        val parsedMenuItems = menuItemsSeparated.mapIndexed { index, dishName ->
            val basePrice = when(index % 3) {
                0 -> 12.99
                1 -> 15.50
                else -> 8.25
            }
            MenuItem(
                id = "item-$uniqueId-$index",
                name = dishName.trim(),
                price = basePrice,
                description = "Our classic signature chef specialty recipe made from fresh organic farm ingredients.",
                isVegetarian = index % 2 == 0
            )
        }

        val enrichedDescription = description.ifBlank {
            "Indulge in our exquisite selection of fine $cuisine delicacies, crafted daily by expert culinary masters with love."
        }

        val newRest = Restaurant(
            id = uniqueId,
            name = name.trim(),
            cuisine = cuisine.trim(),
            rating = rating,
            priceRange = priceRange,
            reviewCount = (45..150).random(),
            description = enrichedDescription,
            menu = parsedMenuItems.ifEmpty {
                listOf(MenuItem("item-$uniqueId-def", "Signature Platter", 19.99, "A curated sampler of our house-favorite specialties."))
            }
        )

        _restaurants.value = _restaurants.value + newRest
        _isAddRestaurantOpen.value = false
    }

}

// ==========================================
// 3. MAIN ACTIVITY CONTAINER
// ==========================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    RestaurantFinderAppScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// ==========================================
// 4. THE PRIMARY USER INTERFACE SCREEN
// ==========================================

@Composable
fun RestaurantFinderAppScreen(
    modifier: Modifier = Modifier,
    viewModel: RestaurantViewModel = viewModel()
) {
    val restaurants by viewModel.filteredRestaurantsToDisplay.collectAsStateWithLifecycle()
    val availableCuisines by viewModel.availableCuisines.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCuisine by viewModel.selectedCuisine.collectAsStateWithLifecycle()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsStateWithLifecycle()
    
    val selectedMenuRestaurant by viewModel.selectedRestaurantForMenu.collectAsStateWithLifecycle()
    val isAddRestaurantOpen by viewModel.isAddRestaurantOpen.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // A. TOP BRANDED BANNER AND USER DETAIL
            HeaderBanner(
                totalCount = restaurants.size,
                showFavoritesOnly = showFavoritesOnly,
                onFavoritesToggle = { viewModel.toggleFavoritesFilter() }
            )

            // B. SEARCH TEXT FIELD
            SearchBarSection(
                query = searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) }
            )

            // C. HORIZONTAL CUISINE CATEGORIES BAR
            CuisineCategoriesBar(
                cuisines = availableCuisines,
                selectedCuisine = selectedCuisine,
                onCuisineSelected = { viewModel.selectCuisine(it) }
            )

            // D. VERTICAL RESTAURANTS SCROLL LIST
            if (restaurants.isEmpty()) {
                EmptyStateView(
                    query = searchQuery,
                    cuisine = selectedCuisine,
                    favoritesOnly = showFavoritesOnly,
                    onResetSearch = {
                        viewModel.updateSearchQuery("")
                        viewModel.selectCuisine("All")
                        if (showFavoritesOnly) viewModel.toggleFavoritesFilter()
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(restaurants, key = { it.id }) { restaurant ->
                        RestaurantCard(
                            restaurant = restaurant,
                            onToggleFavorite = { viewModel.toggleFavoriteRestaurant(restaurant.id) },
                            onViewMenu = { viewModel.openRestaurantMenu(restaurant) }
                        )
                    }
                }
            }
        }

        // E. FLOATING ACTION BUTTON TO ADD CUSTOM SPOTS
        FloatingActionButton(
            onClick = { viewModel.openAddRestaurant() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_restaurant_fab")
                .windowInsetsPadding(WindowInsets.navigationBars),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Custom Restaurant",
                modifier = Modifier.size(28.dp)
            )
        }

        // F. IMMERSIVE MENU SIMULATOR TOOL DIALOG
        selectedMenuRestaurant?.let { restaurant ->
            FullMenuDetailsDialog(
                restaurant = restaurant,
                onDismiss = { viewModel.closeRestaurantMenu() }
            )
        }

        // G. ELEGANT CUSTOM ADD SPOT DIALOG
        if (isAddRestaurantOpen) {
            AddRestaurantFormDialog(
                onDismiss = { viewModel.closeAddRestaurant() },
                onSubmit = { name, cuisine, rating, price, description, dishList ->
                    viewModel.addNewRestaurant(name, cuisine, rating, price, description, dishList)
                }
            )
        }
    }
}

// ==========================================
// 5. ATOMIC COMPOSABLES AND DETAIL VIEWS
// ==========================================

@Composable
fun HeaderBanner(
    totalCount: Int,
    showFavoritesOnly: Boolean,
    onFavoritesToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Discover Top Food",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Restaurant Finder",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "🍴",
                    fontSize = 22.sp
                )
            }
        }

        // Right Actions: Favorites filter button and visual user profile badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Elegant badge styled Favorite toggle icon
            IconButton(
                onClick = onFavoritesToggle,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (showFavoritesOnly) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) 
                                else MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    )
                    .testTag("favorite_filter_toggle"),
            ) {
                Icon(
                    imageVector = if (showFavoritesOnly) Icons.Default.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Filter by favorites",
                    tint = if (showFavoritesOnly) FavoriteRed else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }

            // User Avatar Profile Badge represent user context (moviefilm10@gmail.com)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "M", // Matches 'movie' from moviefilm10
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarSection(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
         // Header bar card visual matches: bg-[#2B2930], h-14, rounded-full, border-[#49454F]/40, shadow-lg
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = "Search spots, cuisines, or dishes...",
                    color = Color(0xFF938F99)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFFCAC4D0)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.testTag("clear_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Search",
                            tint = Color(0xFFCAC4D0)
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF2B2930),
                unfocusedContainerColor = Color(0xFF2B2930),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color(0xFF49454F).copy(alpha = 0.4f),
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = Color(0xFFE6E1E5),
                unfocusedTextColor = Color(0xFFE6E1E5)
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("search_restaurant_input")
            )
    }
}

@Composable
fun CuisineCategoriesBar(
    cuisines: List<String>,
    selectedCuisine: String,
    onCuisineSelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("cuisine_categories_row"),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(cuisines) { cuisine ->
            val isSelected = selectedCuisine.equals(cuisine, ignoreCase = true)
            
            // Render custom-styled dynamic pill instead of boring gray cards
            Surface(
                onClick = { onCuisineSelected(cuisine) },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) Color.Transparent else Color(0xFF49454F)
                ),
                color = if (isSelected) Color(0xFFEADDFF) else Color(0xFF332D41),
                tonalElevation = if (isSelected) 4.dp else 0.dp,
                modifier = Modifier
                    .height(38.dp)
                    .testTag("cuisine_chip_$cuisine")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val emoji = when (cuisine.lowercase()) {
                        "all" -> "🍽️"
                        "italian" -> "🍕"
                        "japanese" -> "🍣"
                        "middle eastern" -> "🥙"
                        else -> "🍲"
                    }
                    Text(
                        text = "$emoji $cuisine",
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF21005D) else Color(0xFFE8DEF8),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RestaurantCard(
    restaurant: Restaurant,
    onToggleFavorite: () -> Unit,
    onViewMenu: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("restaurant_card_${restaurant.name.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Row 1: Left avatar icon with unique gradient, right heart icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cuisine-inspired gradient emblem
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val (cuisineEmoji, gradientColors) = when (restaurant.cuisine.lowercase()) {
                        "italian" -> "🍕" to listOf(Color(0xFFE53935), Color(0xFF43A047))
                        "japanese" -> "🍣" to listOf(Color(0xFFFFB300), Color(0xFFE53935))
                        "middle eastern" -> "🥙" to listOf(Color(0xFFFFB300), Color(0xFF8D6E63))
                        else -> "🍲" to listOf(Color(0xFFE65100), Color(0xFFD84315))
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(brush = Brush.linearGradient(gradientColors)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cuisineEmoji,
                            fontSize = 26.sp
                        )
                    }

                    Column {
                        Text(
                            text = restaurant.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = restaurant.cuisine,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "•",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                            )
                            Text(
                                text = restaurant.priceRange,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Interactive favorite button with ripple response
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = Color(0xFF332D41),
                            shape = CircleShape
                        )
                        .testTag("favorite_button_${restaurant.id}")
                ) {
                    Icon(
                        imageVector = if (restaurant.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite toggle",
                        tint = if (restaurant.isFavorite) FavoriteRed else Color(0xFFCAC4D0),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Row 2: Stars and Review description (Represented as elegant dark badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Elegant Rating Badge matches HTML design: bg-[#332D41] px-2 py-1 rounded-full text-[#E8DEF8] ★-[#FFD700]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFF332D41), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "★",
                        color = Color(0xFFFFD700),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${restaurant.rating}",
                        color = Color(0xFFE8DEF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Text(
                    text = "•",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                )

                Text(
                    text = "(${restaurant.reviewCount} reviews)",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 3: Brief Description text
            Text(
                text = restaurant.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Row 4: Styled menu pill tags horizontal flow (matches requested mock menu item badges: bg-[#49454F] text-[#CAC4D0])
            Text(
                text = "Signature Menu:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Show up to first 3 elements
                restaurant.menu.take(3).forEach { item ->
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFF49454F),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = item.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFCAC4D0)
                        )
                    }
                }
                if (restaurant.menu.size > 3) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "+${restaurant.menu.size - 3}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action: View Full Menu button with touch targets >= 48dp
            Button(
                onClick = onViewMenu,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary, // Resolves to D0BCFF (lavender)
                    contentColor = MaterialTheme.colorScheme.onPrimary  // Resolves to 381E72 (deep eggplant dark text)
                ),
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("view_menu_button_${restaurant.id}")
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "View Full Menu",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    query: String,
    cuisine: String,
    favoritesOnly: Boolean,
    onResetSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "No Delicacies Found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val message = when {
            favoritesOnly -> "You haven't favorited any restaurants yet in this category."
            query.isNotBlank() && cuisine != "All" -> "We couldn't find matching spots for \"$query\" in $cuisine."
            query.isNotBlank() -> "No spots or dishes match your search \"$query\"."
            else -> "No restaurants active in our catalog for $cuisine cuisine."
        }
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onResetSearch,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Reset Filter Settings", fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// 6. DETAILED FULL MENU & ORDERING DIALOG
// ==========================================

@Composable
fun FullMenuDetailsDialog(
    restaurant: Restaurant,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // Simulate interactive order quantities inside state dictionary
    var orderQuantities by remember { mutableStateOf(emptyMap<String, Int>()) }
    var simBookingCompleted by remember { mutableStateOf(false) }
    var bookingGuestCount by remember { mutableStateOf(2) }

    // Dynamic calculated totals
    val totalOrderValue = remember(orderQuantities) {
        orderQuantities.entries.sumOf { (itemId, qty) ->
            val dish = restaurant.menu.find { it.id == itemId }
            (dish?.price ?: 0.0) * qty
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false // Enables custom width / responsive tablet mode overlays
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(32.dp)),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // A. Header Card Block
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = restaurant.cuisine,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = restaurant.name,
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.surface,
                                            CircleShape
                                        )
                                        .testTag("dismiss_menu_dialog")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close details",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFC107),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${restaurant.rating}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 14.sp
                                    )
                                }
                                Text("•", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                                Text(
                                    text = restaurant.priceRange,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    fontSize = 14.sp
                                )
                                Text("•", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                                Text(
                                    text = "${restaurant.reviewCount} customer reviews",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = restaurant.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                lineHeight = 20.sp
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    // B. Interactive items scroll container
                    if (simBookingCompleted) {
                        BookingCompletedBanner(
                            restaurantName = restaurant.name,
                            itemsCount = orderQuantities.values.sum(),
                            totalCost = totalOrderValue,
                            guestCount = bookingGuestCount,
                            onClose = {
                                simBookingCompleted = false
                                orderQuantities = emptyMap()
                                onDismiss()
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Text(
                                    text = "Signature Dishes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            items(restaurant.menu) { dish ->
                                val currentQty = orderQuantities[dish.id] ?: 0
                                DishRowItem(
                                    dish = dish,
                                    quantity = currentQty,
                                    onQtyChanged = { newQty ->
                                        orderQuantities = if (newQty > 0) {
                                            orderQuantities + (dish.id to newQty)
                                        } else {
                                            orderQuantities - dish.id
                                        }
                                    }
                                )
                            }

                            // Interactive booking slider simulator block representing professional touch
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                BookingSimulatorPanel(
                                    guestCount = bookingGuestCount,
                                    onGuestCountChanged = { bookingGuestCount = it }
                                )
                            }
                        }

                        // C. Bottom Sticky Action Area
                        Surface(
                            shadowElevation = 8.dp,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp, horizontal = 24.dp)
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Estimated Order Total",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = String.format("$%.2f", totalOrderValue),
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (totalOrderValue == 0.0) {
                                                Toast.makeText(context, "Please add dishes first to simulate order!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                simBookingCompleted = true
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("submit_checkout_simulation")
                                    ) {
                                        Text(
                                            text = if (totalOrderValue > 0.0) "Book & Order Now" else "Interactive Checkout",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DishRowItem(
    dish: MenuItem,
    quantity: Int,
    onQtyChanged: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = dish.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (dish.isVegetarian) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Veg 🌿",
                                    fontSize = 9.sp,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (dish.isSpicy) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFFEBEE), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Spicy 🔥",
                                    fontSize = 9.sp,
                                    color = Color(0xFFC62828),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dish.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = String.format("$%.2f", dish.price),
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-row: Selection controller (Tappable target size >= 48dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (quantity == 0) {
                    IconButton(
                        onClick = { onQtyChanged(1) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                shape = CircleShape
                            )
                            .testTag("add_item_${dish.name.replace(" ", "_").lowercase()}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Item to Cart",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = { onQtyChanged(quantity - 1) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .testTag("decrease_item_${dish.name.replace(" ", "_").lowercase()}")
                        ) {
                            Text(
                                text = "—",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 14.sp
                            )
                        }

                        Text(
                            text = "$quantity",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 15.sp
                        )

                        IconButton(
                            onClick = { onQtyChanged(quantity + 1) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                                .testTag("increase_item_${dish.name.replace(" ", "_").lowercase()}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase Quantity",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookingSimulatorPanel(
    guestCount: Int,
    onGuestCountChanged: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Dine-In Table Reservation",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Number of Guests:  $guestCount persons",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )

                // Plus-minus guest buttons for interactive ease
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { if (guestCount > 1) onGuestCountChanged(guestCount - 1) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Text(
                            text = "—",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "$guestCount",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                    IconButton(
                        onClick = { if (guestCount < 12) onGuestCountChanged(guestCount + 1) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Add, "More guests")
                    }
                }
            }
        }
    }
}

@Composable
fun BookingCompletedBanner(
    restaurantName: String,
    itemsCount: Int,
    totalCost: Double,
    guestCount: Int,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color(0xFFE8F5E9), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Reservation Confirmed!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Booking Code: RF-${(10000..99999).random()}",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Spot Location", color = Color.Gray, fontSize = 13.sp)
                    Text(restaurantName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Guest Count", color = Color.Gray, fontSize = 13.sp)
                    Text("$guestCount persons", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pre-ordered Items", color = Color.Gray, fontSize = 13.sp)
                    Text("$itemsCount dishes ordered", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Amount Paid", color = Color.Gray, fontSize = 13.sp)
                    Text(String.format("$%.2f", totalCost), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onClose,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("dismiss_success_completed")
        ) {
            Text("Looks Delicious!", fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// 7. FORM DIALOG TO ADD CUSTOM SPOTS
// ==========================================

@Composable
fun AddRestaurantFormDialog(
    onDismiss: () -> Unit,
    onSubmit: (name: String, cuisine: String, rating: Double, priceRange: String, description: String, listDishes: List<String>) -> Unit
) {
    var nameState by remember { mutableStateOf("") }
    var cuisineState by remember { mutableStateOf("Italian") }
    var ratingState by remember { mutableStateOf(4.5) }
    var priceState by remember { mutableStateOf("$$") }
    var descState by remember { mutableStateOf("") }
    var signatureDishesState by remember { mutableStateOf("") }

    val presetCuisines = listOf("Italian", "Japanese", "Middle Eastern", "American", "Indian", "Mexican", "Others")
    val presetPrices = listOf("$", "$$", "$$$")

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add Custom Spot",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close form")
                        }
                    }
                }

                item {
                    // Restaurant Name
                    Text(
                        text = "Restaurant Name *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = nameState,
                        onValueChange = { nameState = it },
                        placeholder = { Text("e.g. Taco Fiesta") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_restaurant_name_input")
                    )
                }

                item {
                    // Cuisine Select Buttons
                    Text(
                        text = "Cuisine Category",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(presetCuisines) { cat ->
                            val isChosen = cuisineState == cat
                            Surface(
                                onClick = { cuisineState = cat },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isChosen) Color.Transparent else MaterialTheme.colorScheme.outline),
                                color = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("form_cuisine_$cat")
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isChosen) Color.White else MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    // Star rating slider interaction
                    Text(
                        text = String.format("Overall Rating:  %.1f Stars ⭐", ratingState),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = ratingState.toFloat(),
                        onValueChange = { ratingState = Math.round(it * 10.0) / 10.0 },
                        valueRange = 1f..5f,
                        steps = 39, // Increments of 0.1
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("rating_slider")
                    )
                }

                item {
                    // Price tier picker
                    Text(
                        text = "Price Tier",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        presetPrices.forEach { price ->
                            val isChosen = priceState == price
                            Surface(
                                onClick = { priceState = price },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("form_price_$price"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isChosen) Color.Transparent else MaterialTheme.colorScheme.outline),
                                color = if (isChosen) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = price,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isChosen) Color.White else MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    // Description
                    Text(
                        text = "Description (Optional)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = descState,
                        onValueChange = { descState = it },
                        placeholder = { Text("Describe the ambient setting, signature dishes, or mood...") },
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_restaurant_description")
                    )
                }

                item {
                    // Comma delimited menu list
                    Text(
                        text = "Signature Dishes (comma-separated)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = signatureDishesState,
                        onValueChange = { signatureDishesState = it },
                        placeholder = { Text("e.g. Classic Tacos, Cheesy Nachos, Fruit Sorbet") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_restaurant_menu_input")
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val context = LocalContext.current
                    Button(
                        onClick = {
                            if (nameState.isBlank()) {
                                Toast.makeText(context, "Please enter a restaurant name!", Toast.LENGTH_SHORT).show()
                            } else {
                                val dishesList = if (signatureDishesState.isNotBlank()) {
                                    signatureDishesState.split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                } else {
                                    emptyList()
                                }
                                onSubmit(nameState, cuisineState, ratingState, priceState, descState, dishesList)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_create_restaurant")
                    ) {
                        Text("Add to Discovery List", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
