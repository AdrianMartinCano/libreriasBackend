package dev.pimon.dining.dto;

import dev.pimon.dining.entity.DiningReservation;
import dev.pimon.dining.entity.DiningTable;
import dev.pimon.dining.entity.ReservationStatus;
import dev.pimon.dining.entity.SpecialRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/** Vista completa de una reserva de mesa (panel admin / detalle). */
public record DiningReservationDto(
        String              id,
        String              date,        // ISO 'YYYY-MM-DD'
        String              seatingId,
        String              seatingName,
        String              start,
        String              end,
        int                 partySize,
        List<TableRef>      tables,
        String              clientName,
        String              clientEmail,
        String              clientPhone,
        String              notes,
        Set<SpecialRequest> requests,
        ReservationStatus   status,
        BigDecimal          deposit
) {
    public record TableRef(String id, String label, String zoneName) {}

    public static DiningReservationDto from(DiningReservation r) {
        List<TableRef> tables = r.getTables().stream()
                .map(DiningReservationDto::tableRef)
                .toList();
        return new DiningReservationDto(
                r.getId(), r.getDate().toString(),
                r.getSeating().getId(), r.getSeating().getName(),
                r.getSeating().getStart(), r.getSeating().getEnd(),
                r.getPartySize(), tables,
                r.getClientName(), r.getClientEmail(), r.getClientPhone(), r.getNotes(),
                r.getRequests(), r.getStatus(), r.getDeposit());
    }

    private static TableRef tableRef(DiningTable t) {
        return new TableRef(t.getId(), t.getLabel(),
                t.getZone() != null ? t.getZone().getName() : null);
    }
}
