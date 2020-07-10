import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import retrofit2.*
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

fun main() {
    val moshi = MoshiFactory.makeMoshi()
    val converterFactory = MoshiConverterFactory.create(moshi)
    val stackOverflowService = RetrofitFactory.makeRetrofitService(converterFactory)

    val atomicPage = AtomicInteger()
    val quotaRemaining = AtomicInteger(10000)
    val hasMore = AtomicBoolean(true)
    val sequence = sequence {
        while (hasMore.get() && quotaRemaining.get() >= 2) {
            yield(GlobalScope.async {
                // more than 30 req per seq are banned https://api.stackexchange.com/docs/throttle
                if (quotaRemaining.get() % 30 == 0) {
                    delay(1000)
                }
                // retrieve users
                val filteredUsers = stackOverflowService.listUsers(atomicPage.incrementAndGet()).also {
                    quotaRemaining.set(it.quotaRemaining)
                    hasMore.set(it.hasMore)
                }.users
                        .filter { user -> isExpectedCountry(user.location) }
                        .filter { user -> user.hasAnswers() }

                val usersWithTags = mutableListOf<User>()
                var index = 0
                while (quotaRemaining.get() > 0 && index < filteredUsers.size) {
                    if (quotaRemaining.get() % 30 == 0) {
                        delay(1000)
                    }
                    val user = filteredUsers[index]
                    // taking into consideration only first 100 tags ordered by popularity due to quota
                    val userTags = stackOverflowService.listUserTags(user.userId)
                    if (userTags.tags.any(isExpectedTag())) {
                        usersWithTags += user.copy(tags = userTags.tags.map { it.name })
                    }

                    quotaRemaining.set(userTags.quotaRemaining)
                    index++
                }

                usersWithTags
            })
        }
    }
    val users = sequence.map {
        runBlocking {
            it.await()
        }
    }.flatMap { it.asSequence() }.toSet()
    println(users)
}

val expectedCountries = setOf("moldova", "romania")

private fun isExpectedCountry(location: String?): Boolean {
    return location?.let {
        expectedCountries.any { location.contains(it, true) }
    } ?: false
}

val expectedTags = setOf("c#", "java", ".net", "docker")

private fun isExpectedTag(): (Tag) -> Boolean {
    return { tag ->
        expectedTags.contains(tag.name.toLowerCase())
    }
}

object MoshiFactory {
    fun makeMoshi(): Moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
}

object RetrofitFactory {
    private const val BASE_URL = "https://api.stackexchange.com/2.2/"

    fun makeRetrofitService(converterFactory: Converter.Factory): StackOverflowService {
        val httpClient = OkHttpClient.Builder().apply {
            addInterceptor { chain ->
                val originalRequest = chain.request()
                val originalHttpUrl = originalRequest.url()
                val newUrl = originalHttpUrl.newBuilder()
                        .addQueryParameter("key", "uA5ggX4ruf*YR6hWG1Etnw((")
                        .addQueryParameter("site", "stackoverflow")
                        .build()

                val newRequest = originalRequest.newBuilder()
                        .url(newUrl)
                        .build()
                chain.proceed(newRequest)
            }
        }
        return Retrofit.Builder()
                .client(httpClient.build())
                .baseUrl(BASE_URL)
                .addConverterFactory(converterFactory)
                .build().create(StackOverflowService::class.java)
    }
}

interface StackOverflowService {
    @GET("users?order=desc&sort=reputation")
    suspend fun listUsers(
            @Query("page") page: Int = 1,
            @Query("pagesize") pageSize: Int = 100,
            @Query("min") reputation: Int = 223,
            @Query("filter") filter: String = "!LnNkvq16GJbfkXKi.EM)4I"
    ): Users

    @GET("users/{ids}/tags?order=desc&sort=popular")
    suspend fun listUserTags(
            @Path("ids") ids: Int,
            @Query("page") page: Int = 1,
            @Query("pagesize") pageSize: Int = 100,
            @Query("filter") filter: String = "!-.G.68h(ttpy"
    ): Tags
}

data class Tags(
        @Json(name = "items") val tags: List<Tag>,
        @Json(name = "has_more") val hasMore: Boolean,
        @Json(name = "quota_remaining") val quotaRemaining: Int
)

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
        @Json(name = "profile_image") val profileImage: String,
        @Json(name = "display_name") val displayName: String,
        val tags: List<String> = emptyList()
) {
    fun hasAnswers() = answerCount > 0
}