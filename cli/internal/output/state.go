package output

import (
	"strings"

	queuev1 "buf.build/gen/go/mythicisland/queue/protocolbuffers/go/mythicisland/queue/v1"
	"charm.land/lipgloss/v2"
)

var ticketStates = map[queuev1.TicketState]lipgloss.Style{
	queuev1.TicketState_TICKET_STATE_SEARCHING: Yellow,
	queuev1.TicketState_TICKET_STATE_MATCHED:   Accent,
	queuev1.TicketState_TICKET_STATE_ASSIGNED:  Green,
}

var matchStates = map[queuev1.MatchState]lipgloss.Style{
	queuev1.MatchState_MATCH_STATE_ALLOCATING:   Yellow,
	queuev1.MatchState_MATCH_STATE_OFFERED:      Yellow,
	queuev1.MatchState_MATCH_STATE_COUNTDOWN:    Accent,
	queuev1.MatchState_MATCH_STATE_TRANSFERRING: Accent,
	queuev1.MatchState_MATCH_STATE_COMPLETED:    Green,
	queuev1.MatchState_MATCH_STATE_FAILED:       Red,
	queuev1.MatchState_MATCH_STATE_CANCELLED:    Faint,
}

func TicketState(state queuev1.TicketState) string {
	return Dot(styleOf(ticketStates, state), TicketStateName(state))
}

func TicketStateName(state queuev1.TicketState) string {
	return strings.ToLower(strings.TrimPrefix(state.String(), "TICKET_STATE_"))
}

func MatchState(state queuev1.MatchState) string {
	return Dot(styleOf(matchStates, state), MatchStateName(state))
}

func MatchStateName(state queuev1.MatchState) string {
	return strings.ToLower(strings.TrimPrefix(state.String(), "MATCH_STATE_"))
}

func styleOf[T comparable](styles map[T]lipgloss.Style, state T) lipgloss.Style {
	style, ok := styles[state]
	if !ok {
		return Faint
	}
	return style
}
