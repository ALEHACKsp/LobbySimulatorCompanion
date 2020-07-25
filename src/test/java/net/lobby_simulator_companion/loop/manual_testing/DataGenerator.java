package net.lobby_simulator_companion.loop.manual_testing;

import lombok.extern.slf4j.Slf4j;
import net.lobby_simulator_companion.loop.Factory;
import net.lobby_simulator_companion.loop.domain.Killer;
import net.lobby_simulator_companion.loop.domain.Player;
import net.lobby_simulator_companion.loop.domain.RealmMap;
import net.lobby_simulator_companion.loop.domain.stats.Match;
import net.lobby_simulator_companion.loop.service.LoopDataService;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

/**
 * @author NickyRamone
 */
@Slf4j
public class DataGenerator {

    private static final Random random = new Random();
    private static final LoopDataService dataService = Factory.loopDataService();


    public static void main(String[] args) {
        DataGenerator dg = new DataGenerator();
        Instant startInstant = Instant.now();
        dg.addPlayers(2000);
        dg.addMatches(2000);
        dataService.save();
        log.info("Elapsed: {} seconds", Duration.between(startInstant, Instant.now()).toMillis() / 1000.0);
    }

    private void addPlayers(int n) {
        for (int i = 0; i < n; i++) {
            dataService.addPlayer(randomPlayer());
        }
    }

    private void addMatches(int n) {
        for (int i = 0; i < n; i++) {
            dataService.addMatch(randomMatch());
        }
    }


    private Player randomPlayer() {
        String steamId = UUID.randomUUID().toString();
        int matchesPlayed = random.nextInt(20);

        return Player.builder()
                .steamId64(steamId)
                .dbdPlayerId(UUID.randomUUID().toString())
                .names(Arrays.asList("dummy name " + steamId))
                .description("dummy description for " + steamId)
                .rating(randomPlayerRating())
                .escapes(random.nextInt(10))
                .deaths(random.nextInt(101))
                .firstSeen(randomDate())
                .lastSeen(randomDate())
                .matchesPlayed(random.nextInt(20))
                .timesEncountered(random.nextInt(20))
                .secondsPlayed(matchesPlayed * randomInt(180, 1200))
                .build();
    }


    private Match randomMatch() {
        int secondsQueued = random.nextInt(180);
        int secondsWaited = secondsQueued + randomInt(60, 120);

        return Match.builder()
                .secondsQueued(secondsQueued)
                .secondsWaited(secondsWaited)
                .secondsPlayed(randomInt(3 * 60, 20 * 60))
                .lobbiesFound(1)
                .killerPlayerDbdId(UUID.randomUUID().toString())
                .killerPlayerSteamId64(UUID.randomUUID().toString())
                .killer(randomKiller())
                .realmMap(randomMap())
                .escaped(random.nextBoolean())
                .matchStartTime(randomDate())
                .build();
    }

    private int randomInt(int min, int max) {
        return random.nextInt((max - min) + 1) + min;
    }

    private Killer randomKiller() {
        int killerCount = Killer.values().length;

        return Killer.values()[randomInt(1, killerCount - 1)];
    }

    private RealmMap randomMap() {
        int mapCount = RealmMap.values().length;

        return RealmMap.values()[randomInt(1, mapCount - 1)];
    }

    private LocalDateTime randomDate() {
        return LocalDateTime.of(
                randomInt(2000, 2020),
                randomInt(1, 12),
                randomInt(1, 28),
                randomInt(0, 23),
                randomInt(0, 59),
                randomInt(0, 59),
                0
        );
    }

    private Player.Rating randomPlayerRating() {
        int score = random.nextInt(3);

        switch (score) {
            case 1:
                return Player.Rating.THUMBS_DOWN;
            case 2:
                return Player.Rating.UNRATED;
            default:
                return Player.Rating.THUMBS_UP;
        }
    }
}
