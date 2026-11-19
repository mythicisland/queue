package types

import (
	"fmt"
	"strconv"

	queuev1 "buf.build/gen/go/mythicisland/queue/protocolbuffers/go/mythicisland/queue/v1"
	"github.com/mythicisland/queue/cli/internal/client"
	"github.com/mythicisland/queue/cli/internal/output"
	"github.com/spf13/cobra"
)

func printTable(cmd *cobra.Command, c *client.Client, queueTypes []*queuev1.QueueType) error {
	res, err := c.Data.ListQueueStats(cmd.Context(), &queuev1.ListQueueStatsRequest{})
	if err != nil {
		return err
	}
	stats := make(map[string]*queuev1.QueueStats, len(res.GetStats()))
	for _, s := range res.GetStats() {
		stats[s.GetQueueType()] = s
	}

	active, searching := 0, uint32(0)
	rows := make([][]string, 0, len(queueTypes))
	for _, queueType := range queueTypes {
		s := stats[queueType.GetName()]
		if isActive(queueType) {
			active++
		}
		searching += s.GetSearchingPlayers()
		rows = append(rows, []string{
			queueType.GetName(),
			output.Active(isActive(queueType)),
			queueType.GetServer().GetGroup(),
			mode(queueType),
			players(queueType),
			strconv.FormatUint(uint64(s.GetSearchingPlayers()), 10),
			strconv.FormatUint(uint64(s.GetActiveMatches()), 10),
		})
	}

	w := cmd.OutOrStdout()
	output.Table(w, []string{"NAME", "STATE", "GROUP", "MODE", "PLAYERS", "SEARCHING", "MATCHES"}, rows)
	output.Footer(w, output.Count(len(queueTypes), "queue type"), fmt.Sprintf("%d active", active), fmt.Sprintf("%d searching", searching))
	return nil
}

func mode(queueType *queuev1.QueueType) string {
	if queueType.GetServer().HasShared() {
		return "shared"
	}
	return "exclusive"
}

func players(queueType *queuev1.QueueType) string {
	match := queueType.GetMatch()
	if match.HasTeams() {
		return fmt.Sprintf("%dx%d", match.GetTeams().GetCount(), match.GetTeams().GetSize())
	}
	if match.GetMinPlayers() == match.GetMaxPlayers() {
		return strconv.FormatUint(uint64(match.GetMaxPlayers()), 10)
	}
	return fmt.Sprintf("%d-%d", match.GetMinPlayers(), match.GetMaxPlayers())
}

func isActive(queueType *queuev1.QueueType) bool {
	return !queueType.HasActive() || queueType.GetActive()
}
