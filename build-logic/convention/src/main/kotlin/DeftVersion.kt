/** Strict version parsing prevents component overlap and versionCode collisions. */
data class DeftVersion(val name: String, val code: Int) {
    companion object {
        fun parse(value: String): DeftVersion {
            val match = Regex("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)-([1-9][0-9]?)").matchEntire(value)
            requireNotNull(match) { "deft.version must be A.B.C-D with D in 1..99" }
            val (major, feature, fix, build) = match.destructured
            val a = major.toLongOrNull() ?: throw IllegalArgumentException("Invalid major version")
            val b = feature.toIntOrNull() ?: throw IllegalArgumentException("Invalid feature version")
            val c = fix.toIntOrNull() ?: throw IllegalArgumentException("Invalid fix version")
            require(a <= 2146 && b in 0..99 && c in 0..99) { "Version components exceed versionCode capacity" }
            val code = a * 1_000_000 + b * 10_000 + c * 100 + build.toInt()
            require(code <= Int.MAX_VALUE) { "versionCode exceeds Android capacity" }
            return DeftVersion("$major.$feature.$fix", code.toInt())
        }
    }
}
