package com.github.noamm9.event.impl

import com.github.noamm9.event.Event
import com.github.noamm9.utils.PartyUtils.OnlineState
import com.github.noamm9.utils.PartyUtils.PartyRole

abstract class PartyEvent: Event(false) {
    object Joined: PartyEvent()
    class Left(val members: List<String>): PartyEvent()

    class MemberJoined(val name: String): PartyEvent()
    class MemberLeft(val name: String, val cause: LeaveCause): PartyEvent()
    class MemberStateChange(val name: String, val state: OnlineState): PartyEvent()

    class RoleChange(val name: String, val role: PartyRole): PartyEvent()

    class Invite(val inviter: String, val invitee: String): PartyEvent()
    class InviteExpired(val invitee: String): PartyEvent()

    object Warp: PartyEvent()

    enum class LeaveCause { LEFT, KICKED, DISCONNECTED }
}