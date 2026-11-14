package com.ryanthink.closbotkt.data.knowledge.pinecone

fun pineconeBaseUrl(host: String): String {
    if (host.trim().isEmpty()) {
        throw IllegalArgumentException("Expected PINECONE_HOST")
    }

    var baseUrl = host.trim()
    if (!baseUrl.startsWith("http://", true)) {
        if (!baseUrl.startsWith("https://", true)) {
            baseUrl = "https://$baseUrl"
        }
    }
    return baseUrl.trim().trimEnd('/') + "/"
}
