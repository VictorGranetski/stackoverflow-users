class StackOverflowUsersClient(
        val stackOverflowService: StackOverflowService,
        val expectedCountries: Array<String> = arrayOf("Moldova", "Romania"),
        val minAnswerCount: Int = 1,
        val expectedTags: Set<String> = setOf("c#", "java", ".net", "docker")
) {
    private var requests = 0
    private var page = 1
    private var quotaRemaining = 10000
    private var hasMore = true

    /**
     * documentation says that 30 req per sec are allowed https://api.stackexchange.com/docs/throttle
     * but in practice even 10 requests per sec are banned after about 200 requests
     * doing 5 req per sec ip is banned after about 750 requests
     * ban lasts from 30 sec to few minutes
     */
    fun getUsers(): List<User> {
        val sequence = sequence {
            while (hasMore && quotaRemaining >= 2 && requests < 700) {
                if (requests > 0 && requests % 5 == 0) {
                    println(requests)
                    Thread.sleep(1000)
                }
                // retrieve users
                val filteredUsers = stackOverflowService.listUsers(page).execute().body()!!.also {
                    quotaRemaining = it.quotaRemaining
                    hasMore = it.hasMore
                    requests++
                }.users
                        .filter { user -> user.isFromCountry(*expectedCountries) }
                        .filter { user -> user.hasAnswersEqualOrMoreThan(minAnswerCount) }
                        .let { users -> getUsersWithExpectedTags(users) }

                page++
                yieldAll(filteredUsers)
            }
        }
        return sequence.toList()
    }

    private fun getUsersWithExpectedTags(users: List<User>): Set<User> {
        val usersWithTags = mutableSetOf<User>()
        var index = 0
        while (quotaRemaining > 0 && index < users.size && requests < 700) {
            if (requests > 0 && requests % 5 == 0) {
                println(requests)
                Thread.sleep(1000)
            }
            val user = users[index]
            // taking into consideration only first 100 tags ordered by popularity due to quota
            val userTags = stackOverflowService.listUserTags(user.userId).execute().body()!!.also {
                quotaRemaining = it.quotaRemaining
                requests++
            }
            if (userTags.containAtLeastOneTag(expectedTags)) {
                usersWithTags += user.copy(tags = userTags.tags.map { it.name })
            }

            index++
        }
        return usersWithTags
    }

}