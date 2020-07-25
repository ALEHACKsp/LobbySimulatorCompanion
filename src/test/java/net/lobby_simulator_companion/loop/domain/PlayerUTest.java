package net.lobby_simulator_companion.loop.domain;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author NickyRamone
 */
public class PlayerUTest {

    @Test
    public void addName__shouldTrimSpaces() {
        // arrange
        Player player = new Player();

        // act
        player.addName("  \t  dummy player name\t\n  ");

        // assert
        assertThat(player.getMostRecentName().get(), equalTo("dummy player name"));
    }

    @Test
    public void addName__whenNameIsNew_andLimitNotReached_thenNameShouldBeAddedLast() {
        // arrange
        Player player = new Player();
        player.addName("dummy name 1");
        player.addName("dummy name 2");
        player.addName("dummy name 3");
        player.addName("dummy name 4");
        player.addName("dummy name 5");
        player.addName("dummy name 6");
        player.addName("dummy name 7");
        player.addName("dummy name 8");
        player.addName("dummy name 9");

        // act
        player.addName("dummy name 10");

        // assert
        List<String> expectedNames = Arrays.asList(
                "dummy name 1", "dummy name 2", "dummy name 3", "dummy name 4", "dummy name 5", "dummy name 6",
                "dummy name 7", "dummy name 8", "dummy name 9", "dummy name 10"
        );
        assertThat(player.getNames(), equalTo(expectedNames));
        assertThat(player.getMostRecentName().get(), equalTo("dummy name 10"));
    }


    @Test
    public void addName__whenNameIsNew_andLimitReached_thenNameShouldBeAddedLast_andFirstNameShouldBeRemoved() {
        // arrange
        Player player = new Player();
        player.addName("dummy name 1");
        player.addName("dummy name 2");
        player.addName("dummy name 3");
        player.addName("dummy name 4");
        player.addName("dummy name 5");
        player.addName("dummy name 6");
        player.addName("dummy name 7");
        player.addName("dummy name 8");
        player.addName("dummy name 9");
        player.addName("dummy name 10");

        // act
        player.addName("dummy name 11");

        // assert
        List<String> expectedNames = Arrays.asList(
                "dummy name 2", "dummy name 3", "dummy name 4", "dummy name 5", "dummy name 6",
                "dummy name 7", "dummy name 8", "dummy name 9", "dummy name 10", "dummy name 11"
        );
        assertThat(player.getNames(), equalTo(expectedNames));
        assertThat(player.getMostRecentName().get(), equalTo("dummy name 11"));
    }

    @Test
    public void addName__whenNameIsRepeated_shouldMoveNameToEnd() {
        // arrange
        Player player = new Player();
        player.addName("dummy name 1");
        player.addName("dummy name 2");
        player.addName("dummy name 3");
        player.addName("dummy name 4");

        // act
        player.addName("dummy name 2");

        // assert
        assertThat(player.getNames(), equalTo(Arrays.asList("dummy name 1", "dummy name 3", "dummy name 4", "dummy name 2")));
        assertThat(player.getMostRecentName().get(), equalTo("dummy name 2"));
    }

}
