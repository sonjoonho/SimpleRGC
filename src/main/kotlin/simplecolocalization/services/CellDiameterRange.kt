package simplecolocalization.services

data class CellDiameterRange(val smallest: Double, val largest: Double) {

    companion object {

        /** Extract diameter range from string. */
        fun parseFromText(text: String): CellDiameterRange {
            // Regex matching diameter range (e.g. 0.0-30.0)
            val diameterPtn = "[0-9]+(.[0-9]{1,2})?"
            val rangeRegex = Regex("""^(?<smallest>$diameterPtn)\s*-\s*(?<largest>$diameterPtn)$""")

            val match = rangeRegex.matchEntire(text)
                ?: throw DiameterParseException("Cell diameter should be of the form e.g.'0.0 - 30.0'")

            // Extract smallest and largest diameters
            val groups = match.groups
            val smallest = groups["smallest"]!!.value.toDouble()
            val largest = groups["largest"]!!.value.toDouble()

            if (smallest >= largest) {
                throw DiameterParseException("Smallest cell diameter must be smaller than the largest cell diameter")
            }

            return CellDiameterRange(smallest, largest)
        }
    }
}

class DiameterParseException(message: String) : Exception(message)
