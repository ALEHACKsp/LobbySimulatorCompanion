package net.lobby_simulator_companion.loop.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

/**
 * @author NickyRamone
 */
@RequiredArgsConstructor
@Getter
public enum Survivor {

    UNIDENTIFIED(null, null),
    DWIGHT_FAIRFIELD("CamperMale01", "Dwight"),
    MEG_THOMAS("CamperFemale01", "Meg"),
    CLAUDETTE_MOREL("CamperFemale02", "Claudette"),
    JAKE_PARK("CamperMale02", "Jake"),
    NEA_KARLSSON("CamperFemale03", "Nea"),
    LAURIE_STRODE("CamperFemale04", "Laurie"),
    ACE_VISCONTI("CamperMale03", "Ace"),
    BILL_OVERBECK("CamperMale04", "Bill"),
    FENG_MIN("CamperFemale05", "Feng"),
    DAVID_KING("CamperMale05", "David"),
    QUENTIN_SMITH("CamperMale06", "Quentin"),
    DAVID_TAPP("CamperMale07", "Tapp"),
    KATE_DENSON("CamperFemale06", "Kate"),
    ADAM_FRANCIS("CamperMale08", "Adam"),
    JEFF_JOHANSEN("CamperMale09", "Jeff"),
    JANE_ROMERO("CamperFemale07", "Jane"),
    ASHLEY_WILLIAMS("CamperMale10", "Ash"),
    NANCY_WHEELER("CamperFemale08", "Nancy"),
    STEVE_HARRINGTON("CamperMale11", "Steve"),
    YUI_KIMURA("CamperFemale09", "Yui"),
    ZARINA_KASSIR("CamperFemale10", "Zarina"),
    CHERYL_MASON("CamperFemale11", "Cheryl");

    private final String blueprintId;
    private final String alias;

    public static Survivor fromBlueprintId(String blueprintId) {
        return Stream.of(values())
                .filter(s -> s != UNIDENTIFIED && s.blueprintId.equals(blueprintId))
                .findFirst()
                .orElse(UNIDENTIFIED);
    }

    @Override
    public String toString() {
        return alias;
    }
}
