package com.laundryhub.backend.web.dto;

import java.math.BigDecimal;

import com.laundryhub.backend.domain.catalog.Soap;

/**
 * A soap card (03) or A3 table row.
 *
 * @param id          soap id
 * @param name        e.g. "Hypoallergenic"
 * @param description card text
 * @param addOnPerKg  pesos per kilo
 * @param included    whether the add-on is 0 (card shows "Included")
 * @param available   A3 "Available" checkbox
 * @param tintHex     icon tile color
 * @param sortOrder   display position
 */
public record SoapResponse(Long id, String name, String description, BigDecimal addOnPerKg, boolean included,
                           boolean available, String tintHex, int sortOrder) {

    /**
     * @param soap a soap
     * @return the DTO
     */
    public static SoapResponse from(Soap soap) {
        return new SoapResponse(soap.getId(), soap.getName(), soap.getDescription(), soap.getAddOnPerKg(),
                soap.isIncluded(), soap.isAvailable(), soap.getTintHex(), soap.getSortOrder());
    }
}
