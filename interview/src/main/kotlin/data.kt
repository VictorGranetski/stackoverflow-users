import com.squareup.moshi.Json

data class Tags(
        @Json(name = "items") val tags: List<Tag>,
        @Json(name = "has_more") val hasMore: Boolean,
        @Json(name = "quota_remaining") val quotaRemaining: Int
) {
    fun containAtLeastOneTag(expectedTags: Set<String>) = tags.any { tag ->
        expectedTags.contains(tag.name)
    }
}

data class Tag(
        @Json(name = "name") val name: String
)

data class Users(
        @Json(name = "items") val users: List<User>,
        @Json(name = "has_more") val hasMore: Boolean,
        @Json(name = "quota_remaining") val quotaRemaining: Int
)

data class User(
        @Json(name = "user_id") val userId: Int,
        @Json(name = "answer_count") val answerCount: Int,
        @Json(name = "question_count") val questionCount: Int,
        @Json(name = "reputation") val reputation: Int,
        @Json(name = "location") val location: String?,
        @Json(name = "link") val link: String,
        @Json(name = "profile_image") val profileImage: String?,
        @Json(name = "display_name") val displayName: String,
        val tags: List<String> = emptyList()
) {
    fun hasAnswersEqualOrMoreThan(answerCount: Int) = this.answerCount > answerCount

    fun isFromCountry(vararg countries: String) = location?.let {
        // location can contain city besides country - 'Chisinau, Moldova'/'Moldova, Chișinău'/'Tiraspol, Moldova'
        countries.any { country ->
            location.contains(country, true)
        }
    } ?: false
}