package com.matteo.chatapp.viewmodel

import com.matteo.chatapp.model.Server
import com.matteo.chatapp.model.ServerMember
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerUiStateTest {

    @Test
    fun upsertReplacesExistingServerWithoutCreatingDuplicates() {
        val original = server(id = "1", name = "Original")
        val other = server(id = "2", name = "Otro")
        val updated = original.copy(name = "Actualizado")

        assertEquals(
            listOf(updated, other),
            listOf(original, other).upsertServer(updated)
        )
    }

    @Test
    fun upsertAddsServerWhenItDoesNotExist() {
        val first = server(id = "1", name = "Uno")
        val second = server(id = "2", name = "Dos")

        assertEquals(listOf(first, second), listOf(first).upsertServer(second))
    }

    @Test
    fun replacingMemberKeepsRoleAndNameOrdering() {
        val owner = member(id = "1", username = "Zeta", role = "owner")
        val member = member(id = "2", username = "Ana", role = "member")
        val admin = member.copy(role = "admin")

        assertEquals(
            listOf(owner, admin),
            listOf(member, owner).replaceAndSortMember(admin)
        )
    }

    private fun server(id: String, name: String) = Server(
        id = id,
        name = name,
        ownerUsername = "matteo"
    )

    private fun member(id: String, username: String, role: String) = ServerMember(
        userId = id,
        username = username,
        role = role
    )
}
