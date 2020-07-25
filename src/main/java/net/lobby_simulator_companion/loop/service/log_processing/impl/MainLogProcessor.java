package net.lobby_simulator_companion.loop.service.log_processing.impl;

import lombok.extern.slf4j.Slf4j;
import net.lobby_simulator_companion.loop.service.log_processing.DbdLogEvent;
import net.lobby_simulator_companion.loop.service.log_processing.MultiPurposeDbdLogProcessor;
import net.lobby_simulator_companion.loop.util.event.EventSupport;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.lobby_simulator_companion.loop.service.DbdLogMonitor.State;
import static net.lobby_simulator_companion.loop.service.DbdLogMonitor.StateWrapper;

/**
 * @author NickyRamone
 */
@Slf4j
public class MainLogProcessor extends MultiPurposeDbdLogProcessor {

    private static final String REGEX__SERVER_CONNECT = "UPendingNetGame::SendInitialJoin.+RemoteAddr: "
            + "([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})(?::([0-9]{1,5}))?";
    private static final Pattern PATTERN__SERVER_CONNECT = Pattern.compile(REGEX__SERVER_CONNECT);

    private static final String REGEX__MATCH_WAIT = "(POST https://.+?/api/v1/queue\\])|"
            + "(\\[PartyContextComponent::UpdateReadyButtonStateInfo\\] Ready button updated : 1)";
    private static final Pattern PATTERN__MATCH_WAIT = Pattern.compile(REGEX__MATCH_WAIT);

    private static final String REGEX__MATCH_WAIT_CANCEL = "RESPONSE: code 200.+?POST https://.+?/api/v1/queue/cancel\\]";
    private static final Pattern PATTERN__MATCH_WAIT_CANCEL = Pattern.compile(REGEX__MATCH_WAIT_CANCEL);

    private static final String REGEX__MATCH_END = "GameFlow: ADBDGameState::SetGameLevelEnded.+reason '([^']+)'";
    private static final Pattern PATTERN__MATCH_END = Pattern.compile(REGEX__MATCH_END);


    public MainLogProcessor(EventSupport eventSupport) {
        super(eventSupport);
        addLineProcessors(Arrays.asList(
                this::checkForServerConnect,
                this::checkForMatchWait,
                this::checkForMatchWaitCancel,
                this::checkForRealmEnter,
                this::checkForMatchStart,
                this::checkForSurvival,
                this::checkForUserLeavingRealm,
                this::checkForMatchEnd,
                this::checkForServerDisconnect
        ));
    }


    private Boolean checkForServerConnect(String logLine, StateWrapper stateWrapper) {
        if (stateWrapper.state != State.SEARCHING_LOBBY) {
            return false;
        }

        Matcher matcher = PATTERN__SERVER_CONNECT.matcher(logLine);
        if (!matcher.find()) {
            return false;
        }

        stateWrapper.state = State.IN_LOBBY;
        String serverAddress = matcher.group(1);
        int serverPort = matcher.group(2) != null ? Integer.valueOf(matcher.group(2)) : 0;
        fireEvent(DbdLogEvent.SERVER_CONNECT, InetSocketAddress.createUnresolved(serverAddress, serverPort));

        return true;
    }


    private Boolean checkForMatchWait(String logLine, StateWrapper stateWrapper) {
        if (stateWrapper.state != State.IDLE) {
            return false;
        }

        Matcher matcher = PATTERN__MATCH_WAIT.matcher(logLine);
        if (!matcher.find()) {
            return false;
        }

        stateWrapper.state = State.SEARCHING_LOBBY;
        fireEvent(DbdLogEvent.MATCH_WAIT, null);

        return true;
    }

    private Boolean checkForMatchWaitCancel(String logLine, StateWrapper stateWrapper) {
        if (stateWrapper.state != State.SEARCHING_LOBBY && stateWrapper.state != State.IN_LOBBY) {
            return false;
        }

        Matcher matcher = PATTERN__MATCH_WAIT_CANCEL.matcher(logLine);

        if (matcher.find()
                || logLine.contains("[MirrorsSocialPresence::DestroyParty]")
                || logLine.contains("[PartyContextComponent::OnQuickmatchComplete] result : UnknownError")
                || logLine.contains("[UDBDGameInstance::RegisterDisconnectError]") // NAT error?
        ) {
            stateWrapper.state = State.IDLE;
            fireEvent(DbdLogEvent.MATCH_WAIT_CANCEL, null);
            return true;
        }

        return false;
    }


    private Boolean checkForRealmEnter(String logLine, StateWrapper stateWrapper) {
        if (stateWrapper.state != State.IN_LOBBY) {
            return false;
        }

        if (logLine.contains("GameFlow: ACollectable::BeginPlay")) {
            fireEvent(DbdLogEvent.REALM_ENTER);
            return true;
        }
        return false;
    }

    private Boolean checkForMatchStart(String logLine, StateWrapper stateWrapper) {
        if (stateWrapper.state != State.IN_LOBBY) {
            return false;
        }

        if (logLine.contains("^^^ OnEnteringOnlineMultiplayer ^^^")) {
            stateWrapper.state = State.IN_MATCH;
            fireEvent(DbdLogEvent.MATCH_START, null);
            return true;
        }
        return false;
    }

    private Boolean checkForSurvival(String logLine, StateWrapper stateWrapper) {
        if (stateWrapper.state != State.IN_MATCH) {
            return false;
        }
        if (logLine.contains("player escaped = true") || logLine.contains("DBD_EscapeThroughHatch: 1")) {
            fireEvent(DbdLogEvent.SURVIVED);
            return true;
        }
        return false;
    }


    private Boolean checkForUserLeavingRealm(String logLine, StateWrapper stateWrapper) {
        if (stateWrapper.state != State.IN_MATCH) {
            return false;
        }

        if (logLine.contains("/api/v1/softWallet/put/analytics")) {
            stateWrapper.state = State.IN_POST_GAME_CHAT;
            fireEvent(DbdLogEvent.USER_LEFT_REALM, null);
            return true;
        }
        return false;
    }

    private Boolean checkForMatchEnd(String logLine, StateWrapper stateWrapper) {
        if (stateWrapper.state != State.IN_POST_GAME_CHAT && stateWrapper.state != State.IN_MATCH) {
            return false;
        }

        Matcher matcher = PATTERN__MATCH_END.matcher(logLine);
        if (matcher.find()) {
            String reason = matcher.group(1);
            boolean killerQuit = "KillerLeft".equals(reason);

            if (killerQuit) {
                fireEvent(DbdLogEvent.SURVIVED);
                fireEvent(DbdLogEvent.USER_LEFT_REALM);
            }
            fireEvent(DbdLogEvent.MATCH_END, !killerQuit);
            return true;
        }
        return false;
    }

    private Boolean checkForServerDisconnect(String logLine, StateWrapper stateWrapper) {
        if (stateWrapper.state != State.IN_MATCH && stateWrapper.state != State.IN_POST_GAME_CHAT && stateWrapper.state != State.IN_LOBBY) {
            return false;
        }

        // the first check detects disconnection while the second detects leaving the post-game chat screen
        if (logLine.contains("SetIsDisconnected from: false to: true") ||
                logLine.contains("FOnlineAsyncTaskMirrorsDestroyMatch")) {

            stateWrapper.state = State.IDLE;
            fireEvent(DbdLogEvent.SERVER_DISCONNECT, null);
            return true;
        }
        return false;
    }

}
