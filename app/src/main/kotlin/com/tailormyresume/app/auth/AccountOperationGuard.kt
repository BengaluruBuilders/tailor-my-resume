package com.tailormyresume.app.auth

import com.tailormyresume.core.data.account.LocalWorkFence
import com.tailormyresume.core.domain.FirebaseUidProvider
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountOperationGuard @Inject constructor(private val uids: FirebaseUidProvider, private val fence: LocalWorkFence) {
    data class Ticket(val owner: String?, val generation: Long)

    suspend fun capture(): Ticket = Ticket(uids.uid(), fence.currentTicket()).also(::check)

    fun check(ticket: Ticket) {
        fence.check(ticket.generation)
        if (ticket.owner == null || ticket.owner != uids.uid()) throw CancellationException("Account operation changed")
    }
}
