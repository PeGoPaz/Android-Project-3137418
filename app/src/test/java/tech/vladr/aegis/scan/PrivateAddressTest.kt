package tech.vladr.aegis.scan

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateAddressTest {
    @Test
    fun anAddressFromAMakersBlockIsNotPrivate() {
        assertFalse(isPrivateAddress("ac:f8:cc:05:0f:a8"))
        assertFalse(isPrivateAddress("00:1a:2b:3c:4d:5e"))
    }

    @Test
    fun theSecondHexDigitTellsAMadeUpAddress() {
        // 2, 6, A and E are the digits with the "locally administered" bit and not the group bit.
        for (digit in listOf('2', '6', 'a', 'e')) assertTrue("x$digit", isPrivateAddress("d${digit}:a1:19:7c:30:02"))
        for (digit in listOf('0', '4', '8', 'c')) assertFalse("x$digit", isPrivateAddress("d${digit}:a1:19:7c:30:02"))
    }

    @Test
    fun caseAndSpacesDoNotMatter() {
        assertTrue(isPrivateAddress(" DA:A1:19:7C:30:02 "))
    }

    @Test
    fun groupAddressesAndNonsenseAreNotPrivate() {
        assertFalse(isPrivateAddress("03:00:00:00:00:01"))
        assertFalse(isPrivateAddress("01:00:5e:00:00:fb"))
        assertFalse(isPrivateAddress(""))
        assertFalse(isPrivateAddress("zz:00:00:00:00:00"))
    }
}
