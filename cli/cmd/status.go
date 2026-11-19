package cmd

import (
	"fmt"

	queuev1 "buf.build/gen/go/mythicisland/queue/protocolbuffers/go/mythicisland/queue/v1"
	"github.com/mythicisland/queue/cli/internal/client"
	"github.com/mythicisland/queue/cli/internal/output"
	"github.com/spf13/cobra"
)

func newStatusCommand() *cobra.Command {
	return &cobra.Command{
		Use:   "status",
		Short: "Show which queue you are connected to and what it is doing",
		Args:  cobra.NoArgs,
		RunE: func(cmd *cobra.Command, _ []string) error {
			c, err := client.Connect(cmd)
			if err != nil {
				return err
			}
			defer c.Close()

			typesRes, err := c.Data.ListQueueTypes(cmd.Context(), &queuev1.ListQueueTypesRequest{})
			if err != nil {
				return err
			}
			statsRes, err := c.Data.ListQueueStats(cmd.Context(), &queuev1.ListQueueStatsRequest{})
			if err != nil {
				return err
			}

			active := 0
			for _, queueType := range typesRes.GetQueueTypes() {
				if !queueType.HasActive() || queueType.GetActive() {
					active++
				}
			}
			var tickets, players, matches uint32
			for _, stats := range statsRes.GetStats() {
				tickets += stats.GetSearchingTickets()
				players += stats.GetSearchingPlayers()
				matches += stats.GetActiveMatches()
			}

			name := c.Context.Name
			if name == "" {
				name = output.Faint.Render("none, set by flags or environment")
			}
			output.Fields(cmd.OutOrStdout(), [][2]string{
				{"Context", name},
				{"Queue", c.Context.URL + "  " + output.Dot(output.Green, "connected")},
				{"Queue types", fmt.Sprintf("%d · %d active", len(typesRes.GetQueueTypes()), active)},
				{"Searching", fmt.Sprintf("%s in %s", output.Count(int(players), "player"), output.Count(int(tickets), "ticket"))},
				{"Matches", fmt.Sprintf("%d running", matches)},
			})
			output.Next(cmd.OutOrStdout(),
				output.Step{Command: "queue types list", Description: "See all queue types"},
				output.Step{Command: "queue tickets list", Description: "See who is searching"},
				output.Step{Command: "queue matches list", Description: "See the running matches"},
			)
			return nil
		},
	}
}
