import retrofit2.converter.moshi.MoshiConverterFactory

fun main() {
    val moshi = MoshiFactory.makeMoshi()
    val converterFactory = MoshiConverterFactory.create(moshi)
    val stackOverflowService = RetrofitFactory.makeRetrofitService(converterFactory)

    val stackOverflowUsersClient = StackOverflowUsersClient(
            stackOverflowService = stackOverflowService,
            expectedCountries = arrayOf("Moldova", "Romania"),
            minAnswerCount = 1,
            expectedTags = setOf("c#", "java", ",net", "docker")
    )
    println(stackOverflowUsersClient.getUsers())
}