package com.ccat.serverscanner.model

data class ServerPingResponse(
    val description: Description,
    val players: Players,
    val version: Version,
    val favicon: String?
)

data class Description(
    val text: String
)

data class Players(
    val max: Int,
    val online: Int,
    val sample: List<Player>
)

data class Player(
    val name: String,
    val id: String
)

data class Version(
    val name: String,
    val protocol: Int
)