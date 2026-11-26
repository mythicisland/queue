package tickets

import (
	"strings"

	"github.com/mythicisland/queue/cli/internal/client"
	"github.com/mythicisland/queue/cli/internal/output"
	"github.com/spf13/cobra"
)

func newGetCommand() *cobra.Command {
	var player string

	cmd := &cobra.Command{
		Use:   "get [id]",
		Short: "Show a ticket",
		Example: `  queue tickets get 3f1c2a9e-1d4b-4c55-9e0a-8f2d7c6b5a41
  queue tickets get --player 069a79f4-44e9-4726-a5be-fca90e38aaf5`,
		Args:              cobra.MaximumNArgs(1),
		ValidArgsFunction: completeIDs,
		RunE: func(cmd *cobra.Command, args []string) error {
			t, err := targetOf(args, player)
			if err != nil {
				return err
			}
			format, err := output.Format(cmd, output.FormatTable)
			if err != nil {
				return err
			}

			c, err := client.Connect(cmd)
			if err != nil {
				return err
			}
			defer c.Close()

			ticket, err := fetch(cmd, c, t)
			if err != nil {
				return err
			}

			if format != output.FormatTable {
				return output.Print(cmd.OutOrStdout(), ticket, format)
			}

			fields := [][2]string{
				{"ID", ticket.GetId()},
				{"State", output.TicketState(ticket.GetState())},
				{"Players", strings.Join(ticket.GetPlayerIds(), "\n")},
				{"Queue types", strings.Join(ticket.GetQueueTypes(), ", ")},
				{"Waiting", output.Age(ticket.GetCreatedAt().AsTime())},
			}
			if ticket.GetMatchId() != "" {
				fields = append(fields, [2]string{"Match", ticket.GetMatchId()})
			}
			if ticket.HasAssignment() {
				fields = append(fields, [2]string{"Server", ticket.GetAssignment().GetServerName()})
			}
			fields = append(fields,
				[2]string{"Properties", properties(ticket.GetProperties())},
				[2]string{"Failures", failures(ticket.GetFailures())},
			)

			w := cmd.OutOrStdout()
			output.Fields(w, fields)
			if ticket.GetMatchId() != "" {
				output.Next(w, output.Step{Command: "queue matches get " + ticket.GetMatchId(), Description: "Show the match of this ticket"})
			}
			return nil
		},
	}
	cmd.Flags().StringVar(&player, "player", "", "find the ticket of this player instead")
	return cmd
}
