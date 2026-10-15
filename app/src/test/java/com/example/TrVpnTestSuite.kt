package com.example

import com.example.data.remote.SeedServers
import com.example.data.remote.VpnGateCsvParser
import com.example.domain.model.AppThemeMode
import com.example.domain.model.OvpnProfile
import com.example.domain.model.VpnProto
import com.example.domain.model.VpnServer
import com.example.domain.model.VpnState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrVpnTestSuite {

    @Test
    fun testBundledSeedServersParsing() {
        val servers = VpnGateCsvParser.parse(SeedServers.BUNDLED_SEED_CSV)
        assertTrue("Seed servers should not be empty", servers.isNotEmpty())
        assertEquals(4, servers.size)

        val first = servers.first()
        assertEquals("Japan", first.countryLong)
        assertEquals("JP", first.countryShort)
        assertEquals("🇯🇵", first.flagEmoji)
        assertNotNull(first.profile)
        assertEquals(VpnProto.UDP, first.profile?.proto)
        assertEquals(1194, first.profile?.remotePort)
        assertTrue(first.profile!!.isValid)
    }

    @Test
    fun testDuplicateIpRemoval() {
        val csvWithDuplicates = """*vpn_servers
#HostName,IP,Score,Ping,Speed,CountryLong,CountryShort,NumVpnSessions,Uptime,TotalUsers,TotalTraffic,LogType,Operator,Message,OpenVPN_ConfigData_Base64
node1.opengw.net,1.1.1.1,100,10,1000,Japan,JP,1,10,1,100,2wk,Op1,Msg,ZGV2IHR1bgpwcm90byB1ZHAKcmVtb3RlIDEuMS4xLjEgMTE5NAo8Y2E+Ci0tLS0tQkVHSU4gQ0VSVElGSUNBVEUtLS0tLQpNSUlCCi0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0KPC9jYT4=
node2.opengw.net,1.1.1.1,200,20,2000,Japan,JP,2,20,2,200,2wk,Op2,Msg,ZGV2IHR1bgpwcm90byB1ZHAKcmVtb3RlIDEuMS4xLjEgMTE5NAo8Y2E+Ci0tLS0tQkVHSU4gQ0VSVElGSUNBVEUtLS0tLQpNSUlCCi0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0KPC9jYT4=
*"""
        val servers = VpnGateCsvParser.parse(csvWithDuplicates)
        assertEquals("Duplicate IP addresses should be deduplicated to 1 entry", 1, servers.size)
        assertEquals("1.1.1.1", servers[0].ip)
    }

    @Test
    fun testInvalidProfileFiltering() {
        // Missing CA certificate
        val invalidBase64 = "ZGV2IHR1bgpwcm90byB1ZHAKcmVtb3RlIDEuMi4zLjQgMTE5NA=="
        val profile = VpnGateCsvParser.parseOvpnProfile(invalidBase64)
        assertNull("Profile without CA cert should be invalid and return null", profile)

        // Invalid base64
        val malformed = VpnGateCsvParser.parseOvpnProfile("!!!NotBase64@@@")
        assertNull("Malformed base64 should return null", malformed)
    }

    @Test
    fun testVpnStateTransitions() {
        val mockServer = VpnServer(
            id = "mock_1",
            hostName = "vpn.example.com",
            ip = "10.0.0.1",
            score = 100,
            publishedPingMs = 25,
            speedBps = 10_000_000,
            countryLong = "United States",
            countryShort = "US",
            numSessions = 5,
            uptimeSeconds = 100,
            totalUsers = 10,
            totalTrafficBytes = 1000,
            logType = "none",
            operator = "TRVPN",
            message = "Test",
            ovpnConfigBase64 = ""
        )

        var state: VpnState = VpnState.Disconnected
        assertFalse(state.isConnected)
        assertFalse(state.isConnectingOrReconnecting)

        state = VpnState.Preparing(mockServer)
        assertTrue(state.isConnectingOrReconnecting)
        assertEquals(mockServer, state.activeServer)

        state = VpnState.Connecting(mockServer, "TLS Handshake")
        assertTrue(state.isConnectingOrReconnecting)

        state = VpnState.Connected(mockServer)
        assertTrue(state.isConnected)
        assertFalse(state.isConnectingOrReconnecting)

        state = VpnState.Disconnecting
        assertTrue(state.isTransitioning)

        state = VpnState.Failed(mockServer, "Handshake timed out", canRetry = true)
        assertEquals("Handshake timed out", (state as VpnState.Failed).message)
        assertTrue((state as VpnState.Failed).canRetry)
    }

    @Test
    fun testCountryFlagEmoji() {
        assertEquals("🇺🇸", VpnServer.countryCodeToEmoji("US"))
        assertEquals("🇯🇵", VpnServer.countryCodeToEmoji("JP"))
        assertEquals("🇩🇪", VpnServer.countryCodeToEmoji("DE"))
        assertEquals("🇰🇷", VpnServer.countryCodeToEmoji("KR"))
        assertEquals("🌐", VpnServer.countryCodeToEmoji("UNKNOWN"))
    }

    @Test
    fun testAppThemeModeValues() {
        assertEquals(3, AppThemeMode.entries.size)
        assertEquals(AppThemeMode.DARK, AppThemeMode.valueOf("DARK"))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.valueOf("LIGHT"))
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.valueOf("SYSTEM"))
    }
}
