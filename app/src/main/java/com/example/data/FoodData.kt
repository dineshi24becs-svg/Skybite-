package com.example.data

data class Category(
    val name: String,
    val iconName: String
)

data class FoodItem(
    val id: Int,
    val name: String,
    val price: Double,
    val description: String,
    val imageUrl: String,
    val category: String,
    val isTrending: Boolean = false,
    val isPopular: Boolean = false,
    val isAiRecommended: Boolean = false,
    val rating: Double = 4.5,
    val deliveryTimeMin: Int = 25
)

object FoodData {
    val categories = listOf(
        Category("Biryani", "restaurant"),
        Category("Pizza", "local_pizza"),
        Category("Burgers", "lunch_dining"),
        Category("Chinese", "ramen_dining"),
        Category("Desserts", "cake"),
        Category("Drinks", "local_bar"),
        Category("South Indian", "rice_bowl"),
        Category("Wraps", "wrap_text"),
        Category("Shawarma", "dinner_dining"),
        Category("Sushi", "set_meal"),
        Category("Tacos", "bakery_dining"),
        Category("Snacks", "cookie"),
        Category("Indian", "kebab_dining"),
        Category("Italian", "dinner_dining"),
        Category("Sides", "layers"),
        Category("Platters", "grid_view"),
        Category("Noodles", "ramen_dining"),
        Category("Rolls", "wrap_text"),
        Category("Kebabs", "kebab_dining"),
        Category("Salads", "local_hospital"), // Green leaf
        Category("Ice Cream", "icecream"),
        Category("Cakes", "cake"),
        Category("Waffles", "grid_on"),
        Category("Momos", "circle"),
        Category("Pasta", "dinner_dining"),
        Category("Sandwiches", "layers"),
        Category("Soups", "soup_kitchen"),
        Category("Steaks", "restaurant"),
        Category("Seafood", "set_meal"),
        Category("Vegan", "eco")
    )

