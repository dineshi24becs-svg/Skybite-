package com.example.data

import kotlin.math.sqrt

data class MapNode(
    val id: String,
    val name: String,
    val xPercent: Float, // 0 to 100 on canvas width coordinate
    val yPercent: Float  // 0 to 100 on canvas height coordinate
)

data class MapEdge(
    val fromNodeId: String,
    val toNodeId: String,
    val baseWeight: Float,
    val name: String,
    var trafficMultiplier: Float = 1.0f // modified dynamically in high-traffic mode
) {
    val weight: Float
        get() = baseWeight * trafficMultiplier
}

object PathFinder {
    
    // Nodes representing major sky ports and navigation waypoints
    val nodes = listOf(
        MapNode("HOTEL", "Hotel Hangar Start", 15f, 82f),
        MapNode("N1", "Quantum Junction", 30f, 65f),
        MapNode("N2", "Nebula Intersection", 25f, 40f),
        MapNode("N3", "Aether Crossroad", 48f, 70f),
        MapNode("N4", "Ion Plaza", 52f, 45f),
        MapNode("N5", "Cyber Bypass", 68f, 60f),
        MapNode("N6", "Starlight Point", 72f, 30f),
        MapNode("CUSTOMER", "Destination Hangar", 85f, 18f)
    )

    // Edges (streets/flight channels) connecting waypoints
    fun getEdges(heavyTraffic: Boolean): List<MapEdge> {
        return listOf(
            MapEdge("HOTEL", "N1", 15f, "Launch Lane"),
            MapEdge("HOTEL", "N3", 35f, "Stratosphere Ave", if (heavyTraffic) 2.5f else 1.0f), // direct but gets heavily congested
            MapEdge("N1", "N2", 18f, "Quantum Boulevard"),
            MapEdge("N1", "N3", 12f, "Nebula Way"),
            MapEdge("N2", "N4", 20f, "Gravity Bypass"),
            MapEdge("N3", "N4", 15f, "Aether Pass"),
            MapEdge("N3", "N5", 22f, "Cosmic Lane", if (heavyTraffic) 1.8f else 1.0f),
            MapEdge("N4", "N6", 18f, "Ion Highway"),
            MapEdge("N5", "N6", 12f, "Cyber Road"),
            MapEdge("N5", "CUSTOMER", 28f, "Atmospheric Descent"),
            MapEdge("N6", "CUSTOMER", 15f, "Starlight Path"),
            MapEdge("N4", "CUSTOMER", 32f, "Singularity Drive", if (heavyTraffic) 2.2f else 1.0f)
        )
    }

    /**
     * Dijkstra's Shortest Path Algorithm to calculate the optimal path
     * between startNodeId and endNodeId based on current edge weights.
     */
    fun findShortestPath(
        startId: String = "HOTEL",
        endId: String = "CUSTOMER",
        heavyTraffic: Boolean = false
    ): List<MapNode> {
        val activeEdges = getEdges(heavyTraffic)
        val distances = mutableMapOf<String, Float>()
        val previous = mutableMapOf<String, String?>()
        val unvisited = nodes.map { it.id }.toMutableSet()

        for (node in nodes) {
            distances[node.id] = if (node.id == startId) 0f else Float.MAX_VALUE
            previous[node.id] = null
        }

        while (unvisited.isNotEmpty()) {
            val currentId = unvisited.minByOrNull { distances[it] ?: Float.MAX_VALUE } ?: break
            if (currentId == endId || distances[currentId] == Float.MAX_VALUE) break

            unvisited.remove(currentId)

            val currentNeighbors = activeEdges.filter { it.fromNodeId == currentId || it.toNodeId == currentId }
            for (edge in currentNeighbors) {
                val neighborId = if (edge.fromNodeId == currentId) edge.toNodeId else edge.fromNodeId
                if (!unvisited.contains(neighborId)) continue

                val alt = (distances[currentId] ?: 0f) + edge.weight
                if (alt < (distances[neighborId] ?: Float.MAX_VALUE)) {
                    distances[neighborId] = alt
                    previous[neighborId] = currentId
                }
            }
        }

        val path = mutableListOf<MapNode>()
        var u: String? = endId
        while (u != null) {
            val node = nodes.find { it.id == u }
            if (node != null) {
                path.add(0, node)
            }
            u = previous[u]
        }
        return if (path.firstOrNull()?.id == startId) path else emptyList()
    }

    /**
     * Interpolates the current 2D coordinate on the path based on flight progress (0.0f to 1.0f).
     */
    fun getPositionOnPath(path: List<MapNode>, progress: Float): Pair<Float, Float> {
        if (path.isEmpty()) return Pair(50f, 50f)
        if (path.size == 1) return Pair(path[0].xPercent, path[0].yPercent)
        if (progress <= 0f) return Pair(path.first().xPercent, path.first().yPercent)
        if (progress >= 1f) return Pair(path.last().xPercent, path.last().yPercent)

        val segments = mutableListOf<Float>()
        var totalLength = 0f
        for (i in 0 until path.size - 1) {
            val dx = path[i + 1].xPercent - path[i].xPercent
            val dy = path[i + 1].yPercent - path[i].yPercent
            val len = sqrt(dx * dx + dy * dy)
            segments.add(len)
            totalLength += len
        }

        if (totalLength == 0f) return Pair(path.first().xPercent, path.first().yPercent)

        val targetLength = totalLength * progress
        var accumulatedLength = 0f
        for (i in 0 until segments.size) {
            val segLen = segments[i]
            if (accumulatedLength + segLen >= targetLength) {
                val segProgress = (targetLength - accumulatedLength) / segLen
                val startNode = path[i]
                val endNode = path[i + 1]
                val x = startNode.xPercent + (endNode.xPercent - startNode.xPercent) * segProgress
                val y = startNode.yPercent + (endNode.yPercent - startNode.yPercent) * segProgress
                return Pair(x, y)
            }
            accumulatedLength += segLen
        }

        return Pair(path.last().xPercent, path.last().yPercent)
    }
}
