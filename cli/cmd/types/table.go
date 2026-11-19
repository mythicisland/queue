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
	searching := make(map[string]uint32, len(res.GetStats()))
	for _, stats := range res.GetStats() {
		searching[stats.GetQueueType()] = stats.GetSearchingPlayers()
	}

	rows := make([][]string, 0, len(queueTypes))
	for _, queueType := range queueTypes {
		rows = append(rows, []string{
			queueType.GetName(),
			queueType.GetServer().GetGroup(),
			mode(queueType),
			players(queueType),
			strconv.FormatUint(uint64(searching[queueType.GetName()]), 10),
			strconv.FormatBool(isActive(queueType)),
		})
	}
	output.Table([]string{"NAME", "GROUP", "MODE", "PLAYERS", "SEARCHING", "ACTIVE"}, rows)
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
