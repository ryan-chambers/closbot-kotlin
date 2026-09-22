package com.ryanthink.closbotkt.core.di

import javax.inject.Inject

/**
 * Trivial class with no dependencies of its own, injected into [com.ryanthink.closbotkt.MainActivity]
 * purely to prove the Hilt graph is wired end to end. Delete once a real injected dependency exists.
 */
class Greeter @Inject constructor() {
    fun greet(): String = "Hilt wiring works"
}
