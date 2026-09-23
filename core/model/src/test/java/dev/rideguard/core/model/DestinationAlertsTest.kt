package dev.rideguard.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DestinationAlertsTest {
    @Test
    fun `street warning wins even when the zone is allowed`() {
        val destination = OfferDestination("Palermo", "Av. Rafael Obligado 1234")
        val alert = DestinationAlerts.find(
            destination,
            avoidedZones = listOf("Avellaneda"),
            avoidedStreets = listOf(AvoidedStreet("Avenida Rafael Obligado", "Palermo")),
        )

        assertEquals(DestinationAlert.Kind.STREET, alert?.kind)
    }

    @Test
    fun `street name must not be mistaken for a zone`() {
        val destination = OfferDestination("Palermo", "Avenida Avellaneda 1234")
        assertNull(DestinationAlerts.find(destination, listOf("Avellaneda"), emptyList()))
    }

    @Test
    fun `scoped street requires a confirmed matching zone`() {
        val rule = AvoidedStreet("Calle Interna", "Palermo")
        assertNull(DestinationAlerts.find(OfferDestination(street = "Calle Interna"), emptyList(), listOf(rule)))
        assertNull(DestinationAlerts.find(OfferDestination("Recoleta", "Calle Interna"), emptyList(), listOf(rule)))
        assertEquals(DestinationAlert.Kind.STREET,
            DestinationAlerts.find(OfferDestination("Palermo", "Calle Interna"), emptyList(), listOf(rule))?.kind)
    }

    @Test
    fun `global street still warns if zone is absent`() {
        val alert = DestinationAlerts.find(
            OfferDestination(street = "Juan Pedro de Jujuy"),
            emptyList(),
            listOf(AvoidedStreet("Juan Pedro de Jujuy")),
        )
        assertEquals(DestinationAlert.Kind.STREET, alert?.kind)
    }

    @Test
    fun `matching uses complete normalized names rather than substrings`() {
        assertNull(DestinationAlerts.find(
            OfferDestination("Palermo Chico", "Avenida Rafael Obligado Bis 123"),
            listOf("Palermo"),
            listOf(AvoidedStreet("Avenida Rafael Obligado")),
        ))
        assertEquals(DestinationAlert.Kind.ZONE,
            DestinationAlerts.find(OfferDestination(zone = "Lanús"), listOf("Lanus"), emptyList())?.kind)
    }
}
