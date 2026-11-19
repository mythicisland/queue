package matches

import (
	"fmt"
	"strings"
	"time"

	queuev1 "buf.build/gen/go/mythicisland/queue/protocolbuffers/go/mythicisland/queue/v1"
	"github.com/mythicisland/queue/cli/internal/client"
	"github.com/mythicisland/queue/cli/internal/manifest"
	"github.com/mythicisland/queue/cli/internal/output"
	"github.com/spf13/cobra"
)

func newGetCommand() *cobra.Command {
	return &cobra.Command{
		Use:               "get <id>",
		Short:             "Show a match",
		Args:              cobra.ExactArgs(1),
		ValidArgsFunction: completeIDs,
		RunE: func(cmd *cobra.Command, args []string) error {
			format, err := output.Format(cmd, output.FormatTable)
			if err != nil {
				return err
			}

			c, err := client.Connect(cmd)
			if err != nil {
				return err
			}
			defer c.Close()

			res, err := c.Data.GetMatch(cmd.Context(), queuev1.GetMatchRequest_builder{MatchId: args[0]}.Build())
			if err != nil {
				return err
			}
			match := res.GetMatch()

			if format != output.FormatTable {
				data, err := manifest.Encode(match, format)
				if err != nil {
					return err
				}
				_, err = cmd.OutOrStdout().Write(data)
				return err
			}

			fields := [][2]string{
				{"ID", match.GetId()},
				{"Queue type", match.GetQueueType()},
				{"State", output.MatchState(match.GetState())},
				{"Age", output.Age(match.GetCreatedAt().AsTime())},
			}
			if name := server(match); name != "" {
				fields = append(fields, [2]string{"Server", name + "  " + output.Faint.Render(match.GetAssignment().GetServerId())})
			}
			if match.HasCountdownEndTime() {
				left := time.Until(match.GetCountdownEndTime().AsTime()).Round(time.Second)
				if left > 0 {
					fields = append(fields, [2]string{"Countdown", left.String() + " left"})
				}
			}
			for i, team := range match.GetTeams() {
				fields = append(fields, [2]string{fmt.Sprintf("Team %d", i+1), strings.Join(team.GetPlayerIds(), "\n")})
			}
			tickets := make([]string, 0, len(match.GetTickets()))
			for _, ticket := range match.GetTickets() {
				tickets = append(tickets, ticket.GetId()+"  "+output.Faint.Render(output.Count(len(ticket.GetPlayerIds()), "player")))
			}
			fields = append(fields, [2]string{"Tickets", strings.Join(tickets, "\n")})

			output.Fields(cmd.OutOrStdout(), fields)
			return nil
		},
	}
}