    val foodItems = listOf(
        FoodItem(
            id = 1,
            name = "Aero-Spiced Hyderabadi Biryani",
            price = 349.00,
            description = "Slow-cooked long grain basmati rice, tender pieces of chicken infused with 24 secret sky-spices, served with mirchi ka salan and drone-chilled raita.",
            imageUrl = "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=500&auto=format&fit=crop&q=60",
            category = "Biryani",
            isTrending = true,
            isPopular = true,
            isAiRecommended = true,
            rating = 4.9
        ),
        FoodItem(
            id = 2,
            name = "Neon Tech Margherita Pizza",
            price = 299.00,
            description = "Glow-oven baked fresh thin crust, vibrant Roma tomato sauce, premium mozzarella pearls, basil leaves, and a drizzle of extra virgin olive oil.",
            imageUrl = "https://images.unsplash.com/photo-1604068549290-dea0e4a305ca?w=500&auto=format&fit=crop&q=60",
            category = "Pizza",
            isPopular = true,
            rating = 4.7
        ),
        FoodItem(
            id = 3,
            name = "SkyHigh Double Smash Burger",
            price = 249.00,
            description = "Two flame-grilled smash patties, melted cheddar, crispy onion strings, custom SkyBite smoky sauce, on a toasted brioche bun.",
            imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500&auto=format&fit=crop&q=60",
            category = "Burgers",
            isTrending = true,
            isPopular = true,
            rating = 4.8
        ),
        FoodItem(
            id = 4,
            name = "Quantum Schezwan Noodles",
            price = 199.00,
            description = "Wok-tossed noodles with colorful peppers, crunchy spring onion, and fiery homemade Schezwan sauce with a hint of garlic.",
            imageUrl = "https://images.unsplash.com/photo-1585032226651-759b368d7246?w=500&auto=format&fit=crop&q=60",
            category = "Noodles",
            isAiRecommended = true,
            rating = 4.5
        ),
        FoodItem(
            id = 5,
            name = "Stratosphere Choco Volcano Cake",
            price = 159.00,
            description = "Rich dark chocolate sponge with a warm, gooey molten chocolate center that flows like lava. A favorite of dessert lovers.",
            imageUrl = "https://images.unsplash.com/photo-1563729784474-d77dbb933a9e?w=500&auto=format&fit=crop&q=60",
            category = "Desserts",
            isTrending = true,
            rating = 4.9
        ),
        FoodItem(
            id = 6,
            name = "Hyperdrive Blue Lagoon Cooler",
            price = 119.00,
            description = "A refreshing electric blue mocktail crafted with blue curaçao, fresh lime, mint leaves, and a bubbly splash of soda.",
            imageUrl = "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?w=500&auto=format&fit=crop&q=60",
            category = "Drinks",
            isPopular = true,
            rating = 4.6
        ),
        FoodItem(
            id = 7,
            name = "Sonic Butter Masala Dosa",
            price = 149.00,
            description = "Crispy golden crepe brushed with fresh butter, filled with spiced mashed potatoes, served with coconut chutney and hot sambar.",
            imageUrl = "https://images.unsplash.com/photo-1668236543090-82eba5ee5976?w=500&auto=format&fit=crop&q=60",
            category = "South Indian",
            isAiRecommended = true,
            rating = 4.7
        ),
        FoodItem(
            id = 8,
            name = "Laser-Cut Spicy Paneer Wrap",
            price = 179.00,
            description = "Crispy wrap stuffed with spicy marinated paneer cubes, crunchy cabbage slaw, green chutney, and futuristic mint mayo.",
            imageUrl = "https://images.unsplash.com/photo-1626700051175-6518c4793f4f?w=500&auto=format&fit=crop&q=60",
            category = "Wraps",
            rating = 4.4
        ),
        FoodItem(
            id = 9,
            name = "Gravity-Defying Chicken Shawarma",
            price = 189.00,
            description = "Shaved slow-roasted seasoned chicken, pickled vegetables, garlic toum sauce, rolled tightly in a warm Lebanese pita.",
            imageUrl = "https://images.unsplash.com/photo-1529042410759-befb1204b468?w=500&auto=format&fit=crop&q=60",
            category = "Shawarma",
            isTrending = true,
            rating = 4.8
        ),
        FoodItem(
            id = 10,
            name = "Cyberpunk Salmon Sushi Roll",
            price = 449.00,
            description = "Premium fresh Atlantic salmon, creamy avocado, cucumber, rolled with vinegared sushi rice and served with wasabi and soy sauce.",
            imageUrl = "https://images.unsplash.com/photo-1579871494447-9811cf80d66c?w=500&auto=format&fit=crop&q=60",
            category = "Sushi",
            isTrending = true,
            isAiRecommended = true,
            rating = 4.9
        ),
        FoodItem(
            id = 11,
            name = "Turbo Fire Beef Tacos",
            price = 229.00,
            description = "Three crunchy taco shells filled with seasoned ground beef, sour cream, pico de gallo, shredded lettuce, and hot salsa.",
            imageUrl = "https://images.unsplash.com/photo-1565299585323-38d6b0865b47?w=500&auto=format&fit=crop&q=60",
            category = "Tacos",
            rating = 4.5
        ),
        FoodItem(
            id = 12,
            name = "Crispy Tech Potato Fries",
            price = 99.00,
            description = "Perfectly cut, double-fried golden russet potatoes tossed with active peri-peri seasoning and sea salt.",
            imageUrl = "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=500&auto=format&fit=crop&q=60",
            category = "Snacks",
            isPopular = true,
            rating = 4.5
        ),
        FoodItem(
            id = 13,
            name = "Fusion Paneer Tikka Masala",
            price = 319.00,
            description = "Clay-oven roasted cottage cheese chunks simmered in a rich, creamy, tomato-onion gravy with aromatic Indian spices.",
            imageUrl = "https://images.unsplash.com/photo-1565557623262-b51c2513a641?w=500&auto=format&fit=crop&q=60",
            category = "Indian",
            isAiRecommended = true,
            rating = 4.7
        ),
        FoodItem(
            id = 14,
            name = "Vector Alfredo White Pasta",
            price = 279.00,
            description = "Penne pasta cooked in a silky smooth parmesan cream sauce, loaded with sliced button mushrooms, garlic, and herbs.",
            imageUrl = "https://images.unsplash.com/photo-1645112411341-6c4fd023714a?w=500&auto=format&fit=crop&q=60",
            category = "Pasta",
            rating = 4.6
        ),
        FoodItem(
            id = 15,
            name = "Supercharged Garlic Bread",
            price = 129.00,
            description = "Four slices of fresh baguette baked with rich garlic butter, loaded with melted stringy mozzarella cheese.",
            imageUrl = "https://images.unsplash.com/photo-1573140247632-f8fd74997d5c?w=500&auto=format&fit=crop&q=60",
            category = "Sides",
            rating = 4.4
        ),
        FoodItem(
            id = 16,
            name = "The Mega Drone Platter",
            price = 599.00,
            description = "A majestic sharing board consisting of mini burgers, hot chicken wings, peri-peri wedges, onion rings, and a collection of dips.",
            imageUrl = "https://images.unsplash.com/photo-1544025162-d76694265947?w=500&auto=format&fit=crop&q=60",
            category = "Platters",
            isPopular = true,
            rating = 4.9
        ),
        FoodItem(
            id = 17,
            name = "Galactic Spring Rolls",
            price = 149.00,
            description = "Crispy golden wrappers filled with seasoned julienned vegetables, glass noodles, and fried to absolute perfection.",
            imageUrl = "https://images.unsplash.com/photo-1544025162-d76694265947?w=500&auto=format&fit=crop&q=60", // Generic good photo
            category = "Rolls",
            rating = 4.3
        ),
        FoodItem(
            id = 18,
            name = "Electro-Seared Paneer Kebab",
            price = 259.00,
            description = "Slabs of premium cottage cheese marinated in spiced yogurt, skewered with onions and peppers, seared over charcoal.",
            imageUrl = "https://images.unsplash.com/photo-1565557623262-b51c2513a641?w=500&auto=format&fit=crop&q=60",
            category = "Kebabs",
            rating = 4.6
        ),
        FoodItem(
            id = 19,
            name = "Bio-Green Avocado Salad",
            price = 219.00,
            description = "Freshly picked butterhead lettuce, creamy sliced avocados, cherry tomatoes, cucumbers, tossed in a citrus vinaigrette.",
            imageUrl = "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=500&auto=format&fit=crop&q=60",
            category = "Salads",
            isAiRecommended = true,
            rating = 4.6
        ),
        FoodItem(
            id = 20,
            name = "Zero-Gravity Mango Ice Cream",
            price = 139.00,
            description = "Premium double-churned cream infused with pure Alphonso mango pulp, creating a smooth and frosty delightful dessert.",
            imageUrl = "https://images.unsplash.com/photo-1497034825429-c343d7c6a68f?w=500&auto=format&fit=crop&q=60",
            category = "Ice Cream",
            isPopular = true,
            rating = 4.8
        ),
        FoodItem(
            id = 21,
            name = "Orbital Velvet Cake Slice",
            price = 169.00,
            description = "Fluffy red velvet cake layers filled and frosted with tangy cream cheese frosting, topped with red velvet crumbs.",
            imageUrl = "https://images.unsplash.com/photo-1588195538326-c5b1e9f80a1b?w=500&auto=format&fit=crop&q=60",
            category = "Cakes",
            rating = 4.7
        ),
        FoodItem(
            id = 22,
            name = "Warp-Drive Nutella Waffle",
            price = 189.00,
            description = "Freshly ironed warm waffle smothered with Nutella, topped with chocolate chips and a scoop of vanilla bean gelato.",
            imageUrl = "https://images.unsplash.com/photo-1562376502-6f769499c886?w=500&auto=format&fit=crop&q=60",
            category = "Waffles",
            rating = 4.8
        ),
        FoodItem(
            id = 23,
            name = "Steam-Dispatched Steamed Momos",
            price = 139.00,
            description = "Soft, thin wrappers filled with juicy minced chicken and spices, steamed and served with a fiery hot tomato dipping chili chutney.",
            imageUrl = "https://images.unsplash.com/photo-1534422298391-e4f8c172dddb?w=500&auto=format&fit=crop&q=60",
            category = "Momos",
            isTrending = true,
            rating = 4.7
        ),
        FoodItem(
            id = 24,
            name = "Matrix Cheese Sandwich",
            price = 149.00,
            description = "Artisanal sourdough bread stuffed with sharp cheddar, mozzarella, sliced tomatoes, and spicy green pesto, grilled with herb butter.",
            imageUrl = "https://images.unsplash.com/photo-1528735602780-2552fd46c7af?w=500&auto=format&fit=crop&q=60",
            category = "Sandwiches",
            rating = 4.4
        ),
        FoodItem(
            id = 25,
            name = "Aero Tomato-Basil Soup",
            price = 129.00,
            description = "A warm, silky, classic soup made with sun-ripened tomatoes, fresh basil leaves, and a touch of light cooking cream.",
            imageUrl = "https://images.unsplash.com/photo-1547592165-e1d17fed6005?w=500&auto=format&fit=crop&q=60",
            category = "Soups",
            rating = 4.5
        ),
        FoodItem(
            id = 26,
            name = "Supersonic Grilled Steak",
            price = 499.00,
            description = "Juicy tenderloin steak seared to medium rare, served with creamy mashed potatoes, grilled asparagus, and a side of pepper sauce.",
            imageUrl = "https://images.unsplash.com/photo-1546964124-0cce460f38ef?w=500&auto=format&fit=crop&q=60",
            category = "Steaks",
            isTrending = true,
            rating = 4.9
        ),
        FoodItem(
            id = 27,
            name = "Deep-Sea Butter Garlic Prawns",
            price = 429.00,
            description = "Plump fresh prawns sautéed in a rich garlic butter sauce, topped with chopped parsley and fresh lemon wedges.",
            imageUrl = "https://images.unsplash.com/photo-1559737605-de6a22290d22?w=500&auto=format&fit=crop&q=60",
            category = "Seafood",
            rating = 4.7
        ),
        FoodItem(
            id = 28,
            name = "Green-Tech Vegan Burger",
            price = 229.00,
            description = "Plant-based delicious patty, vegan cheese, crisp lettuce, juicy tomato slice, vegan garlic sauce, on an organic whole wheat bun.",
            imageUrl = "https://images.unsplash.com/photo-1585238342024-78d387f4a707?w=500&auto=format&fit=crop&q=60",
            category = "Vegan",
            isAiRecommended = true,
            rating = 4.6
        ),
        FoodItem(
            id = 29,
            name = "Cyber Kung-Pao Chicken",
            price = 289.00,
            description = "Wok-fried chicken cubes with roasted peanuts, mixed peppers, and green chillies in a spicy, savory sauce.",
            imageUrl = "https://images.unsplash.com/photo-1525755662778-989d0524087e?w=500&auto=format&fit=crop&q=60",
            category = "Chinese",
            isPopular = true,
            rating = 4.7
        ),
        FoodItem(
            id = 30,
            name = "Tuscan Sun Penne Arrabbiata",
            price = 269.00,
            description = "Spicy pasta prepared in a robust garlic-flavored sauce made from red chili peppers and fresh vine tomatoes.",
            imageUrl = "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=500&auto=format&fit=crop&q=60", // Sourcing generic
            category = "Italian",
            rating = 4.5
        ),
        FoodItem(
            id = 31,
            name = "Tectonic Triple Chocolate Pastry",
            price = 149.00,
            description = "Three premium layers of milk, white, and dark chocolate mousse on a soft chocolate sponge, glazed with ganache.",
            imageUrl = "https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=500&auto=format&fit=crop&q=60",
            category = "Desserts",
            isPopular = true,
            rating = 4.8
        ),
        FoodItem(
            id = 32,
            name = "Hyper-Charged Cold Brew Coffee",
            price = 129.00,
            description = "18-hour slow steeped premium single origin Arabica coffee, poured over crystal clear ice, option of sweet cream.",
            imageUrl = "https://images.unsplash.com/photo-1517701604599-bb29b565090c?w=500&auto=format&fit=crop&q=60",
            category = "Drinks",
            isTrending = true,
            rating = 4.7
        )
    )
}
