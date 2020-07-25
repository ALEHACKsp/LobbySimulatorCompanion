package net.lobby_simulator_companion.loop.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author NickyRamone
 */
@RequiredArgsConstructor
@Getter
public enum RealmMap {

    UNIDENTIFIED(null, "?"),

    // Autohaven Wreckers
    AZAROVS_RESTING_PLACE("Jnk_Office", "Azarov's Resting Place"),
    BLOOD_LODGE("Jnk_Lodge", "Blood Lodge"),
    GAS_HEAVEN("Jnk_GasStation", "Gas Heaven"),
    WRECKERS_YARD("Jnk_Scrapyard", "Wrecker's Yard"),
    WRETCHED_SHOP("Jnk_Garage", "Wretched Shop"),

    // Backwater Swamp
    PALE_ROSE("Swp_PaleRose", "The Pale Rose"),
    GRIM_PANTRY("Swp_GrimPantry", "Grim Pantry"),

    // Coldwind Farm
    FRACTURED_COWSHED("Frm_Barn", "Fractured Cowshed"),
    RANCID_ABATTOIR("Frm_Slaughterhouse", "Rancid Abattoir"),
    ROTTEN_FIELDS("Frm_Cornfield", "Rotten Fields"),
    THOMPSON_HOUSE("Frm_Farmhouse", "The Thompson House"),
    TORMENT_CREEK("Frm_Silo", "Torment Creek"),

    // Crotus Prenn Asylum
    DISTURBED_WARD("Asy_Asylum", "Disturbed Ward"),
    FATHER_CAMPBELLS_CHAPEL("Asy_Chapel", "Father Campbell's Chapel"),

    // Gideon Meat Plant
    THE_GAME("Fin_Hideout", "The Game"),

    // Grave of Glenvale
    DEAD_DAWG_SALOON("Ukr_Saloon", "Dead Dawg Saloon"),

    // Haddonfield
    LAMPKIN_LANE("Sub_Street", "Lampkin Lane"),

    // Hawkins National Laboratory
    UNDERGROUND_COMPLEX("Qat_Lab", "The Underground Complex"),

    // Lery's Memorial Institute
    TREATMENT_THEATRE("Hos_Treatment", "Treatment Theatre"),

    // MacMillan Estate
    COAL_TOWER("Ind_CoalTower", "Coal Tower"),
    GROANING_STOREHOUSE("Ind_Storehouse", "Groaning Storehouse"),
    IRONWORKS_OF_MISERY("Ind_Foundry", "Ironworks of Misery"),
    SHELTER_WOODS("Ind_Forest", "Shelter Woods"),
    SUFFOCATION_PIT("Ind_Mine", "Suffocation Pit"),

    // Ormond
    MOUNT_ORMOND_RESORT("Kny_Cottage", "Mount Ormond Resort"),

    // Red Forest
    MOTHERS_DWELLING("Brl_MaHouse", "Mother's Dwelling"),
    TEMPLE_OF_PURGATION("Brl_Temple", "The Temple of Purgation"),

    // Silent Hill
    MIDWICH_ELEMENTARY_SCHOOL("Wal_Level_01", "Midwich Elementary School"),

    // Springwood
    BADHAM_PRESCHOOL_1("Eng_Street_01", "Badham Preschool I"),
    BADHAM_PRESCHOOL_2("Eng_Street_02", "Badham Preschool II"),
    BADHAM_PRESCHOOL_3("Eng_Street_03", "Badham Preschool III"),
    BADHAM_PRESCHOOL_4("Eng_Street_04", "Badham Preschool IV"),
    BADHAM_PRESCHOOL_5("Eng_Street_05", "Badham Preschool V"),

    // Yamaoka Estate
    FAMILY_RESIDENCE("Hti_Manor", "Family Residence"),
    SANCTUM_OF_WRATH("Hti_Shrine", "Sanctum of Wrath"),
    ;

    private final String id;
    private final String description;

    public boolean isIdentified() {
        return this != UNIDENTIFIED;
    }

    @Override
    public String toString() {
        return description;
    }

}
