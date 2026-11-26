package tickets

import (
	"fmt"
	"slices"
	"strconv"
	"strings"

	queuev1 "buf.build/gen/go/mythicisland/queue/protocolbuffers/go/mythicisland/queue/v1"
	"github.com/mythicisland/queue/cli/internal/client"
	"github.com/mythicisland/queue/cli/internal/output"
	"github.com/spf13/cobra"
)

func newListCommand() *cobra.Command {
	var queueType string

	cmd := &cobra.Command{
		Use:     "list",
		Aliases: []string{"ls"},
		Short:   "List all tickets, oldest first",
		Example: `  queue tickets list
  queue tickets list --type bedwars-4x4`,
		Args: cobra.NoArgs,
		RunE: func(cmd *cobra.Command, _ []string) error {
			format, err := output.Format(cmd, output.FormatTable)
			if err != nil {
				return err
			}

			c, err := client.Connect(cmd)
			if err != nil {
				return err
			}
			defer c.Close()

			req := queuev1.ListTicketsRequest_builder{QueueType: queueType}.Build()
			res, err := c.Data.ListTickets(cmd.Context(), req)
			if err != nil {
				return err
			}
			tickets := res.GetTickets()
			slices.SortFunc(tickets, func(a, b *queuev1.Ticket) int {
				return a.GetCreatedAt().AsTime().Compare(b.GetCreatedAt().AsTime())
			})

			if format != output.FormatTable {
				return output.PrintList(cmd.OutOrStdout(), tickets, format)
			}

			w := cmd.OutOrStdout()
			if len(tickets) == 0 {
				output.Info(w, "No tickets right now")
				return nil
			}

			searching, players := 0, 0
			rows := make([][]string, 0, len(tickets))
			for _, ticket := range tickets {
				if ticket.GetState() == queuev1.TicketState_TICKET_STATE_SEARCHING {
					searching++
				}
				players += len(ticket.GetPlayerIds())
				rows = append(rows, []string{
					ticket.GetId(),
					output.TicketState(ticket.GetState()),
					strconv.Itoa(len(ticket.GetPlayerIds())),
					strings.Join(ticket.GetQueueTypes(), ", "),
					output.Age(ticket.GetCreatedAt().AsTime()),
				})
			}
			output.Table(w, []string{"ID", "STATE", "PLAYERS", "QUEUE TYPES", "WAITING"}, rows)
			output.Footer(w, output.Count(len(tickets), "ticket"), fmt.Sprintf("%d searching", searching), output.Count(players, "player"))
			output.Next(w, output.Step{Command: "queue tickets get <id>", Description: "Show players, properties and failures of a ticket"})
			return nil
		},
	}
	cmd.Flags().StringVar(&queueType, "type", "", "only tickets of this queue type")
	return cmd
}
