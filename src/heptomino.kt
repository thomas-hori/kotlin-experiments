import kotlin.math.max
import kotlin.math.min

fun generateGrid (w:Int, h:Int):List<MutableList<Boolean>> {
    /**
     * Generate an empty two-state cellular automaton grid of the specified dimensions.
     */
    return List(w){ MutableList(h){false} }
}

fun gridLike (grid:List<MutableList<Boolean>>):List<MutableList<Boolean>> {
    /**
     * Return an empty two-state cellular automaton grid of the same dimensions as the grid
     * passed in.
     */
    return generateGrid(grid.size, grid[0].size)
}

fun mooreNeighbourhood (x:Int, y:Int, grid:List<MutableList<Boolean>>, depth:Int=1):Int {
    /**
     * Return the number of active cells in the given cell's Moore neighbourhood of the
     * given depth. Out-of-range areas are treated as inactive.
     */
    var count = 0
    for (curX in max(x - depth, 0)..min(x + depth, grid.lastIndex)) {
        for (curY in max(y - depth, 0)..min(y + depth, grid[curX].lastIndex)) {
            if (curX == x && curY == y) {
                continue
            }
            if (grid[curX][curY]) {
                count += 1
            }
        }
    }
    return count
}

fun nextGenerationConway (lastGeneration:List<MutableList<Boolean>>):List<MutableList<Boolean>> {
    /**
     * Increment a generation in the two-state cellular automaton rule
     * (Moore(Depth=1) Survival(2,3) Birth(3)), more popularly known as
     * Conway's Game of Life.
     */
    val newGeneration = gridLike(lastGeneration)
    for (x in 0..newGeneration.lastIndex) {
        for (y in 0..newGeneration[x].lastIndex) {
            val count = mooreNeighbourhood(x, y, lastGeneration)
            newGeneration[x][y] = when (count) {
                3 -> true
                2 -> lastGeneration[x][y]
                else -> false
            }
        }
    }
    return newGeneration
}

fun nextGenerationHighLife (lastGeneration:List<MutableList<Boolean>>):List<MutableList<Boolean>> {
    /**
     * Increment a generation in the two-state cellular automaton rule
     * (Moore(Depth=1) Survival(2,3) Birth(3,6)), more popularly known as HighLife.
     */
    val newGeneration = gridLike(lastGeneration)
    for (x in 0..newGeneration.lastIndex) {
        for (y in 0..newGeneration[x].lastIndex) {
            val count = mooreNeighbourhood(x, y, lastGeneration)
            newGeneration[x][y] = when (count) {
                3 -> true
                2 -> lastGeneration[x][y]
                6 -> !(lastGeneration[x][y])
                else -> false
            }
        }
    }
    return newGeneration
}

fun showGrid (grid:List<MutableList<Boolean>>) {
    /**
     * Output a two-state cellular automaton grid to the console.
     *
     * Per convention amongst GoL applications, in a textual representation
     * of a grid, either "b" or "." (here, ".") denotes an inactive cell,
     * while "o" or "*" (here, "o") denotes an active cell.
     */
    for (y in 0..grid[0].lastIndex) {
        for (x in 0..grid.lastIndex) {
            if (grid[x][y]) {
                print("o")
            } else {
                print(".")
            }
        }
        println("")
    }
}

fun heptomino () {
    /**
     * Test the B-heptomino, a pattern whose population stabilises over a moderately large
     * number of generations under Conway's Life.
     */
    var grid = generateGrid(128, 128)
    // Set up a B-heptomino
    grid[62][62] = true
    grid[64][62] = true
    grid[65][62] = true
    grid[62][63] = true
    grid[63][63] = true
    grid[64][63] = true
    grid[63][64] = true
    // Run it for 148 generations
    // Output should look like this: https://conwaylife.com/wiki/File:BheptominoFinal.png
    for (i in 1..148) {
        grid = nextGenerationConway(grid)
    }
    showGrid(grid)
    println()
}

fun butterfly (generationFunction:(List<MutableList<Boolean>>) -> List<MutableList<Boolean>>) {
    /**
     * Test the Butterfly, a pattern which stabilises under Conway's Life but which
     * self-replicates indefinitely under HighLife, with the given rule function.
     */
    var grid = generateGrid(128, 128)
    // Set up a butterfly
    grid[62][62] = true
    grid[62][63] = true
    grid[63][63] = true
    grid[62][64] = true
    grid[64][64] = true
    grid[63][65] = true
    grid[64][65] = true
    grid[65][65] = true
    // Run it for 48 generations
    // GoL output should look like this: https://conwaylife.com/wiki/File:Butterfly2.png
    // HighLife output should have replicated itself (allowing for inert spark debris)
    for (i in 1..48) {
        grid = generationFunction(grid)
    }
    showGrid(grid)
    println()
}

fun main() {
    heptomino()
    butterfly(::nextGenerationConway)
    butterfly(::nextGenerationHighLife)
}