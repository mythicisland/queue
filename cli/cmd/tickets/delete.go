package tickets

import (
	"errors"

	queuev1 "buf.build/gen/go/mythicisland/queue/protocolbuffers/go/mythicisland/queue/v1"
	"charm.land/huh/v2"
	"github.com/mythicisland/queue/cli/internal/client"
	"github.com/mythicisland/queue/cli/internal/output"
	"github.com/mythicisland/queue/cli/internal/prompt"
	"github.com/spf13/cobra"
)

func newDeleteCommand() *cobra.Command {
	var player string
	var yes bool

	cmd := &cobra.Command{
		Use:     "delete [id]",
		Aliases: []string{"rm"},
		Short:   "Take a ticket out of the queue",
		Long:    "Take a ticket out of the queue, as if its players left. A match the ticket is in handles it like players leaving.",
		Example: `  queue tickets delete 3f1c2a9e-1d4b-4c55-9e0a-8f2d7c6b5a41
  queue tickets delete --player 069a79f4-44e9-4726-a5be-fca90e38aaf5 -y`,
		Args:              cobra.MaximumNArgs(1),
		ValidArgsFunction: completeIDs,
		RunE: func(cmd *cobra.Command, args []string) error {
			t, err := targetOf(args, player)
			if err != nil {
				return err
			}

			if !yes {
				confirmed, err := prompt.Confirm("Delete this ticket?", "Delete")
				if errors.Is(err, huh.ErrUserAborted) || err == nil && !confirmed {
					return nil
				}
				if err != nil {
					return err
				}
			}

			c, err := client.Connect(cmd)
			if err != nil {
				return err
			}
			defer c.Close()

			req := queuev1.DeleteTicketRequest_builder{}
			if t.playerID != "" {
				req.PlayerId = &t.playerID
			} else {
				req.TicketId = &t.ticketID
			}
			if _, err := c.Tickets.DeleteTicket(cmd.Context(), req.Build()); err != nil {
				return err
			}

			if t.playerID != "" {
				output.Success(cmd.OutOrStdout(), "Deleted the ticket of %s", t.playerID)
			} else {
				output.Success(cmd.OutOrStdout(), "Deleted ticket %s", t.ticketID)
			}
			return nil
		},
	}
	cmd.Flags().StringVar(&player, "player", "", "delete the ticket of this player instead")
	cmd.Flags().BoolVarP(&yes, "yes", "y", false, "delete without asking")
	return cmd
}
