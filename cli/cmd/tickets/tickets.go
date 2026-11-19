package tickets

import (
	"errors"
	"fmt"
	"maps"
	"slices"
	"strings"

	queuev1 "buf.build/gen/go/mythicisland/queue/protocolbuffers/go/mythicisland/queue/v1"
	"github.com/mythicisland/queue/cli/internal/client"
	"github.com/mythicisland/queue/cli/internal/output"
	"github.com/spf13/cobra"
)

func NewCommand() *cobra.Command {
	cmd := &cobra.Command{
		Use:     "tickets",
		Aliases: []string{"ticket"},
		Short:   "See and remove the tickets in the queue",
	}
	cmd.AddCommand(newListCommand(), newGetCommand(), newDeleteCommand())
	return cmd
}

type target struct {
	ticketID string
	playerID string
}

func targetOf(args []string, player string) (target, error) {
	switch {
	case len(args) == 1 && player != "":
		return target{}, errors.New("pass a ticket id or --player, not both")
	case len(args) == 1:
		return target{ticketID: args[0]}, nil
	case player != "":
		return target{playerID: player}, nil
	default:
		return target{}, errors.New("pass a ticket id or --player")
	}
}

func fetch(cmd *cobra.Command, c *client.Client, t target) (*queuev1.Ticket, error) {
	if t.playerID != "" {
		req := queuev1.GetTicketByPlayerRequest_builder{PlayerId: t.playerID}.Build()
		res, err := c.Data.GetTicketByPlayer(cmd.Context(), req)
		return res.GetTicket(), err
	}
	req := queuev1.GetTicketRequest_builder{TicketId: t.ticketID}.Build()
	res, err := c.Data.GetTicket(cmd.Context(), req)
	return res.GetTicket(), err
}

func properties(values map[string]string) string {
	if len(values) == 0 {
		return output.Faint.Render("none")
	}
	lines := make([]string, 0, len(values))
	for _, key := range slices.Sorted(maps.Keys(values)) {
		lines = append(lines, output.Faint.Render(key+"=")+values[key])
	}
	return strings.Join(lines, "\n")
}

func failures(values map[string]uint32) string {
	if len(values) == 0 {
		return output.Faint.Render("none")
	}
	lines := make([]string, 0, len(values))
	for _, key := range slices.Sorted(maps.Keys(values)) {
		lines = append(lines, fmt.Sprintf("%s %s", key, output.Faint.Render(fmt.Sprintf("%dx", values[key]))))
	}
	return strings.Join(lines, "\n")
}

func completeIDs(cmd *cobra.Command, args []string, _ string) ([]string, cobra.ShellCompDirective) {
	if len(args) > 0 {
		return nil, cobra.ShellCompDirectiveNoFileComp
	}

	c, err := client.Connect(cmd)
	if err != nil {
		return nil, cobra.ShellCompDirectiveError
	}
	defer c.Close()

	res, err := c.Data.ListTickets(cmd.Context(), &queuev1.ListTicketsRequest{})
	if err != nil {
		return nil, cobra.ShellCompDirectiveError
	}

	ids := make([]string, 0, len(res.GetTickets()))
	for _, ticket := range res.GetTickets() {
		ids = append(ids, ticket.GetId())
	}
	return ids, cobra.ShellCompDirectiveNoFileComp
}
